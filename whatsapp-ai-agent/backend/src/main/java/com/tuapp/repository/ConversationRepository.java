package com.tuapp.repository;

import com.tuapp.model.Conversation;
import com.tuapp.model.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    void deleteByTenant(Tenant tenant);
}
