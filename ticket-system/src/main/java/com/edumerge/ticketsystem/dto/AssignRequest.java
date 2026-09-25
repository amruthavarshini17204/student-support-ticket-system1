package com.edumerge.ticketsystem.dto;

import lombok.Data;

@Data
public class AssignRequest {
    private Long assignedToId;
    private Long actorId; // who performed the assignment (for the activity log)
}
