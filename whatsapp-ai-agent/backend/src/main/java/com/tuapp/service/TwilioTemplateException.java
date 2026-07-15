package com.tuapp.service;

/**
 * Error creando o consultando una plantilla de WhatsApp (Twilio Content API)
 * con un mensaje pensado para mostrarle directo al admin en el panel - ver
 * TwilioTemplateService.
 */
public class TwilioTemplateException extends RuntimeException {
    public TwilioTemplateException(String mensaje) {
        super(mensaje);
    }
}
