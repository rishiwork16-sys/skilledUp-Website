package com.skilledup.support.repository;

import com.skilledup.support.model.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    List<Ticket> findByStudentId(Long studentId);

    List<Ticket> findByStatus(Ticket.TicketStatus status);

    long countByStatus(Ticket.TicketStatus status);
}
