package com.tuapp.controller;

import com.tuapp.model.Tenant;
import com.tuapp.model.TenantPlan;
import com.tuapp.security.PanelAuth;
import com.tuapp.service.TenantService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API de administración de tenants (negocios/locales). Semana 3: reemplaza
 * la carga hardcodeada de contexto por alta real en base de datos.
 *
 * Protegida por JWT (ver SecurityConfig): requiere header
 * "Authorization: Bearer &lt;token&gt;", emitido por AuthController tras
 * /auth/login. La consume el panel Next.js (panel-frontend/).
 *
 * Autorización por tenant (ver PanelAuth): el admin ve/edita todo; el dueño
 * de un negocio (login propio, TenantService.fijarCredencialesPanel) solo ve
 * y edita el suyo. Alta de negocios, cambio de plan y credenciales de panel
 * quedan admin-only - un dueño no debería poder auto-subirse de plan ni
 * crear negocios ajenos.
 */
@RestController
@RequestMapping("/admin/tenants")
public class TenantController {

    private final TenantService tenantService;

    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @GetMapping
    public List<Tenant> listar() {
        if (PanelAuth.esAdmin()) {
            return tenantService.listar();
        }
        // Un dueño de negocio solo ve el suyo (mismo formato que el admin: una lista).
        return tenantService.buscarPorId(PanelAuth.tenantIdActual())
                .map(List::of)
                .orElse(List.of());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Tenant> buscarPorId(@PathVariable Long id) {
        if (!PanelAuth.puedeAcceder(id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return tenantService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Tenant> crear(@Valid @RequestBody CrearTenantRequest request) {
        if (!PanelAuth.esAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        Tenant tenant = tenantService.crear(
                request.businessName(),
                request.whatsappNumber(),
                request.businessContext());
        return ResponseEntity.status(HttpStatus.CREATED).body(tenant);
    }

    @PutMapping("/{id}/contexto")
    public ResponseEntity<Tenant> actualizarContexto(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarContextoRequest request) {
        if (!PanelAuth.puedeAcceder(id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(tenantService.actualizarContexto(id, request.businessContext()));
    }

    @PutMapping("/{id}/plan")
    public ResponseEntity<Tenant> actualizarPlan(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarPlanRequest request) {
        if (!PanelAuth.esAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(tenantService.actualizarPlan(id, request.plan()));
    }

    @PutMapping("/{id}/credenciales")
    public ResponseEntity<Tenant> fijarCredenciales(
            @PathVariable Long id,
            @Valid @RequestBody FijarCredencialesRequest request) {
        if (!PanelAuth.esAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(
                tenantService.fijarCredencialesPanel(id, request.panelUsername(), request.panelPassword()));
    }

    public record CrearTenantRequest(
            @NotBlank String businessName,
            @NotBlank String whatsappNumber,
            @NotBlank String businessContext) {
    }

    public record ActualizarContextoRequest(@NotBlank String businessContext) {
    }

    public record ActualizarPlanRequest(@NotNull TenantPlan plan) {
    }

    public record FijarCredencialesRequest(@NotBlank String panelUsername, @NotBlank String panelPassword) {
    }
}
