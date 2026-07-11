package com.tuapp.service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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
 * libre - Meta los rechaza si no. El sandbox de Twilio permite texto libre,
 * así que por ahora se usa texto plano.
 * TODO: migrar a Content API con contentSid cuando haya WABA verificado y
 * plantilla de recordatorio aprobada.
 * TODO Fase 2 (mes 3+): migración a Meta directo sin Twilio (doc sección 5.5).
 * TODO: Instagram (Graph API directa, no pasa por Twilio).
 */
@Slf4j
@Service
public class MessagingService {

    private final String numeroRemitente;

    public MessagingService(
            @Value("${twilio.account-sid}") String accountSid,
            @Value("${twilio.auth-token}") String authToken,
            @Value("${twilio.whatsapp-number}") String numeroRemitente) {
        Twilio.init(accountSid, authToken);
        this.numeroRemitente = numeroRemitente.startsWith("whatsapp:")
                ? numeroRemitente
                : "whatsapp:" + numeroRemitente;
    }

    /**
     * Manda un mensaje saliente por WhatsApp, iniciado por el negocio (no en
     * respuesta a un mensaje del cliente). Usado por el job de recordatorios.
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
}
