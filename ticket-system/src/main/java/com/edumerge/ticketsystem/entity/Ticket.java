package com.edumerge.ticketsystem.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ticket")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Ticket {

    public enum Category { FEES, ATTENDANCE, ID_CARD, DOCUMENTS, CERTIFICATES, OTHER }

    public enum Priority { LOW, MEDIUM, HIGH, URGENT }

    // OPEN -> IN_PROGRESS -> (PENDING_STUDENT <-> IN_PROGRESS) -> RESOLVED -> CLOSED
    // CLOSED is terminal; RESOLVED can be reopened back to IN_PROGRESS.
    public enum Status { OPEN, IN_PROGRESS, PENDING_STUDENT, RESOLVED, CLOSED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "raised_by_id", nullable = false)
    private User raisedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_id")
    private User assignedTo;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // SLA deadline, computed at creation time from priority
    private LocalDateTime dueAt;

    private LocalDateTime resolvedAt;

    private LocalDateTime closedAt;
}
