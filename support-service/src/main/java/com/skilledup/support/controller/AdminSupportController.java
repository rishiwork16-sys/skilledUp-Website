package com.skilledup.support.controller;

import com.skilledup.support.model.Ticket;
import com.skilledup.support.service.AdminSupportService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/support/admin")
@RequiredArgsConstructor
public class AdminSupportController {

    private final AdminSupportService adminSupportService;

    @GetMapping
    public ResponseEntity<List<Ticket>> getAllTickets() {
        return ResponseEntity.ok(adminSupportService.getAllTickets());
    }

    @GetMapping("/open")
    public ResponseEntity<List<Ticket>> getOpenTickets() {
        return ResponseEntity.ok(adminSupportService.getOpenTickets());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Ticket> getTicketById(@PathVariable Long id) {
        return ResponseEntity.ok(adminSupportService.getTicketById(id));
    }

    @PostMapping("/{id}/reply")
    public ResponseEntity<Ticket> replyToTicket(
            @PathVariable Long id,
            @RequestBody ReplyRequest request) {
        return ResponseEntity.ok(adminSupportService.replyToTicket(id, request.getReply()));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Ticket> updateStatus(
            @PathVariable Long id,
            @RequestBody StatusRequest request) {
        return ResponseEntity.ok(adminSupportService.updateTicketStatus(id, request.getStatus()));
    }

    @PostMapping("/{id}/close")
    public ResponseEntity<Ticket> closeTicket(@PathVariable Long id) {
        return ResponseEntity.ok(adminSupportService.closeTicket(id));
    }

    @GetMapping("/count/open")
    public ResponseEntity<Long> getOpenTicketsCount() {
        return ResponseEntity.ok(adminSupportService.getOpenTicketsCount());
    }

    @Data
    static class ReplyRequest {
        private String reply;
    }

    @Data
    static class StatusRequest {
        private Ticket.TicketStatus status;
    }
}
