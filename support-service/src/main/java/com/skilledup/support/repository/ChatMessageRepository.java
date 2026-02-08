package com.skilledup.support.repository;

import com.skilledup.support.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByTicketIdOrderByTimestampAsc(Long ticketId);
}
