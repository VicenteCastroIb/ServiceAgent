package com.tuapp.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuapp.model.Tenant;
import com.tuapp.service.AiResponseService;
import com.tuapp.service.HandoffService;
import com.tuapp.service.InstagramMessagingService;
import com.tuapp.service.TenantService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Recibe los webhooks de Instagram (Graph API directa de Meta, doc secciones
 * 3 y 5.1 - a diferencia de WhatsApp, que va vía Twilio) y dispara el mismo
 * flujo de un mensaje que WebhookController (AiResponseService genera la
 * respuesta, según el plan puede invocar tools), pero adaptado a cómo
 * funciona la Messenger Platform de Meta:
 *
 * - GET: verificación única del webhook al configurarlo en el Meta App
 *   Dashboard (hub.mode/hub.verify_token/hub.challenge).
 * - POST: mensajes entrantes reales. A diferencia de Twilio (que responde
 *   sincrónico con TwiML en el mismo POST), acá hay que reconocer el webhook
 *   con 200 y mandar la respuesta aparte, por la Send API
 *   (InstagramMessagingService) - lo hacemos de forma sincrónica dentro del
 *   mismo request por simplicidad (v1), antes de devolver el 200.
 * - Autenticación: no hay JWT ni validación de firma HMAC estilo Twilio -
 *   Meta firma el body con X-Hub-Signature-256 (HMAC-SHA256 con el secreto
 *   de la Meta App, compartido por todos los tenants - no es por tenant como
 *   WooCommerce/Flow, porque es una única Meta App la que reenvía los
 *   webhooks de todas las cuentas de Instagram conectadas).
 *
 * Importante (doc sección 6): Instagram no tiene un equivalente a las
 * plantillas "utility" de WhatsApp - solo se puede responder dentro de la
 * ventana de 24hs desde el último mensaje del cliente. Por eso los
 * recordatorios proactivos (ReminderJob) siguen siendo solo WhatsApp;
 * Instagram queda 100% reactivo en todos los planes.
 */
@Slf4j
@RestController
@RequestMapping("/webhooks/instagram")
public class InstagramWebhookController {

    private final String verifyToken;
    private final String appSecret;
    private final TenantService tenantService;
    private final HandoffService handoffService;
    private final AiResponseService aiResponseService;
    private final InstagramMessagingService instagramMessagingService;
    private final ObjectMapper objectMapper;

    public InstagramWebhookController(
            @Value("${meta.webhook-verify-token:}") String verifyToken,
            @Value("${meta.app-secret:}") String appSecret,
            TenantService tenantService,
            HandoffService handoffService,
            AiResponseService aiResponseService,
            InstagramMessagingService instagramMessagingService,
            ObjectMapper objectMapper) {
        this.verifyToken = verifyToken;
        this.appSecret = appSecret;
        this.tenantService = tenantService;
        this.handoffService = handoffService;
        this.aiResponseService = aiResponseService;
        this.instagramMessagingService = instagramMessagingService;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    public ResponseEntity<String> verificar(
            @RequestParam(name = "hub.mode", required = false) String modo,
            @RequestParam(name = "hub.verify_token", required = false) String tokenRecibido,
            @RequestParam(name = "hub.challenge", required = false) String challenge) {
        if (verifyToken.isBlank()) {
            log.warn("Verificación de webhook de Instagram recibida pero meta.webhook-verify-token no está configurado");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        if (!"subscribe".equals(modo) || !verifyToken.equals(tokenRecibido) || challenge == null) {
            log.warn("Verificación de webhook de Instagram rechazada (modo={})", modo);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(challenge);
    }

    @PostMapping
    public ResponseEntity<String> recibirMensaje(
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String firma,
            @RequestBody String rawBody) {

        if (appSecret.isBlank()) {
            log.warn("Webhook de Instagram recibido pero meta.app-secret no está configurado");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        if (!firmaValida(firma, rawBody)) {
            log.warn("Firma de Instagram inválida o ausente");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            JsonNode payload = objectMapper.readTree(rawBody);
            if (!"instagram".equals(payload.path("object").asText())) {
                return ResponseEntity.ok("EVENT_RECEIVED");
            }
            for (JsonNode entry : payload.path("entry")) {
                String instagramAccountId = entry.path("id").asText(null);
                for (JsonNode evento : entry.path("messaging")) {
                    procesarEvento(instagramAccountId, evento);
                }
            }
        } catch (Exception e) {
            // Meta espera un 200 rápido (igual que Flow con su confirmación de
            // pago): cualquier error se loguea acá adentro, nunca se propaga
            // como error HTTP - evita que Meta reintente el webhook completo
            // y termine mandando respuestas duplicadas al cliente.
            log.error("Error procesando webhook de Instagram", e);
        }
        return ResponseEntity.ok("EVENT_RECEIVED");
    }

    private void procesarEvento(String instagramAccountId, JsonNode evento) {
        // Se ignoran los eco de nuestros propios mensajes salientes y los que
        // no traen texto (adjuntos, reacciones, etc. - fuera de alcance v1).
        if (evento.path("message").path("is_echo").asBoolean(false)) {
            return;
        }
        String igsid = evento.path("sender").path("id").asText(null);
        String texto = evento.path("message").path("text").asText(null);
        if (instagramAccountId == null || igsid == null || texto == null || texto.isBlank()) {
            return;
        }

        // Prefijo "instagram:" para que el identificador de conversación no
        // pueda colisionar nunca con un numeroCliente de WhatsApp (formato
        // "whatsapp:+56...") en HandoffService/SchedulingService/PaymentService,
        // que son channel-agnostic y usan este string como clave/identidad.
        String idCliente = "instagram:" + igsid;
        try {
            Tenant tenant = tenantService.resolverPorInstagramAccountId(instagramAccountId);

            if (handoffService.estaPausada(idCliente)) {
                log.info("Conversación de Instagram con {} está pausada (handoff activo) - no se responde automático", idCliente);
                return;
            }

            String respuesta = aiResponseService.generarRespuestaParaTenant(tenant, idCliente, texto);
            instagramMessagingService.enviarMensaje(tenant, igsid, respuesta);
        } catch (Exception e) {
            log.error("Error respondiendo mensaje de Instagram de {} (cuenta {})", igsid, instagramAccountId, e);
        }
    }

    private boolean firmaValida(String firmaHeader, String rawBody) {
        if (firmaHeader == null || !firmaHeader.startsWith("sha256=")) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(appSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(rawBody.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            String esperada = "sha256=" + hex;
            return MessageDigest.isEqual(
                    esperada.getBytes(StandardCharsets.UTF_8), firmaHeader.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("Error calculando firma de Instagram", e);
            return false;
        }
    }
}
