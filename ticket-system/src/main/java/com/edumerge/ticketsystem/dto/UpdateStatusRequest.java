package com.edumerge.ticketsystem.dto;

import com.edumerge.ticketsystem.entity.Ticket;
import lombok.Data;

@Data
public class UpdateStatusRequest {
    private Ticket.Status status;
    private Long actorId;
    private String note;
}
