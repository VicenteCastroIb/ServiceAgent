package com.tuapp.repository;

import com.tuapp.model.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, Long> {
    Optional<Tenant> findByWhatsappNumber(String whatsappNumber);

    Optional<Tenant> findByPanelUsername(String panelUsername);

    /** Para validar unicidad de ownerEmail (ver TenantService.registrarSelfService/actualizarOwnerEmail). */
    Optional<Tenant> findByOwnerEmail(String ownerEmail);

    Optional<Tenant> findByInstagramAccountId(String instagramAccountId);

    /** Para InstagramTokenRefreshJob: tenants con token cargado que vence antes de la fecha dada. */
    List<Tenant> findByInstagramAccountIdIsNotNullAndInstagramTokenExpiresAtBefore(Instant antesDe);
}
