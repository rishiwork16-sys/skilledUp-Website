package com.skilledup.support.service;

import com.skilledup.support.dto.ChatMessageDto;
import com.skilledup.support.model.ChatMessage;
import com.skilledup.support.model.Ticket;
import com.skilledup.support.repository.ChatMessageRepository;
import com.skilledup.support.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SupportService {

    private final TicketRepository ticketRepository;
    private final ChatMessageRepository chatMessageRepository;

    public Ticket createTicket(Ticket ticket) {
        return ticketRepository.save(ticket);
    }

    public List<Ticket> getMyTickets(Long studentId) {
        return ticketRepository.findByStudentId(studentId);
    }

    public List<Ticket> getAllTickets() {
        return ticketRepository.findAll();
    }

    public Ticket getTicket(Long ticketId) {
        return ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));
    }

    @Transactional
    public ChatMessage saveMessage(ChatMessageDto messageDto) {
        ChatMessage message = ChatMessage.builder()
                .ticketId(messageDto.getTicketId())
                .senderId(messageDto.getSenderId())
                .content(messageDto.getContent())
                .timestamp(Instant.now())
                .isAdmin(messageDto.isAdmin())
                .build();

        return chatMessageRepository.save(message);
    }

    public List<ChatMessage> getChatHistory(Long ticketId) {
        return chatMessageRepository.findByTicketIdOrderByTimestampAsc(ticketId);
    }

    @Transactional
    public Ticket updateTicketStatus(Long ticketId, Ticket.TicketStatus status) {
        Ticket ticket = getTicket(ticketId);
        ticket.setStatus(status);
        ticket.setUpdatedAt(Instant.now());
        return ticketRepository.save(ticket);
    }
}
