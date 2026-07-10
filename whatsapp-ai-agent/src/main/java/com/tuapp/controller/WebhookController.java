package com.tuapp.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Recibe los webhooks entrantes de WhatsApp (Twilio, fase 1) e Instagram
 * (Graph API de Meta) y dispara el flujo de un mensaje descrito en el doc,
 * sección 5.4: AiResponseService genera la respuesta y, según el plan del
 * tenant, puede invocar las tools de agendamiento o pago.
 *
 * TODO Semana 1: endpoint de webhook de Twilio (sandbox).
 */
@RestController
@RequestMapping("/webhooks")
public class WebhookController {
}
