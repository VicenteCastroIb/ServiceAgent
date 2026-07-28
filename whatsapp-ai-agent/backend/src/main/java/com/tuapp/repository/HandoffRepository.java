package com.tuapp.repository;

import com.tuapp.model.Handoff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HandoffRepository extends JpaRepository<Handoff, Long> {

    Optional<Handoff> findByNumeroCliente(String numeroCliente);

    boolean existsByNumeroCliente(String numeroCliente);

    /** Para el panel del dueño de un negocio (ver HandoffService.listarPausadas). */
    List<Handoff> findByTenantId(Long tenantId);

    /** Al borrar un tenant completo (ver TenantService.eliminar). */
    void deleteByTenantId(Long tenantId);
}
