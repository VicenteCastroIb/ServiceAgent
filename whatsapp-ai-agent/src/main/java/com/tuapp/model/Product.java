package com.tuapp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Producto del catálogo de un Tenant (plan Catálogo/Ecommerce, ver doc sección 3 y 5.3).
 * Usado por CatalogSyncService para sincronizar con la tienda online (WooCommerce u otra).
 *
 * TODO: foto, link de compra, stock, sincronización con tienda externa.
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
}
