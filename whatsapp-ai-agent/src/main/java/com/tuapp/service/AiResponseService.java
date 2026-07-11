package com.tuapp.service;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.core.JsonValue;
import com.anthropic.models.messages.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tuapp.model.Appointment;
import com.tuapp.model.AppointmentStatus;
import com.tuapp.model.Tenant;
import com.tuapp.model.TenantPlan;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
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
 * Semana 5: agendar_cita y cancelar_reagendar_cita, expuestas solo si el
 * tenant tiene plan PRO o CATALOGO (doc sección 5.4).
 * TODO: agregar la tool de pago (generar_link_pago) para el plan Catálogo.
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
    private final SchedulingService schedulingService;

    public AiResponseService(
            @Value("${anthropic.api-key}") String apiKey,
            HandoffService handoffService,
            TenantService tenantService,
            SchedulingService schedulingService) {
        this.client = AnthropicOkHttpClient.builder().apiKey(apiKey).build();
        this.handoffService = handoffService;
        this.tenantService = tenantService;
        this.schedulingService = schedulingService;
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
        Tenant tenant = null;
        try {
            tenant = tenantService.resolverPorNumeroWhatsapp(numeroNegocio);
            return generarRespuestaInterno(tenant, numeroCliente, mensaje);
        } catch (Exception e) {
            // Si falla resolver el tenant o el proveedor de IA (sin crédito, caído,
            // rate limit, etc.) no dejamos el webhook en 500: respondemos algo
            // razonable y derivamos a humano para que igual lo atiendan.
            log.error("Falló generarRespuesta para negocio={} cliente={}: {}",
                    numeroNegocio, numeroCliente, e.getMessage(), e);
            // tenant puede seguir siendo null acá si justo falló resolverlo -
            // el handoff queda sin tenant asociado, visible solo para el admin.
            Long tenantId = tenant != null ? tenant.getId() : null;
            handoffService.derivarAHumano(tenantId, numeroCliente, "error técnico del agente de IA: " + e.getMessage());
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

        boolean tieneAgendamiento = tenant.getPlan() != TenantPlan.BASICO;

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
                .addTool(derivarAHumano);

        if (tieneAgendamiento) {
            paramsBuilder.addTool(construirToolAgendarCita());
            paramsBuilder.addTool(construirToolCancelarReagendarCita());
        }
        paramsBuilder.addUserMessage(mensaje);

        for (int intento = 0; intento < MAX_ITERACIONES_TOOLS; intento++) {
            Message respuesta = client.messages().create(paramsBuilder.build());

            List<ToolUseBlock> toolUses = respuesta.content().stream()
                    .flatMap(bloque -> bloque.toolUse().stream())
                    .toList();

            if (toolUses.isEmpty()) {
                return extraerTexto(respuesta);
            }

            for (ToolUseBlock toolUse : toolUses) {
                String resultado = ejecutarTool(tenant, numeroCliente, toolUse);

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
                                        .content(resultado)
                                        .build())));
            }
        }

        log.warn("Se alcanzó el máximo de iteraciones de tools sin respuesta final para {}", numeroCliente);
        return "Dame un momento, ya te va a responder alguien del local.";
    }

    private String construirSystemPrompt(Tenant tenant) {
        StringBuilder prompt = new StringBuilder("""
                Sos el agente de atención por WhatsApp de %s. Respondé SIEMPRE en español \
                de Chile, de forma breve (whatsapp, no un correo), usando el tono indicado abajo.
                Respondé solo con información del contexto del negocio. Si no sabés algo, decilo \
                y ofrecé derivar a un humano en vez de inventar.
                Usá la herramienta derivar_a_humano cuando corresponda según sus instrucciones.
                """.formatted(tenant.getBusinessName()));

        if (tenant.getPlan() != TenantPlan.BASICO) {
            // La fecha de hoy le permite a Claude resolver fechas relativas
            // ("mañana", "el viernes") al llamar a agendar_cita.
            prompt.append("""

                    Este negocio tiene agendamiento de horas habilitado. Usá agendar_cita cuando \
                    el cliente quiera reservar una hora, y cancelar_reagendar_cita cuando quiera \
                    cancelar o cambiar una cita existente. Si el horario pedido no está disponible, \
                    el resultado de la tool te lo va a decir - proponele otro horario al cliente. \
                    Hoy es %s.
                    """.formatted(LocalDate.now()));
        }

        prompt.append("""

                Contexto del negocio:
                %s
                """.formatted(tenant.getBusinessContext()));

        return prompt.toString();
    }

    private Tool construirToolAgendarCita() {
        return Tool.builder()
                .name("agendar_cita")
                .description("""
                        Agenda una cita para el cliente en la fecha y hora indicadas, si el \
                        negocio tiene ese horario disponible.""")
                .inputSchema(Tool.InputSchema.builder()
                        .properties(JsonValue.from(Map.of(
                                "fecha", Map.of("type", "string", "description", "Fecha en formato YYYY-MM-DD"),
                                "hora", Map.of("type", "string", "description", "Hora en formato HH:mm (24 horas)"),
                                "servicio", Map.of("type", "string", "description", "Servicio o motivo de la cita")
                        )))
                        .build())
                .build();
    }

    private Tool construirToolCancelarReagendarCita() {
        return Tool.builder()
                .name("cancelar_reagendar_cita")
                .description("""
                        Cancela o reagenda una cita existente del cliente. Para reagendar, \
                        incluí nueva_fecha y nueva_hora; para solo cancelar, dejalos vacíos. \
                        Si no sabés el id de la cita no lo mandes - se usa automáticamente la \
                        próxima cita confirmada de ese cliente en este negocio.""")
                .inputSchema(Tool.InputSchema.builder()
                        .properties(JsonValue.from(Map.of(
                                "id", Map.of("type", "integer", "description", "Id de la cita, si se conoce (opcional)"),
                                "nueva_fecha", Map.of("type", "string", "description", "Nueva fecha YYYY-MM-DD (opcional, solo para reagendar)"),
                                "nueva_hora", Map.of("type", "string", "description", "Nueva hora HH:mm (opcional, solo para reagendar)")
                        )))
                        .build())
                .build();
    }

    private String ejecutarTool(Tenant tenant, String numeroCliente, ToolUseBlock toolUse) {
        return switch (toolUse.name()) {
            case "derivar_a_humano" -> {
                String motivo = leerMotivo(toolUse);
                handoffService.derivarAHumano(tenant.getId(), numeroCliente, motivo);
                yield "Derivación registrada. Avisale al cliente que en breve lo contacta alguien del local.";
            }
            case "agendar_cita" -> ejecutarAgendarCita(tenant, numeroCliente, toolUse);
            case "cancelar_reagendar_cita" -> ejecutarCancelarReagendarCita(tenant, numeroCliente, toolUse);
            default -> {
                log.warn("Tool desconocida invocada por el modelo: {}", toolUse.name());
                yield "Esa acción no está disponible.";
            }
        };
    }

    private String ejecutarAgendarCita(Tenant tenant, String numeroCliente, ToolUseBlock toolUse) {
        AgendarCitaInput input = toolUse._input().convert(AgendarCitaInput.class);
        try {
            LocalDate fecha = LocalDate.parse(input.fecha);
            LocalTime hora = LocalTime.parse(input.hora);
            Appointment cita = schedulingService.agendarCita(tenant, numeroCliente, fecha, hora, input.servicio);
            return "Cita agendada para el %s a las %s (id %d). Confirmale el horario al cliente."
                    .formatted(cita.getStartTime().toLocalDate(), cita.getStartTime().toLocalTime(), cita.getId());
        } catch (SchedulingException e) {
            return e.getMessage();
        } catch (DateTimeParseException | NullPointerException e) {
            return "No entendí bien la fecha/hora, pedile al cliente que la confirme en formato claro.";
        }
    }

    private String ejecutarCancelarReagendarCita(Tenant tenant, String numeroCliente, ToolUseBlock toolUse) {
        CancelarReagendarCitaInput input = toolUse._input().convert(CancelarReagendarCitaInput.class);
        try {
            LocalDate nuevaFecha = (input.nuevaFecha != null && !input.nuevaFecha.isBlank())
                    ? LocalDate.parse(input.nuevaFecha) : null;
            LocalTime nuevaHora = (input.nuevaHora != null && !input.nuevaHora.isBlank())
                    ? LocalTime.parse(input.nuevaHora) : null;
            Appointment cita = schedulingService.cancelarOReagendarCita(
                    tenant, numeroCliente, input.id, nuevaFecha, nuevaHora);
            return cita.getStatus() == AppointmentStatus.CANCELADA
                    ? "Cita cancelada."
                    : "Cita reagendada para el %s a las %s."
                            .formatted(cita.getStartTime().toLocalDate(), cita.getStartTime().toLocalTime());
        } catch (SchedulingException e) {
            return e.getMessage();
        } catch (DateTimeParseException e) {
            return "No entendí bien la nueva fecha/hora, pedile al cliente que la confirme en formato claro.";
        }
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

    /** Forma esperada del input de la tool agendar_cita, para deserializar con Jackson. */
    private static class AgendarCitaInput {
        public String fecha;
        public String hora;
        public String servicio;
    }

    /** Forma esperada del input de la tool cancelar_reagendar_cita, para deserializar con Jackson. */
    private static class CancelarReagendarCitaInput {
        public Long id;
        @JsonProperty("nueva_fecha")
        public String nuevaFecha;
        @JsonProperty("nueva_hora")
        public String nuevaHora;
    }
}
