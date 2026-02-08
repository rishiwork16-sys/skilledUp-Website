package com.skilledup.support.controller;

import com.skilledup.support.dto.ApiMessage;
import com.skilledup.support.dto.ChatMessageDto;
import com.skilledup.support.model.ChatMessage;
import com.skilledup.support.model.Ticket;
import com.skilledup.support.service.SupportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller // Note: @Controller for both REST and WebSocket
@RequiredArgsConstructor
public class SupportController {

    private final SupportService supportService;

    // --- WebSocket Endpoints ---

    @MessageMapping("/chat/{ticketId}")
    @SendTo("/topic/ticket/{ticketId}")
    public ChatMessageDto sendMessage(@DestinationVariable Long ticketId, @Payload ChatMessageDto messageDto) {
        supportService.saveMessage(messageDto);
        return messageDto;
    }

    // --- REST Endpoints ---

    // --- REST Endpoints ---

    @PostMapping("/api/support/tickets")
    @ResponseBody
    public ResponseEntity<Ticket> createTicket(@RequestBody Ticket ticket) {
        return ResponseEntity.ok(supportService.createTicket(ticket));
    }

    @GetMapping("/api/support/tickets/my-tickets")
    @ResponseBody
    public ResponseEntity<List<Ticket>> getMyTickets(@RequestParam Long studentId) {
        return ResponseEntity.ok(supportService.getMyTickets(studentId));
    }

    @GetMapping("/api/support/tickets/{ticketId}/messages")
    @ResponseBody
    public ResponseEntity<List<ChatMessage>> getChatHistory(@PathVariable Long ticketId) {
        return ResponseEntity.ok(supportService.getChatHistory(ticketId));
    }

    @GetMapping("/api/support/health")
    @ResponseBody
    public ResponseEntity<ApiMessage> health() {
        return ResponseEntity.ok(new ApiMessage("Support Service is running"));
    }

    @PostMapping("/api/support/tickets/{ticketId}/reply")
    @ResponseBody
    public ResponseEntity<ChatMessageDto> replyToTicket(@PathVariable Long ticketId,
            @RequestBody ChatMessageDto messageDto) {
        messageDto.setTicketId(ticketId);
        supportService.saveMessage(messageDto);
        return ResponseEntity.ok(messageDto);
    }
}
