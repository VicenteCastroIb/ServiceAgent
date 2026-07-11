package com.tuapp.service;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.core.JsonValue;
import com.anthropic.models.messages.*;
import com.tuapp.model.Tenant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Genera la respuesta del agente de IA para un mensaje entrante, usando el
 * contexto propio del tenant (catálogo, precios, horarios, tono) y, según el
 * plan contratado, puede invocar las tools de function calling:
 * derivar_a_humano, agendar_cita, cancelar_reagendar_cita, generar_link_pago
 * (ver doc, secciones 4, 5.3 y 5.4).
 *
 * Semana 2: Claude Haiku 4.5 con prompt caching (el system prompt, que no
 * cambia entre mensajes de un mismo tenant, se marca con cache_control para
 * no re-cobrar esos tokens en cada mensaje - ver doc sección 6).
 * Semana 3: el contexto ya no está hardcodeado - se resuelve el Tenant real
 * vía TenantService según el número de WhatsApp del negocio ("To" del
 * webhook).
 * TODO: agregar las tools de agendamiento/pago según el plan contratado.
 */
@Slf4j
@Service
public class AiResponseService {

    private static final Model MODELO = Model.CLAUDE_HAIKU_4_5_20251001;
    private static final long MAX_TOKENS = 1024;
    private static final int MAX_ITERACIONES_TOOLS = 3;

    private final AnthropicClient client;
    private final HandoffService handoffService;
    private final TenantService tenantService;

    public AiResponseService(
            @Value("${anthropic.api-key}") String apiKey,
            HandoffService handoffService,
            TenantService tenantService) {
        this.client = AnthropicOkHttpClient.builder().apiKey(apiKey).build();
        this.handoffService = handoffService;
        this.tenantService = tenantService;
    }

    /**
     * Genera la respuesta para un mensaje entrante de un cliente.
     *
     * @param numeroNegocio número de WhatsApp del NEGOCIO (to), usado para
     *                      resolver qué tenant/contexto corresponde.
     * @param numeroCliente número de WhatsApp del cliente (from), usado como
     *                      identificador de conversación para derivar_a_humano.
     * @param mensaje       texto del mensaje entrante.
     * @return el texto a responder por WhatsApp.
     */
    public String generarRespuesta(String numeroNegocio, String numeroCliente, String mensaje) {
        try {
            Tenant tenant = tenantService.resolverPorNumeroWhatsapp(numeroNegocio);
            return generarRespuestaInterno(tenant, numeroCliente, mensaje);
        } catch (Exception e) {
            // Si falla resolver el tenant o el proveedor de IA (sin crédito, caído,
            // rate limit, etc.) no dejamos el webhook en 500: respondemos algo
            // razonable y derivamos a humano para que igual lo atiendan.
            log.error("Falló generarRespuesta para negocio={} cliente={}: {}",
                    numeroNegocio, numeroCliente, e.getMessage(), e);
            handoffService.derivarAHumano(numeroCliente, "error técnico del agente de IA: " + e.getMessage());
            return "Estamos con un problema técnico en este momento. En breve te contacta alguien del local para ayudarte.";
        }
    }

    private String generarRespuestaInterno(Tenant tenant, String numeroCliente, String mensaje) {
        Tool derivarAHumano = Tool.builder()
                .name("derivar_a_humano")
                .description("""
                        Deriva la conversación a un humano (el dueño del negocio) cuando: \
                        el cliente lo pide explícitamente, hay baja confianza en cómo responder, \
                        se detecta un reclamo o negociación de precio, o después de varios intentos \
                        fallidos de ayudar. Pausa las respuestas automáticas para este cliente.""")
                .inputSchema(Tool.InputSchema.builder()
                        .properties(JsonValue.from(Map.of(
                                "motivo", Map.of(
                                        "type", "string",
                                        "description", "Motivo breve y concreto de la derivación."
                                )
                        )))
                        .build())
                .build();

        MessageCreateParams.Builder paramsBuilder = MessageCreateParams.builder()
                .model(MODELO)
                .maxTokens(MAX_TOKENS)
                .systemOfTextBlockParams(List.of(
                        TextBlockParam.builder()
                                .text(construirSystemPrompt(tenant))
                                // El contexto del negocio no cambia entre mensajes: se cachea
                                // para no pagar esos tokens de nuevo en cada mensaje (doc sección 6).
                                .cacheControl(CacheControlEphemeral.builder().build())
                                .build()
                ))
                .addTool(derivarAHumano)
                .addUserMessage(mensaje);

        for (int intento = 0; intento < MAX_ITERACIONES_TOOLS; intento++) {
            Message respuesta = client.messages().create(paramsBuilder.build());

            List<ToolUseBlock> toolUses = respuesta.content().stream()
                    .flatMap(bloque -> bloque.toolUse().stream())
                    .toList();

            if (toolUses.isEmpty()) {
                return extraerTexto(respuesta);
            }

            for (ToolUseBlock toolUse : toolUses) {
                if ("derivar_a_humano".equals(toolUse.name())) {
                    String motivo = leerMotivo(toolUse);
                    handoffService.derivarAHumano(numeroCliente, motivo);
                }

                paramsBuilder
                        .addAssistantMessageOfBlockParams(List.of(ContentBlockParam.ofToolUse(
                                ToolUseBlockParam.builder()
                                        .id(toolUse.id())
                                        .name(toolUse.name())
                                        .input(toolUse._input())
                                        .build())))
                        .addUserMessageOfBlockParams(List.of(ContentBlockParam.ofToolResult(
                                ToolResultBlockParam.builder()
                                        .toolUseId(toolUse.id())
                                        .content("Derivación registrada. Avisale al cliente que en breve lo contacta alguien del local.")
                                        .build())));
            }
        }

        log.warn("Se alcanzó el máximo de iteraciones de tools sin respuesta final para {}", numeroCliente);
        return "Dame un momento, ya te va a responder alguien del local.";
    }

    private String construirSystemPrompt(Tenant tenant) {
        return """
                Sos el agente de atención por WhatsApp de %s. Respondé SIEMPRE en español \
                de Chile, de forma breve (whatsapp, no un correo), usando el tono indicado abajo.
                Respondé solo con información del contexto del negocio. Si no sabés algo, decilo \
                y ofrecé derivar a un humano en vez de inventar.
                Usá la herramienta derivar_a_humano cuando corresponda según sus instrucciones.

                Contexto del negocio:
                %s
                """.formatted(tenant.getBusinessName(), tenant.getBusinessContext());
    }

    private String extraerTexto(Message respuesta) {
        return respuesta.content().stream()
                .flatMap(bloque -> bloque.text().stream())
                .map(TextBlock::text)
                .reduce("", (a, b) -> a + b);
    }

    private String leerMotivo(ToolUseBlock toolUse) {
        DerivarAHumanoInput input = toolUse._input().convert(DerivarAHumanoInput.class);
        return (input != null && input.motivo != null) ? input.motivo : "no especificado";
    }

    /** Forma esperada del input de la tool derivar_a_humano, para deserializar con Jackson. */
    private static class DerivarAHumanoInput {
        public String motivo;
    }
}
