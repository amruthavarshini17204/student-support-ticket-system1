package com.edumerge.ticketsystem.dto;

import com.edumerge.ticketsystem.entity.Ticket;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class TicketResponse {
    private Long id;
    private String title;
    private String description;
    private Ticket.Category category;
    private Ticket.Priority priority;
    private Ticket.Status status;
    private String raisedByName;
    private Long raisedById;
    private String assignedToName;
    private Long assignedToId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime dueAt;
    private LocalDateTime resolvedAt;
    private boolean breached;
    private long ageHours;
    private List<ActivityResponse> activity;
}
