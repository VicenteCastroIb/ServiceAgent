package com.tuapp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Envía mensajes salientes por WhatsApp (Twilio, fase 1) e Instagram (Graph
 * API directa de Meta, doc sección 5.1).
 *
 * Se usa para los mensajes proactivos iniciados por el negocio (recordatorios
 * de cita, plan Pro, doc sección 6) - a diferencia del webhook reactivo
 * (WebhookController), que responde con TwiML dentro del mismo request/
 * response HTTP de Twilio, esto llama a la REST API de Twilio para mandar un
 * mensaje nuevo en cualquier momento.
 *
 * Importante (doc sección 6 y 11): en producción, con un WABA verificado,
 * los mensajes proactivos fuera de la ventana de 24h deben mandarse como
 * plantilla "utility" pre-aprobada (Twilio Content API, contentSid), no texto
 * libre - Meta los rechaza si no. El sandbox de Twilio permite texto libre.
 * ReminderJob decide cuál de los dos métodos de acá abajo usar según si hay
 * una plantilla aprobada configurada (reminders.content-sid) - ver
 * TwilioTemplateService para cómo se crea y aprueba esa plantilla.
 * TODO Fase 2 (mes 3+): migración a Meta directo sin Twilio (doc sección 5.5).
 */
@Slf4j
@Service
public class MessagingService {

    private final String numeroRemitente;
    private final ObjectMapper objectMapper;

    public MessagingService(
            @Value("${twilio.account-sid}") String accountSid,
            @Value("${twilio.auth-token}") String authToken,
            @Value("${twilio.whatsapp-number}") String numeroRemitente,
            ObjectMapper objectMapper) {
        Twilio.init(accountSid, authToken);
        this.numeroRemitente = numeroRemitente.startsWith("whatsapp:")
                ? numeroRemitente
                : "whatsapp:" + numeroRemitente;
        this.objectMapper = objectMapper;
    }

    /**
     * Manda un mensaje saliente por WhatsApp con texto libre, iniciado por el
     * negocio (no en respuesta a un mensaje del cliente). Solo válido dentro
     * de la ventana de 24hs o en el sandbox de Twilio - en producción con WABA
     * verificado, Meta lo rechaza fuera de esa ventana (usar
     * {@link #enviarWhatsAppConPlantilla} en ese caso).
     *
     * @param numeroDestino número del cliente, formato "whatsapp:+56...".
     */
    public void enviarWhatsApp(String numeroDestino, String texto) {
        Message mensaje = Message.creator(
                        new PhoneNumber(numeroDestino),
                        new PhoneNumber(numeroRemitente),
                        texto)
                .create();
        log.info("Mensaje saliente enviado a {} (sid={})", numeroDestino, mensaje.getSid());
    }

    /**
     * Manda un mensaje saliente por WhatsApp usando una plantilla "utility"
     * ya aprobada por Meta (Twilio Content API) - la única forma válida de
     * iniciar una conversación fuera de la ventana de 24hs en producción con
     * un WABA verificado (doc sección 6). Ver TwilioTemplateService para
     * crear/aprobar la plantilla y ReminderJob para cómo arma las variables.
     *
     * @param numeroDestino     número del cliente, formato "whatsapp:+56...".
     * @param contentSid        sid de la plantilla ya aprobada (empieza con "HX").
     * @param variables         valores para los placeholders {{1}}, {{2}}, ...
     *                          de la plantilla, con la clave siendo el número
     *                          como string ("1", "2", ...).
     */
    public void enviarWhatsAppConPlantilla(String numeroDestino, String contentSid, Map<String, String> variables) {
        try {
            String contentVariables = objectMapper.writeValueAsString(variables);
            Message mensaje = Message.creator(
                            new PhoneNumber(numeroDestino),
                            new PhoneNumber(numeroRemitente),
                            "")
                    .setContentSid(contentSid)
                    .setContentVariables(contentVariables)
                    .create();
            log.info("Mensaje de plantilla enviado a {} (sid={}, contentSid={})",
                    numeroDestino, mensaje.getSid(), contentSid);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo enviar el mensaje de plantilla de WhatsApp", e);
        }
    }
}
