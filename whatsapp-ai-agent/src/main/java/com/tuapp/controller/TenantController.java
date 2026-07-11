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
 * Protegido por la configuración default de SecurityConfig (requiere login -
 * ver la contraseña generada en el log al arrancar la app). No hay panel
 * visual todavía: se usa con curl/Postman hasta que se construya el panel
 * web con Thymeleaf (segunda mitad de la Semana 3).
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
