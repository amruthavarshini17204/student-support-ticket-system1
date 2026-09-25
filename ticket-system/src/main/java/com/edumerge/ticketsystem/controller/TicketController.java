package com.edumerge.ticketsystem.controller;

import com.edumerge.ticketsystem.dto.*;
import com.edumerge.ticketsystem.entity.Ticket;
import com.edumerge.ticketsystem.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    @PostMapping
    public TicketResponse create(@Valid @RequestBody CreateTicketRequest req) {
        return ticketService.createTicket(req);
    }

    @GetMapping
    public List<TicketResponse> list(
            @RequestParam(required = false) Ticket.Status status,
            @RequestParam(required = false) Ticket.Priority priority,
            @RequestParam(required = false) Ticket.Category category,
            @RequestParam(required = false) Long assignedToId,
            @RequestParam(required = false) Long raisedById,
            @RequestParam(required = false) Boolean breachedOnly) {
        return ticketService.listTickets(status, priority, category, assignedToId, raisedById, breachedOnly);
    }

    @GetMapping("/{id}")
    public TicketResponse get(@PathVariable Long id) {
        return ticketService.getTicket(id);
    }

    @PutMapping("/{id}/assign")
    public TicketResponse assign(@PathVariable Long id, @RequestBody AssignRequest req) {
        return ticketService.assignTicket(id, req);
    }

    @PutMapping("/{id}/status")
    public TicketResponse updateStatus(@PathVariable Long id, @RequestBody UpdateStatusRequest req) {
        return ticketService.updateStatus(id, req);
    }

    @PostMapping("/{id}/comments")
    public TicketResponse addComment(@PathVariable Long id, @RequestBody CommentRequest req) {
        return ticketService.addComment(id, req);
    }
}
