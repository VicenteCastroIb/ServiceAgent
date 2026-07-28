package com.tuapp.controller;

import com.tuapp.security.RateLimiter;
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
import java.time.Duration;

/**
 * Webhooks de Flow para la suscripción que los TENANTS nos pagan a NOSOTROS
 * (doc secciones 3 y 6) - cuenta Flow propia de la plataforma, ver
 * SubscriptionBillingService. Público a nivel de Spring Security
 * (/webhooks/** permitAll, ver SecurityConfig) igual que el resto de
 * webhooks de Flow/Twilio: nunca se confía en el contenido del request, solo
 * se usa el token para consultar el estado real contra Flow.
 *
 * Rate limiting GLOBAL (no por IP, a propósito): estos dos endpoints llaman
 * de vuelta a la API de Flow (getRegisterStatus/getStatus) ANTES de validar
 * nada localmente - a diferencia de FlowWebhookController.confirmacion, que
 * primero busca la orden en base y recién llama a Flow si el token existe.
 * Sin límite, cualquiera podría loopear estos endpoints con tokens
 * inventados y hacer que nosotros generemos tráfico ilimitado hacia Flow con
 * nuestras propias credenciales (costo/latencia de nuestro lado, y riesgo de
 * que Flow nos empiece a limitar el apiKey por volumen anómalo). Por IP
 * sería contraproducente acá: quien realmente llama a estos endpoints es la
 * infraestructura de Flow (servidor a servidor), no el navegador de cada
 * cliente final - todas las confirmaciones legítimas de TODOS los tenants
 * comparten el mismo origen, así que limitar por IP arriesgaría bloquear
 * tráfico real de Flow en vez de solo al abuso. Por eso la clave es fija
 * (una sola ventana compartida) con un techo generoso.
 */
@Slf4j
@RestController
@RequestMapping("/webhooks/flow-billing")
public class FlowBillingWebhookController {

    private static final int MAX_LLAMADAS_POR_MINUTO = 300;

    private final SubscriptionBillingService subscriptionBillingService;
    private final RateLimiter rateLimiter;
    private final String panelPublicUrl;

    public FlowBillingWebhookController(
            SubscriptionBillingService subscriptionBillingService,
            RateLimiter rateLimiter,
            @Value("${panel.public-url:}") String panelPublicUrl) {
        this.subscriptionBillingService = subscriptionBillingService;
        this.rateLimiter = rateLimiter;
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
        if (!rateLimiter.permitir("flow-billing:retorno", MAX_LLAMADAS_POR_MINUTO, Duration.ofMinutes(1))) {
            log.warn("Rate limit excedido en /webhooks/flow-billing/retorno (tenantId={})", tenantId);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }
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
        if (!rateLimiter.permitir("flow-billing:notificacion", MAX_LLAMADAS_POR_MINUTO, Duration.ofMinutes(1))) {
            log.warn("Rate limit excedido en /webhooks/flow-billing/notificacion");
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }
        subscriptionBillingService.procesarNotificacionPago(token);
        return ResponseEntity.ok().build();
    }
}
