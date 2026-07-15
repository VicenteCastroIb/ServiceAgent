package com.tuapp.controller;

import com.tuapp.model.Tenant;
import com.tuapp.security.PanelAuth;
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

    public InstagramController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

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

    public record CredencialesInstagramRequest(
            @NotBlank String instagramAccountId, @NotBlank String accessToken) {
    }
}
