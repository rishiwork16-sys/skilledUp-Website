package com.skilledup.support.service;

import com.skilledup.support.model.ChatMessage;
import com.skilledup.support.model.Ticket;
import com.skilledup.support.repository.ChatMessageRepository;
import com.skilledup.support.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminSupportService {

    private final TicketRepository ticketRepository;
    private final ChatMessageRepository chatMessageRepository;

    public List<Ticket> getAllTickets() {
        return ticketRepository.findAll();
    }

    public List<Ticket> getOpenTickets() {
        return ticketRepository.findByStatus(Ticket.TicketStatus.OPEN);
    }

    public Ticket getTicketById(Long id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));
    }

    @Transactional
    public Ticket replyToTicket(Long ticketId, String adminReply) {
        Ticket ticket = getTicketById(ticketId);

        // Create Admin Message
        ChatMessage message = ChatMessage.builder()
                .ticketId(ticketId)
                .senderId("ADMIN")
                .content(adminReply)
                .timestamp(Instant.now())
                .isAdmin(true)
                .build();

        chatMessageRepository.save(message);

        // Update Ticket Status
        ticket.setStatus(Ticket.TicketStatus.RESOLVED); // Or IN_PROGRESS/RESOLVED based on workflow
        ticket.setUpdatedAt(Instant.now());

        Ticket updated = ticketRepository.save(ticket);
        log.info("Admin replied to ticket {}", ticketId);
        return updated;
    }

    @Transactional
    public Ticket updateTicketStatus(Long ticketId, Ticket.TicketStatus newStatus) {
        Ticket ticket = getTicketById(ticketId);
        ticket.setStatus(newStatus);
        ticket.setUpdatedAt(Instant.now());

        Ticket updated = ticketRepository.save(ticket);
        log.info("Updated ticket {} status to {}", ticketId, newStatus);
        return updated;
    }

    @Transactional
    public Ticket closeTicket(Long ticketId) {
        return updateTicketStatus(ticketId, Ticket.TicketStatus.CLOSED);
    }

    public long getOpenTicketsCount() {
        return ticketRepository.countByStatus(Ticket.TicketStatus.OPEN);
    }

    // Auto-delete tickets older than 7 days
    @Scheduled(cron = "0 0 0 * * *") // Run daily at midnight
    @Transactional
    public void deleteExpiredTickets() {
        Instant expirationTime = Instant.now().minus(7, ChronoUnit.DAYS);
        List<Ticket> expiredTickets = ticketRepository.findAll();

        // Filter manually or use a custom query (better for performance, but this is
        // simple)
        // Ideally: ticketRepository.deleteByCreatedAtBefore(expirationTime);

        // For simplicity and to avoid adding repository method right now if not needed:
        expiredTickets.stream()
                .filter(t -> t.getCreatedAt().isBefore(expirationTime))
                .forEach(t -> {
                    ticketRepository.delete(t);
                    log.info("Deleted expired ticket {}", t.getId());
                });
    }
}
