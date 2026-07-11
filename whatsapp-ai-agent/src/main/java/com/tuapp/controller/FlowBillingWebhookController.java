package com.tuapp.controller;

import com.tuapp.service.SubscriptionBillingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Webhooks de Flow para la suscripción que los TENANTS nos pagan a NOSOTROS
 * (doc secciones 3 y 6) - cuenta Flow propia de la plataforma, ver
 * SubscriptionBillingService. Público a nivel de Spring Security
 * (/webhooks/** permitAll, ver SecurityConfig) igual que el resto de
 * webhooks de Flow/Twilio: nunca se confía en el contenido del request, solo
 * se usa el token para consultar el estado real contra Flow.
 */
@Slf4j
@RestController
@RequestMapping("/webhooks/flow-billing")
public class FlowBillingWebhookController {

    private final SubscriptionBillingService subscriptionBillingService;

    public FlowBillingWebhookController(SubscriptionBillingService subscriptionBillingService) {
        this.subscriptionBillingService = subscriptionBillingService;
    }

    /** url_return de /customer/register: Flow llama acá cuando el dueño terminó de registrar su tarjeta. */
    @PostMapping("/retorno/{tenantId}")
    public ResponseEntity<String> retornoRegistroTarjeta(@PathVariable Long tenantId, @RequestParam String token) {
        subscriptionBillingService.procesarRetornoTarjeta(tenantId, token);
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body("<p>Listo, tu tarjeta quedó registrada. Ya podés cerrar esta ventana.</p>");
    }

    /** urlCallback del plan: Flow llama acá cada vez que cobra un período de una suscripción. */
    @PostMapping("/notificacion")
    public ResponseEntity<Void> notificacionCobro(@RequestParam String token) {
        subscriptionBillingService.procesarNotificacionPago(token);
        return ResponseEntity.ok().build();
    }
}
