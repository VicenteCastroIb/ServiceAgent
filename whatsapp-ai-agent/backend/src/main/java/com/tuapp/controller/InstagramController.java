package com.tuapp.controller;

import com.tuapp.model.Tenant;
import com.tuapp.security.PanelAuth;
import com.tuapp.service.InstagramOAuthService;
import com.tuapp.service.TenantService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * API de administración de las credenciales de Instagram del negocio (doc
 * secciones 3 y 5.1: incluido desde el plan Básico). La consume el panel
 * Next.js. Los mensajes entrantes/salientes en sí los maneja
 * InstagramWebhookController - este controller solo carga las credenciales.
 *
 * Admin-only, igual que WooCommerce/Flow (CatalogController/PaymentController):
 * el token de acceso es sensible y un error de UI no debe poder filtrarlo ni
 * dejarlo cambiar por el dueño del negocio directamente.
 */
@RestController
@RequestMapping("/admin/tenants/{tenantId}/instagram")
public class InstagramController {

    /** Los tokens de Instagram vencen a los 60 días - ver InstagramTokenRefreshJob. */
    private static final long VIGENCIA_TOKEN_DIAS_POR_DEFECTO = 60;

    private final TenantService tenantService;
    private final InstagramOAuthService instagramOAuthService;

    public InstagramController(TenantService tenantService, InstagramOAuthService instagramOAuthService) {
        this.tenantService = tenantService;
        this.instagramOAuthService = instagramOAuthService;
    }

    /** Carga manual, admin-only - la forma original v1 (doc sección 11), sigue disponible como respaldo. */
    @PutMapping("/credenciales")
    public ResponseEntity<Tenant> fijarCredenciales(
            @PathVariable Long tenantId, @Valid @RequestBody CredencialesInstagramRequest request) {
        if (!PanelAuth.esAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        Instant vencimiento = Instant.now().plus(VIGENCIA_TOKEN_DIAS_POR_DEFECTO, ChronoUnit.DAYS);
        Tenant tenant = tenantService.fijarCredencialesInstagram(
                tenantId, request.instagramAccountId(), request.accessToken(), vencimiento);
        return ResponseEntity.ok(tenant);
    }

    /**
     * SELF-SERVICE (doc sección 12): el dueño del negocio inicia la conexión
     * de su propia cuenta de Instagram, sin que el admin intervenga (ver
     * InstagramOAuthService). Por eso, a diferencia de /credenciales, NO es
     * admin-only - puedeAcceder alcanza (admin o el dueño de ESE negocio).
     */
    @PostMapping("/oauth/iniciar")
    public ResponseEntity<?> iniciarConexionOAuth(@PathVariable Long tenantId) {
        if (!PanelAuth.puedeAcceder(tenantId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        if (tenantService.buscarPorId(tenantId).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        try {
            String url = instagramOAuthService.generarUrlAutorizacion(tenantId);
            return ResponseEntity.ok(new IniciarOAuthResponse(url));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    public record CredencialesInstagramRequest(
            @NotBlank String instagramAccountId, @NotBlank String accessToken) {
    }

    public record IniciarOAuthResponse(String url) {
    }

    public record ErrorResponse(String mensaje) {
    }
}
