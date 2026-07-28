package com.tuapp.controller;

import com.tuapp.model.Product;
import com.tuapp.model.Tenant;
import com.tuapp.security.PanelAuth;
import com.tuapp.service.CatalogSyncException;
import com.tuapp.service.CatalogSyncService;
import com.tuapp.service.TenantService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * API de administración del catálogo (plan Catálogo/Ecommerce, doc secciones
 * 3, 5.2 y 5.3): credenciales de WooCommerce del negocio, sincronización
 * manual y listado de productos ya sincronizados. La consume el panel Next.js.
 *
 * Autorización por tenant igual que el resto de /admin/** (ver PanelAuth):
 * cargar/editar las credenciales de WooCommerce queda admin-only (evita que
 * un error de UI filtre claves ajenas); ver el estado de configuración,
 * disparar la sincronización y listar productos lo puede hacer también el
 * dueño de ese negocio.
 */
@RestController
@RequestMapping("/admin/tenants/{tenantId}/catalogo")
public class CatalogController {

    private final TenantService tenantService;
    private final CatalogSyncService catalogSyncService;

    public CatalogController(TenantService tenantService, CatalogSyncService catalogSyncService) {
        this.tenantService = tenantService;
        this.catalogSyncService = catalogSyncService;
    }

    @PutMapping("/woocommerce")
    public ResponseEntity<Tenant> fijarCredenciales(
            @PathVariable Long tenantId, @Valid @RequestBody CredencialesWooCommerceRequest request) {
        if (!PanelAuth.esAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        Tenant tenant = tenantService.fijarCredencialesWooCommerce(
                tenantId, request.url(), request.consumerKey(), request.consumerSecret());
        return ResponseEntity.ok(tenant);
    }

    @PostMapping("/sincronizar")
    public ResponseEntity<?> sincronizar(@PathVariable Long tenantId) {
        if (!PanelAuth.puedeAcceder(tenantId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        Tenant tenant = buscarTenant(tenantId);
        if (tenant == null) {
            return ResponseEntity.notFound().build();
        }
        try {
            int cantidad = catalogSyncService.sincronizar(tenant);
            return ResponseEntity.ok(new SincronizacionResponse(cantidad));
        } catch (CatalogSyncException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/productos")
    public ResponseEntity<List<Product>> listarProductos(@PathVariable Long tenantId) {
        if (!PanelAuth.puedeAcceder(tenantId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        Tenant tenant = buscarTenant(tenantId);
        if (tenant == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(catalogSyncService.listarProductos(tenant));
    }

    /**
     * Alta manual de un producto (doc FAQ de la landing: "si todavía no
     * tenés tienda online, podés cargar el catálogo manualmente desde el
     * panel"). Mismo criterio de autorización que /contexto en TenantController
     * (PanelAuth.puedeAcceder): lo puede hacer el admin o el propio dueño del
     * negocio, no queda admin-only como las credenciales de WooCommerce/Flow
     * (esto es contenido del catálogo, no un secreto).
     */
    @PostMapping("/productos")
    public ResponseEntity<?> crearProducto(@PathVariable Long tenantId, @Valid @RequestBody ProductoRequest request) {
        if (!PanelAuth.puedeAcceder(tenantId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        Tenant tenant = buscarTenant(tenantId);
        if (tenant == null) {
            return ResponseEntity.notFound().build();
        }
        Product producto = catalogSyncService.crearProductoManual(
                tenant, request.name(), request.price(), request.category(), request.subcategory(), request.stockQuantity());
        return ResponseEntity.status(HttpStatus.CREATED).body(producto);
    }

    @PutMapping("/productos/{productId}")
    public ResponseEntity<?> actualizarProducto(
            @PathVariable Long tenantId, @PathVariable Long productId, @Valid @RequestBody ActualizarProductoRequest request) {
        if (!PanelAuth.puedeAcceder(tenantId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        Tenant tenant = buscarTenant(tenantId);
        if (tenant == null) {
            return ResponseEntity.notFound().build();
        }
        try {
            Product producto = catalogSyncService.actualizarProducto(
                    tenant, productId, request.name(), request.price(), request.category(),
                    request.subcategory(), request.stockQuantity(), request.active());
            return ResponseEntity.ok(producto);
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/productos/{productId}")
    public ResponseEntity<Void> eliminarProducto(@PathVariable Long tenantId, @PathVariable Long productId) {
        if (!PanelAuth.puedeAcceder(tenantId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        Tenant tenant = buscarTenant(tenantId);
        if (tenant == null) {
            return ResponseEntity.notFound().build();
        }
        try {
            catalogSyncService.eliminarProducto(tenant, productId);
            return ResponseEntity.noContent().build();
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    private Tenant buscarTenant(Long tenantId) {
        return tenantService.buscarPorId(tenantId).orElse(null);
    }

    public record CredencialesWooCommerceRequest(
            @NotBlank String url, @NotBlank String consumerKey, @NotBlank String consumerSecret) {
    }

    public record SincronizacionResponse(int productosSincronizados) {
    }

    public record ErrorResponse(String mensaje) {
    }

    public record ProductoRequest(
            @NotBlank String name,
            @NotNull @PositiveOrZero BigDecimal price,
            String category,
            String subcategory,
            @PositiveOrZero Integer stockQuantity) {
    }

    public record ActualizarProductoRequest(
            @NotBlank String name,
            @NotNull @PositiveOrZero BigDecimal price,
            String category,
            String subcategory,
            @PositiveOrZero Integer stockQuantity,
            boolean active) {
    }
}
