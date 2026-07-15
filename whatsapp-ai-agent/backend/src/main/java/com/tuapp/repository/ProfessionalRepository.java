package com.tuapp.repository;

import com.tuapp.model.Professional;
import com.tuapp.model.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProfessionalRepository extends JpaRepository<Professional, Long> {

    List<Professional> findByTenant(Tenant tenant);

    List<Professional> findByTenantAndActiveTrue(Tenant tenant);

    void deleteByTenant(Tenant tenant);
}
