package com.tuapp.repository;

import com.tuapp.model.Product;
import com.tuapp.model.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    void deleteByTenant(Tenant tenant);

    List<Product> findByTenant(Tenant tenant);

    List<Product> findByTenantAndActiveTrue(Tenant tenant);

    Optional<Product> findByTenantAndExternalId(Tenant tenant, Long externalId);
}
