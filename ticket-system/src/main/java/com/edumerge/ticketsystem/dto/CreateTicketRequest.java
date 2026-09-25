package com.edumerge.ticketsystem.dto;

import com.edumerge.ticketsystem.entity.Ticket;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateTicketRequest {
    @NotBlank
    private String title;
    private String description;
    @NotNull
    private Ticket.Category category;
    private Ticket.Priority priority; // optional; defaults to MEDIUM if not supplied
    @NotNull
    private Long raisedById;
}
