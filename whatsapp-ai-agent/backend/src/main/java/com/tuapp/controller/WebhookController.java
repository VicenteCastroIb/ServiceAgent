package com.tuapp.controller;

import com.tuapp.model.ChannelType;
import com.tuapp.model.MessageDirection;
import com.tuapp.model.MessageSender;
import com.tuapp.model.Tenant;
import com.tuapp.service.AiResponseService;
import com.tuapp.service.ConversationService;
import com.tuapp.service.HandoffService;
import com.tuapp.service.TenantService;
import com.twilio.security.RequestValidator;
import com.twilio.twiml.MessagingResponse;
import com.twilio.twiml.messaging.Message;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Recibe los webhooks entrantes de WhatsApp (Twilio, fase 1) e Instagram
 * (Graph API de Meta) y dispara el flujo de un mensaje descrito en el doc,
 * sección 5.4: AiResponseService genera la respuesta y, según el plan del
 * tenant, puede invocar las tools de agendamiento o pago.
 *
 * Semana 1: endpoint de webhook de Twilio (sandbox), con validación de firma
 * para rechazar requests que no vengan realmente de Twilio.
 * Semana 2: delega en AiResponseService. Si la conversación está pausada por
 * un handoff a humano (HandoffService), el bot no responde nada automático -
 * el dueño la retoma manualmente desde el panel (doc, sección 4).
 * Semana 3: se extrae también el "To" (número del negocio) para que
 * AiResponseService resuelva el tenant real correspondiente.
 */
@Slf4j
@RestController
@RequestMapping("/webhooks")
public class WebhookController {

    private final RequestValidator requestValidator;
    private final AiResponseService aiResponseService;
    private final HandoffService handoffService;
    private final TenantService tenantService;
    private final ConversationService conversationService;

    public WebhookController(
            @Value("${twilio.auth-token}") String authToken,
            AiResponseService aiResponseService,
            HandoffService handoffService,
            TenantService tenantService,
            ConversationService conversationService) {
        this.requestValidator = new RequestValidator(authToken);
        this.aiResponseService = aiResponseService;
        this.handoffService = handoffService;
        this.tenantService = tenantService;
        this.conversationService = conversationService;
    }

    @PostMapping(value = "/whatsapp", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> receiveWhatsAppMessage(
            HttpServletRequest request,
            @RequestHeader(value = "X-Twilio-Signature", required = false) String signature,
            @RequestParam MultiValueMap<String, String> allParams) {

        // Validación de firma: sin esto, cualquiera que descubra la URL de ngrok
        // podría mandar POSTs falsos haciéndose pasar por Twilio.
        String url = request.getRequestURL().toString();
        Map<String, String> params = new HashMap<>();
        allParams.forEach((key, values) -> params.put(key, values.isEmpty() ? "" : values.get(0)));

        if (signature == null || !requestValidator.validate(url, params, signature)) {
            log.warn("Firma de Twilio inválida o ausente en request a {}", url);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        String from = params.getOrDefault("From", "desconocido");
        String to = params.getOrDefault("To", "desconocido");
        String incomingBody = params.getOrDefault("Body", "");
        log.info("Mensaje de WhatsApp recibido de {} para {}: {}", from, to, incomingBody);

        // Resuelto en paralelo a AiResponseService (que hace lo mismo
        // internamente) solo para poder persistir el historial - ver
        // Javadoc de la clase. Nunca debe alterar el flujo de respuesta al
        // cliente, por eso queda envuelto en su propio try/catch silencioso.
        Tenant tenant = resolverTenantSilencioso(to);
        registrarMensajeSilencioso(tenant, from, MessageDirection.IN, MessageSender.CLIENTE, incomingBody);

        if (handoffService.estaPausada(from)) {
            // Ya se derivó a un humano: el bot no contesta más en esta conversación,
            // la sigue el dueño desde el panel (doc, sección 4).
            log.info("Conversación con {} está pausada (handoff activo) - no se responde automático", from);
            return ResponseEntity.ok(new MessagingResponse.Builder().build().toXml());
        }

        String replyText = aiResponseService.generarRespuesta(to, from, incomingBody);
        registrarMensajeSilencioso(tenant, from, MessageDirection.OUT, MessageSender.BOT, replyText);

        Message message = new Message.Builder(replyText).build();
        MessagingResponse twiml = new MessagingResponse.Builder()
                .message(message)
                .build();

        return ResponseEntity.ok(twiml.toXml());
    }

    private Tenant resolverTenantSilencioso(String numeroNegocio) {
        try {
            return tenantService.resolverPorNumeroWhatsapp(numeroNegocio);
        } catch (Exception e) {
            // No se loguea como error: AiResponseService.generarRespuesta ya
            // loguea y maneja este mismo caso (número sin tenant asociado) -
            // acá solo significa que este mensaje puntual no queda en el
            // historial del panel.
            return null;
        }
    }

    private void registrarMensajeSilencioso(
            Tenant tenant, String clientContact, MessageDirection direction, MessageSender sender, String content) {
        if (tenant == null) {
            return;
        }
        try {
            conversationService.registrarMensaje(tenant, ChannelType.WHATSAPP, clientContact, direction, sender, content);
        } catch (Exception e) {
            log.warn("No se pudo persistir el mensaje ({}) de {} para tenant {}: {}",
                    direction, clientContact, tenant.getId(), e.getMessage());
        }
    }
}
