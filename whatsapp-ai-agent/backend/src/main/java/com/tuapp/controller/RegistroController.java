package com.tuapp.controller;

import com.tuapp.model.Tenant;
import com.tuapp.model.TenantPlan;
import com.tuapp.security.RateLimiter;
import com.tuapp.service.BillingException;
import com.tuapp.service.SubscriptionBillingService;
import com.tuapp.service.TenantService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

/**
 * Alta SELF-SERVICE de un negocio nuevo (doc sección 12: "el flujo de
 * auto-registro es el principal e ideal" - la venta puerta a puerta es un
 * canal de captación, pero el alta real pasa por acá).
 * <p>
 * Único endpoint público de escritura de la API (ver SecurityConfig: sin JWT,
 * permitAll). Por eso tiene dos resguardos que el resto de /admin/** no
 * necesita:
 * 1) Rate limiting por IP (RateLimiter) - sin esto, cualquiera podría hacer
 *    loop contra este endpoint y generar clientes/cargos de prueba en la
 *    cuenta Flow real de la plataforma.
 * 2) El plan CATALOGO queda explícitamente afuera - es a medida (cotización,
 *    doc sección 3), no tiene cobro recurrente automático en Flow
 *    (ver SubscriptionBillingService.planIdParaPlan).
 * <p>
 * Nota de alcance: esto NO incluye verificación de email ni captcha todavía -
 * el rate limit por IP es la única defensa contra abuso en esta primera
 * versión. Si en producción se ve spam de registros, el siguiente paso es
 * sumar verificación de email antes de tocar Flow.
 */
@Slf4j
@RestController
@RequestMapping("/public/registro")
public class RegistroController {

    private static final int MAX_INTENTOS_POR_HORA = 5;

    private final TenantService tenantService;
    private final SubscriptionBillingService subscriptionBillingService;
    private final RateLimiter rateLimiter;

    public RegistroController(
            TenantService tenantService,
            SubscriptionBillingService subscriptionBillingService,
            RateLimiter rateLimiter) {
        this.tenantService = tenantService;
        this.subscriptionBillingService = subscriptionBillingService;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping
    public ResponseEntity<?> registrar(@Valid @RequestBody RegistroRequest request, HttpServletRequest httpRequest) {
        if (request.plan() == TenantPlan.CATALOGO) {
            return ResponseEntity.badRequest().body(new ErrorResponse(
                    "El plan Catálogo es a medida - escribinos directamente para cotizarlo."));
        }

        String ip = httpRequest.getRemoteAddr();
        if (!rateLimiter.permitir("registro:" + ip, MAX_INTENTOS_POR_HORA, Duration.ofHours(1))) {
            log.warn("Rate limit excedido en /public/registro para ip={}", ip);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(new ErrorResponse("Demasiados intentos. Probá de nuevo más tarde."));
        }

        Tenant tenant;
        try {
            tenant = tenantService.registrarSelfService(
                    request.businessName(), request.ownerEmail(),
                    request.panelUsername(), request.panelPassword(), request.plan());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(e.getMessage()));
        }

        try {
            String urlRegistroTarjeta = subscriptionBillingService.iniciarSuscripcion(tenant, request.ownerEmail());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new RegistroResponse(tenant.getId(), urlRegistroTarjeta, null));
        } catch (BillingException e) {
            // El tenant ya quedó creado (con su login del panel funcionando),
            // pero no se pudo generar el link de pago automático - por
            // ejemplo si todavía no está cargada la cuenta Flow propia de la
            // plataforma (flow.billing.*, ver doc sección 10: se activa recién
            // antes del 3er cliente). No se revierte el alta: el admin puede
            // completar el cobro manualmente (registrarPagoManual) y avisarle
            // al dueño.
            log.warn("Tenant {} creado pero no se pudo iniciar la suscripción en Flow: {}", tenant.getId(), e.getMessage());
            return ResponseEntity.status(HttpStatus.CREATED).body(new RegistroResponse(
                    tenant.getId(), null,
                    "Tu negocio quedó registrado. No pudimos generar el link de pago automático en este momento - te vamos a contactar para completarlo."));
        }
    }

    public record RegistroRequest(
            @NotBlank @Size(max = 150) String businessName,
            @NotBlank @Email @Size(max = 150) String ownerEmail,
            @NotBlank @Size(min = 4, max = 50) @Pattern(
                    regexp = "^[a-zA-Z0-9._-]+$",
                    message = "Solo letras, números, puntos, guiones y guion bajo") String panelUsername,
            @NotBlank @Size(min = 8, max = 100) String panelPassword,
            @NotNull TenantPlan plan) {
    }

    public record RegistroResponse(Long tenantId, String urlRegistroTarjeta, String mensaje) {
    }

    public record ErrorResponse(String mensaje) {
    }
}
