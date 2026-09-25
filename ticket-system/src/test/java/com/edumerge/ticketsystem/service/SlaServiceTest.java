package com.edumerge.ticketsystem.service;

import com.edumerge.ticketsystem.entity.Ticket;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class SlaServiceTest {

    private final SlaService slaService = new SlaService();

    @Test
    void computeDueAt_addsCorrectHoursForPriority() {
        LocalDateTime created = LocalDateTime.of(2026, 9, 25, 9, 0);
        assertEquals(created.plusHours(4), slaService.computeDueAt(created, Ticket.Priority.URGENT));
        assertEquals(created.plusHours(24), slaService.computeDueAt(created, Ticket.Priority.HIGH));
        assertEquals(created.plusHours(72), slaService.computeDueAt(created, Ticket.Priority.MEDIUM));
        assertEquals(created.plusHours(120), slaService.computeDueAt(created, Ticket.Priority.LOW));
    }

    @Test
    void isBreached_trueWhenOpenPastDueDate() {
        LocalDateTime created = LocalDateTime.now().minusHours(10);
        Ticket ticket = Ticket.builder()
                .status(Ticket.Status.OPEN)
                .priority(Ticket.Priority.URGENT) // 4h SLA, created 10h ago -> breached
                .createdAt(created)
                .dueAt(slaService.computeDueAt(created, Ticket.Priority.URGENT))
                .build();
        assertTrue(slaService.isBreached(ticket));
    }

    @Test
    void isBreached_falseWhenResolvedBeforeDueDate() {
        LocalDateTime created = LocalDateTime.now().minusHours(10);
        Ticket ticket = Ticket.builder()
                .status(Ticket.Status.RESOLVED)
                .priority(Ticket.Priority.LOW) // 120h SLA, resolved after 10h -> within SLA
                .createdAt(created)
                .dueAt(slaService.computeDueAt(created, Ticket.Priority.LOW))
                .resolvedAt(LocalDateTime.now())
                .build();
        assertFalse(slaService.isBreached(ticket));
    }

    @Test
    void escalateIfNeeded_bumpsPriorityPastEightyPercentOfWindow() {
        // MEDIUM has a 72h window; 60h elapsed is >80% (57.6h threshold) -> should escalate to HIGH
        LocalDateTime created = LocalDateTime.now().minusHours(60);
        Ticket ticket = Ticket.builder()
                .status(Ticket.Status.OPEN)
                .priority(Ticket.Priority.MEDIUM)
                .createdAt(created)
                .build();
        assertEquals(Ticket.Priority.HIGH, slaService.escalateIfNeeded(ticket));
    }

    @Test
    void escalateIfNeeded_noChangeForResolvedTickets() {
        LocalDateTime created = LocalDateTime.now().minusHours(200);
        Ticket ticket = Ticket.builder()
                .status(Ticket.Status.RESOLVED)
                .priority(Ticket.Priority.LOW)
                .createdAt(created)
                .build();
        assertEquals(Ticket.Priority.LOW, slaService.escalateIfNeeded(ticket));
    }

    @Test
    void escalateIfNeeded_urgentStaysUrgent() {
        LocalDateTime created = LocalDateTime.now().minusHours(3);
        Ticket ticket = Ticket.builder()
                .status(Ticket.Status.OPEN)
                .priority(Ticket.Priority.URGENT)
                .createdAt(created)
                .build();
        assertEquals(Ticket.Priority.URGENT, slaService.escalateIfNeeded(ticket));
    }
}
