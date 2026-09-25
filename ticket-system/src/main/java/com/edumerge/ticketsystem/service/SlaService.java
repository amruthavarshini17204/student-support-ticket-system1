package com.edumerge.ticketsystem.service;

import com.edumerge.ticketsystem.entity.Ticket;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class SlaService {

    /** SLA window per priority, in hours. Assumption: fixed policy, not per-category. */
    public long slaHoursFor(Ticket.Priority priority) {
        return switch (priority) {
            case URGENT -> 4;
            case HIGH -> 24;
            case MEDIUM -> 72;
            case LOW -> 120;
        };
    }

    public LocalDateTime computeDueAt(LocalDateTime createdAt, Ticket.Priority priority) {
        return createdAt.plusHours(slaHoursFor(priority));
    }

    /** A ticket is breached if it's still open past its due date, or was resolved after it. */
    public boolean isBreached(Ticket ticket) {
        if (ticket.getStatus() == Ticket.Status.RESOLVED || ticket.getStatus() == Ticket.Status.CLOSED) {
            return ticket.getResolvedAt() != null && ticket.getDueAt() != null
                    && ticket.getResolvedAt().isAfter(ticket.getDueAt());
        }
        return ticket.getDueAt() != null && LocalDateTime.now().isAfter(ticket.getDueAt());
    }

    /**
     * Auto-escalation: once 80% of the SLA window has elapsed on an unresolved ticket,
     * bump its priority one level so it surfaces to staff before it actually breaches.
     * URGENT has nowhere higher to escalate to.
     */
    public Ticket.Priority escalateIfNeeded(Ticket ticket) {
        if (ticket.getStatus() == Ticket.Status.RESOLVED || ticket.getStatus() == Ticket.Status.CLOSED) {
            return ticket.getPriority();
        }
        long totalHours = slaHoursFor(ticket.getPriority());
        long elapsed = Duration.between(ticket.getCreatedAt(), LocalDateTime.now()).toHours();
        if (elapsed >= (long) (totalHours * 0.8)) {
            return switch (ticket.getPriority()) {
                case LOW -> Ticket.Priority.MEDIUM;
                case MEDIUM -> Ticket.Priority.HIGH;
                case HIGH -> Ticket.Priority.URGENT;
                case URGENT -> Ticket.Priority.URGENT;
            };
        }
        return ticket.getPriority();
    }
}
