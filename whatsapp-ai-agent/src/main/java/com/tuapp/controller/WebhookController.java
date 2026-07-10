package com.tuapp.controller;

import com.tuapp.service.AiResponseService;
import com.tuapp.service.HandoffService;
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
 */
@Slf4j
@RestController
@RequestMapping("/webhooks")
public class WebhookController {

    private final RequestValidator requestValidator;
    private final AiResponseService aiResponseService;
    private final HandoffService handoffService;

    public WebhookController(
            @Value("${twilio.auth-token}") String authToken,
            AiResponseService aiResponseService,
            HandoffService handoffService) {
        this.requestValidator = new RequestValidator(authToken);
        this.aiResponseService = aiResponseService;
        this.handoffService = handoffService;
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
        String incomingBody = params.getOrDefault("Body", "");
        log.info("Mensaje de WhatsApp recibido de {}: {}", from, incomingBody);

        if (handoffService.estaPausada(from)) {
            // Ya se derivó a un humano: el bot no contesta más en esta conversación,
            // la sigue el dueño desde el panel (doc, sección 4).
            log.info("Conversación con {} está pausada (handoff activo) - no se responde automático", from);
            return ResponseEntity.ok(new MessagingResponse.Builder().build().toXml());
        }

        String replyText = aiResponseService.generarRespuesta(from, incomingBody);
        Message message = new Message.Builder(replyText).build();
        MessagingResponse twiml = new MessagingResponse.Builder()
                .message(message)
                .build();

        return ResponseEntity.ok(twiml.toXml());
    }
}
