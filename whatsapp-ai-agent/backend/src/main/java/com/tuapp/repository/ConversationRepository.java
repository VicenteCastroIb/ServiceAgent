package com.tuapp.repository;

import com.tuapp.model.Conversation;
import com.tuapp.model.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    void deleteByTenant(Tenant tenant);

    Optional<Conversation> findByTenantAndClientContact(Tenant tenant, String clientContact);

    /** Bandeja de un negocio (dueño), ordenada por mensaje más reciente primero. */
    List<Conversation> findByTenantOrderByLastMessageAtDesc(Tenant tenant);

    /** Bandeja global (admin), ordenada igual. */
    List<Conversation> findAllByOrderByLastMessageAtDesc();
}
