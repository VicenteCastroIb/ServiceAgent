package com.tuapp.controller;

import com.tuapp.model.Tenant;
import com.tuapp.service.TenantService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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
 * TODO: reemplazar el login in-memory generado por autenticación real por
 * dueño de negocio.
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
        return tenantService.listar();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Tenant> buscarPorId(@PathVariable Long id) {
        return tenantService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Tenant> crear(@Valid @RequestBody CrearTenantRequest request) {
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
        return ResponseEntity.ok(tenantService.actualizarContexto(id, request.businessContext()));
    }

    public record CrearTenantRequest(
            @NotBlank String businessName,
            @NotBlank String whatsappNumber,
            @NotBlank String businessContext) {
    }

    public record ActualizarContextoRequest(@NotBlank String businessContext) {
    }
}
