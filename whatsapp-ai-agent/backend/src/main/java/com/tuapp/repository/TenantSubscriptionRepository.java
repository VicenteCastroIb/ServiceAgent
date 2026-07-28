package com.tuapp.repository;

import com.tuapp.model.Tenant;
import com.tuapp.model.TenantSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TenantSubscriptionRepository extends JpaRepository<TenantSubscription, Long> {

    Optional<TenantSubscription> findByTenant(Tenant tenant);

    Optional<TenantSubscription> findByFlowCustomerId(String flowCustomerId);

    /**
     * billingEmail es único (ver TenantSubscription) - a lo sumo un match, sin
     * ambigüedad. Usado por SubscriptionBillingService.procesarNotificacionPago
     * para resolver a qué tenant pertenece un cobro entrante de Flow.
     */
    Optional<TenantSubscription> findByBillingEmail(String billingEmail);

    /** Para validar unicidad antes de guardar (ver SubscriptionBillingService.iniciarSuscripcion). */
    Optional<TenantSubscription> findByBillingEmailAndTenant_IdNot(String billingEmail, Long tenantId);

    void deleteByTenant(Tenant tenant);
}
