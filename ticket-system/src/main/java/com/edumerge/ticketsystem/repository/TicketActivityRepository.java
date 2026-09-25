package com.edumerge.ticketsystem.repository;

import com.edumerge.ticketsystem.entity.TicketActivity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketActivityRepository extends JpaRepository<TicketActivity, Long> {
    List<TicketActivity> findByTicketIdOrderByCreatedAtAsc(Long ticketId);
}
