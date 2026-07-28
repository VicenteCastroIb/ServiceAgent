package com.tuapp.repository;

import com.tuapp.model.Product;
import com.tuapp.model.Tenant;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    void deleteByTenant(Tenant tenant);

    List<Product> findByTenant(Tenant tenant);

    List<Product> findByTenantAndActiveTrue(Tenant tenant);

    Optional<Product> findByTenantAndExternalId(Tenant tenant, Long externalId);

    long countByTenantAndActiveTrue(Tenant tenant);

    /**
     * Categorías distintas del catálogo activo de un tenant, para orientar al
     * modelo en buscar_productos (ver AiResponseService). @Query explícita en
     * vez de derivarla del nombre del método: Spring Data no soporta
     * proyectar una sola columna (category) a partir del nombre de un método
     * findBy - solo reconoce keywords como Distinct/Top, no "qué propiedad
     * devolver", así que hacía falta esto para no arriesgar un mismatch de
     * tipos en runtime.
     */
    @Query("SELECT DISTINCT p.category FROM Product p WHERE p.tenant = :tenant AND p.active = true AND p.category IS NOT NULL")
    List<String> listarCategoriasDistintas(@Param("tenant") Tenant tenant);

    /**
     * Búsqueda filtrada para catálogos grandes (ver AiResponseService.ejecutarBuscarProductos):
     * categoria/subcategoria/texto son opcionales (null = no filtra por ese
     * campo) - LOWER(...) LIKE para que no importen mayúsculas ni que el
     * texto sea exacto. Devuelve como máximo "limite" filas (Pageable), para
     * no volver a mandarle al modelo un catálogo entero disfrazado de
     * "búsqueda" si el filtro queda demasiado amplio.
     */
    @Query("""
            SELECT p FROM Product p
            WHERE p.tenant = :tenant
              AND p.active = true
              AND (:categoria IS NULL OR LOWER(p.category) = LOWER(:categoria))
              AND (:subcategoria IS NULL OR LOWER(p.subcategory) = LOWER(:subcategoria))
              AND (:texto IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :texto, '%')))
            ORDER BY p.name ASC
            """)
    List<Product> buscar(
            @Param("tenant") Tenant tenant,
            @Param("categoria") String categoria,
            @Param("subcategoria") String subcategoria,
            @Param("texto") String texto,
            Pageable limite);
}
