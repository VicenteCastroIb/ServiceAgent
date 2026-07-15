package com.tuapp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Producto del catálogo de un Tenant (plan Catálogo/Ecommerce, ver doc sección 3 y 5.3).
 * Usado por CatalogSyncService para sincronizar con la tienda online (WooCommerce),
 * y por AiResponseService/PaymentService para recomendar productos y armar el
 * link de pago (generar_link_pago).
 *
 * Semana 6: externalId/imageUrl/purchaseUrl/stockQuantity/active/updatedAt se
 * llenan al sincronizar con WooCommerce (ver CatalogSyncService). externalId
 * es el id del producto EN WooCommerce, no el nuestro - se usa para hacer
 * upsert en cada sincronización sin duplicar productos.
 */
@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;

    private String name;

    private BigDecimal price;

    /** Id del producto en WooCommerce (para upsert en la sincronización). Nulo si se cargó a mano. */
    private Long externalId;

    private String imageUrl;

    /** Link a la ficha del producto en la tienda online del negocio. */
    private String purchaseUrl;

    private Integer stockQuantity;

    /** false si WooCommerce ya no lo devuelve como publicado (se desactiva, no se borra). */
    private boolean active = true;

    private Instant updatedAt;
}
