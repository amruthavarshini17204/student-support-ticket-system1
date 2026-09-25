package com.edumerge.ticketsystem.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ticket_activity")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TicketActivity {

    public enum ActivityType { CREATED, COMMENT, STATUS_CHANGE, ASSIGNMENT, ESCALATED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private User actor; // nullable: system-generated entries (e.g. auto-escalation) have no actor

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActivityType type;

    @Column(length = 1000)
    private String message;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
