package com.tuapp.repository;

import com.tuapp.model.Conversation;
import com.tuapp.model.Message;
import com.tuapp.model.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    void deleteByConversation_Tenant(Tenant tenant);

    List<Message> findByConversationOrderBySentAtAsc(Conversation conversation);
}
