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
 *
 * category/subcategory: para catálogos grandes (cientos de productos con
 * variaciones), AiResponseService no vuelca el catálogo completo al prompt -
 * usa el tool buscar_productos filtrando por estos campos, para no gastar
 * contexto/precisión revisando categorías que no tienen nada que ver con lo
 * que pidió el cliente (ej: "quiero una polera negra" -> categoria=Ropa,
 * subcategoria=Poleras, en vez de repasar zapatillas, accesorios, etc.).
 * En la sincronización de WooCommerce, category sale de la primera categoría
 * que trae el producto (WooCommerce no distingue categoría/subcategoría en el
 * endpoint de productos, son todas "categories" planas) - subcategory queda
 * sin sincronizar automáticamente, se puede completar a mano desde el panel.
 * En productos cargados manualmente (externalId null, ver CatalogController)
 * el dueño carga ambos campos directamente.
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

    /** Ver Javadoc de la clase - usados por AiResponseService/buscar_productos para filtrar catálogos grandes. */
    private String category;

    private String subcategory;

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
