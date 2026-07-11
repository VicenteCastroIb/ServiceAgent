package com.tuapp.repository;

import com.tuapp.model.Tenant;
import com.tuapp.model.TenantSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TenantSubscriptionRepository extends JpaRepository<TenantSubscription, Long> {

    Optional<TenantSubscription> findByTenant(Tenant tenant);

    Optional<TenantSubscription> findByFlowCustomerId(String flowCustomerId);

    /** Puede haber más de un match (poco probable pero posible) - ver SubscriptionBillingService.procesarNotificacionPago. */
    List<TenantSubscription> findByBillingEmail(String billingEmail);

    void deleteByTenant(Tenant tenant);
}
