package com.tuapp.controller;

import com.tuapp.model.PaymentOrder;
import com.tuapp.model.Tenant;
import com.tuapp.security.PanelAuth;
import com.tuapp.service.PaymentService;
import com.tuapp.service.TenantService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API de administración de pagos (plan Catálogo/Ecommerce, doc secciones 3,
 * 5.1, 5.2 y 11): credenciales de Flow del negocio y listado de órdenes de
 * pago generadas por la tool generar_link_pago. La consume el panel Next.js.
 *
 * Autorización por tenant igual que CatalogController: cargar/editar las
 * credenciales de Flow queda admin-only; ver el estado de configuración y las
 * órdenes lo puede hacer también el dueño de ese negocio.
 */
@RestController
@RequestMapping("/admin/tenants/{tenantId}/pagos")
public class PaymentController {

    private final TenantService tenantService;
    private final PaymentService paymentService;

    public PaymentController(TenantService tenantService, PaymentService paymentService) {
        this.tenantService = tenantService;
        this.paymentService = paymentService;
    }

    @PutMapping("/flow")
    public ResponseEntity<Tenant> fijarCredenciales(
            @PathVariable Long tenantId, @Valid @RequestBody CredencialesFlowRequest request) {
        if (!PanelAuth.esAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        Tenant tenant = tenantService.fijarCredencialesFlow(tenantId, request.apiKey(), request.secretKey());
        return ResponseEntity.ok(tenant);
    }

    @GetMapping("/ordenes")
    public ResponseEntity<List<PaymentOrder>> listarOrdenes(@PathVariable Long tenantId) {
        if (!PanelAuth.puedeAcceder(tenantId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        Tenant tenant = buscarTenant(tenantId);
        if (tenant == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(paymentService.listarOrdenes(tenant));
    }

    private Tenant buscarTenant(Long tenantId) {
        return tenantService.buscarPorId(tenantId).orElse(null);
    }

    public record CredencialesFlowRequest(@NotBlank String apiKey, @NotBlank String secretKey) {
    }
}
