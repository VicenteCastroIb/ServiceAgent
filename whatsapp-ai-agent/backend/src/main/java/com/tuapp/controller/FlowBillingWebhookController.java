package com.tuapp.controller;

import com.tuapp.service.SubscriptionBillingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

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
    private final String panelPublicUrl;

    public FlowBillingWebhookController(
            SubscriptionBillingService subscriptionBillingService,
            @Value("${panel.public-url:}") String panelPublicUrl) {
        this.subscriptionBillingService = subscriptionBillingService;
        this.panelPublicUrl = panelPublicUrl;
    }

    /**
     * url_return de /customer/register: es el navegador del dueño el que
     * llega acá (Flow lo redirige después de que termina de cargar la
     * tarjeta, doc sección 12) - no un webhook servidor-a-servidor como
     * /notificacion. Por eso, a diferencia de ese endpoint, esto SÍ le
     * importa a un humano mirando la pantalla: si panel.public-url está
     * configurada, lo mandamos derecho al login del panel (con la
     * suscripción ya ACTIVA en ese momento, ver procesarRetornoTarjeta) en
     * vez de dejarlo viendo una respuesta cruda de la API.
     */
    @PostMapping("/retorno/{tenantId}")
    public ResponseEntity<?> retornoRegistroTarjeta(@PathVariable Long tenantId, @RequestParam String token) {
        subscriptionBillingService.procesarRetornoTarjeta(tenantId, token);

        if (panelPublicUrl != null && !panelPublicUrl.isBlank()) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(panelPublicUrl.replaceAll("/+$", "") + "/login?pago=confirmado"))
                    .build();
        }

        // Fallback mientras no esté cargada PANEL_PUBLIC_URL (ej. en local
        // sin configurar) - mejor esto que un redirect a ningún lado.
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body("<p>Listo, tu tarjeta quedó registrada. Ya podés iniciar sesión en el panel con el usuario y clave que creaste.</p>");
    }

    /** urlCallback del plan: Flow llama acá cada vez que cobra un período de una suscripción. */
    @PostMapping("/notificacion")
    public ResponseEntity<Void> notificacionCobro(@RequestParam String token) {
        subscriptionBillingService.procesarNotificacionPago(token);
        return ResponseEntity.ok().build();
    }
}
