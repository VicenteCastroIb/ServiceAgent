package com.tuapp.repository;

import com.tuapp.model.PaymentOrder;
import com.tuapp.model.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, Long> {

    Optional<PaymentOrder> findByFlowToken(String flowToken);

    List<PaymentOrder> findByTenant(Tenant tenant);

    void deleteByTenant(Tenant tenant);
}
