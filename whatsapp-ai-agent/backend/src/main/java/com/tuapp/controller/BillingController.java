package com.tuapp.controller;

import com.tuapp.model.BillingStatus;
import com.tuapp.model.Tenant;
import com.tuapp.model.TenantSubscription;
import com.tuapp.security.PanelAuth;
import com.tuapp.service.BillingException;
import com.tuapp.service.SubscriptionBillingService;
import com.tuapp.service.TenantService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * API de administración de la suscripción que cada TENANT nos paga a
 * NOSOTROS por usar la plataforma (doc secciones 3 y 6) - no confundir con
 * CatalogController/PaymentController, que son del plan Catálogo del tenant
 * (su propia tienda cobrándole a SUS clientes). Ver SubscriptionBillingService
 * para la distinción completa de cuentas Flow.
 *
 * /admin/billing/planes es admin-only (setup único de la plataforma).
 * El resto sigue la autorización por tenant habitual: admin o el dueño de
 * ese negocio (por si necesita re-registrar su tarjeta, por ejemplo si
 * venció).
 */
@RestController
@RequestMapping("/admin")
public class BillingController {

    private final TenantService tenantService;
    private final SubscriptionBillingService subscriptionBillingService;

    public BillingController(TenantService tenantService, SubscriptionBillingService subscriptionBillingService) {
        this.tenantService = tenantService;
        this.subscriptionBillingService = subscriptionBillingService;
    }

    /** Setup único: crea los planes Básico/Pro en la cuenta Flow propia de la plataforma. */
    @PostMapping("/billing/planes")
    public ResponseEntity<Void> crearPlanes() {
        if (!PanelAuth.esAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        try {
            subscriptionBillingService.crearPlanesSiNoExisten();
            return ResponseEntity.ok().build();
        } catch (BillingException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/tenants/{tenantId}/billing/iniciar")
    public ResponseEntity<?> iniciarSuscripcion(
            @PathVariable Long tenantId, @Valid @RequestBody IniciarSuscripcionRequest request) {
        if (!PanelAuth.puedeAcceder(tenantId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        Tenant tenant = tenantService.buscarPorId(tenantId).orElse(null);
        if (tenant == null) {
            return ResponseEntity.notFound().build();
        }
        try {
            String urlRegistroTarjeta = subscriptionBillingService.iniciarSuscripcion(tenant, request.email());
            return ResponseEntity.ok(new IniciarSuscripcionResponse(urlRegistroTarjeta));
        } catch (BillingException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/tenants/{tenantId}/billing")
    public ResponseEntity<TenantSubscription> verSuscripcion(@PathVariable Long tenantId) {
        if (!PanelAuth.puedeAcceder(tenantId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        Tenant tenant = tenantService.buscarPorId(tenantId).orElse(null);
        if (tenant == null) {
            return ResponseEntity.notFound().build();
        }
        TenantSubscription suscripcion = subscriptionBillingService.buscarPorTenant(tenant);
        return suscripcion != null ? ResponseEntity.ok(suscripcion) : ResponseEntity.notFound().build();
    }

    /**
     * Resumen para el gating del panel (doc sección 12): mientras la
     * suscripción no esté ACTIVA, o el admin todavía no le aprovisionó ningún
     * canal (WhatsApp/Instagram - hoy sigue siendo manual, ver
     * InstagramController), el panel muestra "completá tu pago"/"estamos
     * activando tu cuenta" en vez del dashboard completo. Centralizado acá en
     * vez de en el frontend para que la regla de negocio ("listo para operar")
     * viva en un solo lugar.
     */
    @GetMapping("/tenants/{tenantId}/estado")
    public ResponseEntity<EstadoCuentaResponse> verEstadoCuenta(@PathVariable Long tenantId) {
        if (!PanelAuth.puedeAcceder(tenantId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        Tenant tenant = tenantService.buscarPorId(tenantId).orElse(null);
        if (tenant == null) {
            return ResponseEntity.notFound().build();
        }
        TenantSubscription suscripcion = subscriptionBillingService.buscarPorTenant(tenant);
        BillingStatus status = suscripcion != null ? suscripcion.getStatus() : null;
        boolean whatsappConfigurado = tenant.getWhatsappNumber() != null && !tenant.getWhatsappNumber().isBlank();
        boolean instagramConfigurado = tenant.isInstagramConfigurado();
        boolean suscripcionActiva = status == BillingStatus.ACTIVA;
        boolean listoParaOperar = suscripcionActiva && (whatsappConfigurado || instagramConfigurado);

        return ResponseEntity.ok(new EstadoCuentaResponse(
                status, suscripcionActiva, whatsappConfigurado, instagramConfigurado, listoParaOperar));
    }

    /** Admin-only: mientras no haya notificación automática de cobro fallido confirmada con Flow, la mora se marca a mano. */
    @PostMapping("/tenants/{tenantId}/billing/marcar-morosa")
    public ResponseEntity<Void> marcarMorosa(@PathVariable Long tenantId) {
        if (!PanelAuth.esAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        Tenant tenant = tenantService.buscarPorId(tenantId).orElse(null);
        if (tenant == null) {
            return ResponseEntity.notFound().build();
        }
        subscriptionBillingService.marcarMorosa(tenant);
        return ResponseEntity.ok().build();
    }

    /**
     * Admin-only: registra que este tenant pagó la mensualidad por
     * transferencia directa (los primeros clientes, antes de tener cuenta
     * Flow propia - doc sección 10 - o cualquier tenant que prefiera pagar
     * así). Re-llamable cada vez que llega una transferencia nueva.
     */
    @PutMapping("/tenants/{tenantId}/billing/manual")
    public ResponseEntity<TenantSubscription> registrarPagoManual(
            @PathVariable Long tenantId, @Valid @RequestBody RegistrarPagoManualRequest request) {
        if (!PanelAuth.esAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        Tenant tenant = tenantService.buscarPorId(tenantId).orElse(null);
        if (tenant == null) {
            return ResponseEntity.notFound().build();
        }
        TenantSubscription suscripcion = subscriptionBillingService.registrarPagoManual(tenant, request.paidUntil());
        return ResponseEntity.ok(suscripcion);
    }

    public record EstadoCuentaResponse(
            BillingStatus subscriptionStatus,
            boolean suscripcionActiva,
            boolean whatsappConfigurado,
            boolean instagramConfigurado,
            boolean listoParaOperar) {
    }

    public record IniciarSuscripcionRequest(@NotBlank @Email String email) {
    }

    public record IniciarSuscripcionResponse(String urlRegistroTarjeta) {
    }

    public record RegistrarPagoManualRequest(@NotNull LocalDate paidUntil) {
    }

    public record ErrorResponse(String mensaje) {
    }
}
