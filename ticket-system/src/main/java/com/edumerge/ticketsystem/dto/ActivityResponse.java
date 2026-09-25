package com.edumerge.ticketsystem.dto;

import com.edumerge.ticketsystem.entity.TicketActivity;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ActivityResponse {
    private Long id;
    private TicketActivity.ActivityType type;
    private String message;
    private String actorName;
    private LocalDateTime createdAt;
}
