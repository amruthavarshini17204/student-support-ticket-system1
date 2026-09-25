package com.edumerge.ticketsystem.service;

import com.edumerge.ticketsystem.dto.*;
import com.edumerge.ticketsystem.entity.*;
import com.edumerge.ticketsystem.exception.ResourceNotFoundException;
import com.edumerge.ticketsystem.repository.TicketActivityRepository;
import com.edumerge.ticketsystem.repository.TicketRepository;
import com.edumerge.ticketsystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;
    private final TicketActivityRepository activityRepository;
    private final UserRepository userRepository;
    private final SlaService slaService;

    @Transactional
    public TicketResponse createTicket(CreateTicketRequest req) {
        User raisedBy = userRepository.findById(req.getRaisedById())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + req.getRaisedById()));

        Ticket.Priority priority = req.getPriority() != null ? req.getPriority() : Ticket.Priority.MEDIUM;
        LocalDateTime now = LocalDateTime.now();

        Ticket ticket = Ticket.builder()
                .title(req.getTitle())
                .description(req.getDescription())
                .category(req.getCategory())
                .priority(priority)
                .status(Ticket.Status.OPEN)
                .raisedBy(raisedBy)
                .createdAt(now)
                .updatedAt(now)
                .dueAt(slaService.computeDueAt(now, priority))
                .build();

        ticket = ticketRepository.save(ticket);
        logActivity(ticket, raisedBy, TicketActivity.ActivityType.CREATED, "Ticket raised: " + req.getTitle());

        return toResponse(ticket, true);
    }

    @Transactional
    public TicketResponse assignTicket(Long ticketId, AssignRequest req) {
        Ticket ticket = getTicketOrThrow(ticketId);
        User assignee = userRepository.findById(req.getAssignedToId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + req.getAssignedToId()));
        User actor = req.getActorId() != null ? userRepository.findById(req.getActorId()).orElse(null) : null;

        ticket.setAssignedTo(assignee);
        if (ticket.getStatus() == Ticket.Status.OPEN) {
            ticket.setStatus(Ticket.Status.IN_PROGRESS);
        }
        ticket.setUpdatedAt(LocalDateTime.now());
        ticketRepository.save(ticket);

        logActivity(ticket, actor, TicketActivity.ActivityType.ASSIGNMENT, "Assigned to " + assignee.getName());
        return toResponse(ticket, true);
    }

    @Transactional
    public TicketResponse updateStatus(Long ticketId, UpdateStatusRequest req) {
        Ticket ticket = getTicketOrThrow(ticketId);
        Ticket.Status oldStatus = ticket.getStatus();
        Ticket.Status newStatus = req.getStatus();

        if (newStatus == null) {
            throw new IllegalArgumentException("status is required");
        }
        if (!isValidTransition(oldStatus, newStatus)) {
            throw new IllegalArgumentException("Cannot move ticket from " + oldStatus + " to " + newStatus);
        }

        ticket.setStatus(newStatus);
        LocalDateTime now = LocalDateTime.now();
        ticket.setUpdatedAt(now);
        if (newStatus == Ticket.Status.RESOLVED) {
            ticket.setResolvedAt(now);
        }
        if (newStatus == Ticket.Status.CLOSED) {
            ticket.setClosedAt(now);
            if (ticket.getResolvedAt() == null) ticket.setResolvedAt(now);
        }
        ticketRepository.save(ticket);

        User actor = req.getActorId() != null ? userRepository.findById(req.getActorId()).orElse(null) : null;
        String msg = "Status changed from " + oldStatus + " to " + newStatus
                + (req.getNote() != null && !req.getNote().isBlank() ? (" \u2014 " + req.getNote()) : "");
        logActivity(ticket, actor, TicketActivity.ActivityType.STATUS_CHANGE, msg);

        return toResponse(ticket, true);
    }

    @Transactional
    public TicketResponse addComment(Long ticketId, CommentRequest req) {
        Ticket ticket = getTicketOrThrow(ticketId);
        User actor = req.getActorId() != null ? userRepository.findById(req.getActorId()).orElse(null) : null;
        ticket.setUpdatedAt(LocalDateTime.now());

        // A student replying while a ticket is waiting on them nudges it back into progress.
        if (ticket.getStatus() == Ticket.Status.PENDING_STUDENT && actor != null && actor.getRole() == Role.STUDENT) {
            ticket.setStatus(Ticket.Status.IN_PROGRESS);
        }
        ticketRepository.save(ticket);
        logActivity(ticket, actor, TicketActivity.ActivityType.COMMENT, req.getMessage());
        return toResponse(ticket, true);
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> listTickets(Ticket.Status status, Ticket.Priority priority,
                                             Ticket.Category category, Long assignedToId,
                                             Long raisedById, Boolean breachedOnly) {
        Specification<Ticket> spec = Specification.where(null);
        if (status != null) spec = spec.and((r, q, cb) -> cb.equal(r.get("status"), status));
        if (priority != null) spec = spec.and((r, q, cb) -> cb.equal(r.get("priority"), priority));
        if (category != null) spec = spec.and((r, q, cb) -> cb.equal(r.get("category"), category));
        if (assignedToId != null) spec = spec.and((r, q, cb) -> cb.equal(r.get("assignedTo").get("id"), assignedToId));
        if (raisedById != null) spec = spec.and((r, q, cb) -> cb.equal(r.get("raisedBy").get("id"), raisedById));

        List<TicketResponse> responses = ticketRepository.findAll(spec).stream()
                .map(t -> toResponse(t, false))
                .collect(Collectors.toList());

        if (Boolean.TRUE.equals(breachedOnly)) {
            responses = responses.stream().filter(TicketResponse::isBreached).collect(Collectors.toList());
        }

        // Breached tickets first, then by priority severity, then newest first.
        responses.sort(Comparator
                .comparing((TicketResponse t) -> !t.isBreached())
                .thenComparing(t -> t.getPriority().ordinal())
                .thenComparing(TicketResponse::getCreatedAt, Comparator.reverseOrder()));
        return responses;
    }

    @Transactional(readOnly = true)
    public TicketResponse getTicket(Long id) {
        return toResponse(getTicketOrThrow(id), true);
    }

    @Transactional(readOnly = true)
    public DashboardStats getStats() {
        List<Ticket> all = ticketRepository.findAll();

        Map<String, Long> byStatus = all.stream()
                .collect(Collectors.groupingBy(t -> t.getStatus().name(), Collectors.counting()));
        Map<String, Long> byPriority = all.stream()
                .collect(Collectors.groupingBy(t -> t.getPriority().name(), Collectors.counting()));
        Map<String, Long> byCategory = all.stream()
                .collect(Collectors.groupingBy(t -> t.getCategory().name(), Collectors.counting()));

        long breached = all.stream().filter(slaService::isBreached).count();

        List<Ticket> resolved = all.stream().filter(t -> t.getResolvedAt() != null).collect(Collectors.toList());
        double avgResolutionHours = resolved.stream()
                .mapToLong(t -> Duration.between(t.getCreatedAt(), t.getResolvedAt()).toHours())
                .average().orElse(0.0);

        long openOlderThan48h = all.stream()
                .filter(t -> t.getStatus() != Ticket.Status.RESOLVED && t.getStatus() != Ticket.Status.CLOSED)
                .filter(t -> Duration.between(t.getCreatedAt(), LocalDateTime.now()).toHours() >= 48)
                .count();

        return DashboardStats.builder()
                .totalTickets(all.size())
                .byStatus(byStatus)
                .byPriority(byPriority)
                .byCategory(byCategory)
                .breachedCount(breached)
                .avgResolutionHours(Math.round(avgResolutionHours * 10.0) / 10.0)
                .openOlderThan48h(openOlderThan48h)
                .build();
    }

    // ---- helpers ----

    private Ticket getTicketOrThrow(Long id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + id));
    }

    private void logActivity(Ticket ticket, User actor, TicketActivity.ActivityType type, String message) {
        activityRepository.save(TicketActivity.builder()
                .ticket(ticket).actor(actor).type(type).message(message)
                .createdAt(LocalDateTime.now()).build());
    }

    private boolean isValidTransition(Ticket.Status from, Ticket.Status to) {
        if (from == to) return true;
        return switch (from) {
            case OPEN -> to == Ticket.Status.IN_PROGRESS || to == Ticket.Status.CLOSED;
            case IN_PROGRESS -> to == Ticket.Status.PENDING_STUDENT || to == Ticket.Status.RESOLVED || to == Ticket.Status.CLOSED;
            case PENDING_STUDENT -> to == Ticket.Status.IN_PROGRESS || to == Ticket.Status.CLOSED;
            case RESOLVED -> to == Ticket.Status.CLOSED || to == Ticket.Status.IN_PROGRESS; // reopen
            case CLOSED -> false; // terminal
        };
    }

    private TicketResponse toResponse(Ticket t, boolean includeActivity) {
        boolean breached = slaService.isBreached(t);
        long ageHours = Duration.between(t.getCreatedAt(), LocalDateTime.now()).toHours();

        TicketResponse.TicketResponseBuilder builder = TicketResponse.builder()
                .id(t.getId())
                .title(t.getTitle())
                .description(t.getDescription())
                .category(t.getCategory())
                .priority(t.getPriority())
                .status(t.getStatus())
                .raisedByName(t.getRaisedBy() != null ? t.getRaisedBy().getName() : null)
                .raisedById(t.getRaisedBy() != null ? t.getRaisedBy().getId() : null)
                .assignedToName(t.getAssignedTo() != null ? t.getAssignedTo().getName() : null)
                .assignedToId(t.getAssignedTo() != null ? t.getAssignedTo().getId() : null)
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .dueAt(t.getDueAt())
                .resolvedAt(t.getResolvedAt())
                .breached(breached)
                .ageHours(ageHours);

        if (includeActivity) {
            List<ActivityResponse> activity = activityRepository.findByTicketIdOrderByCreatedAtAsc(t.getId()).stream()
                    .map(a -> ActivityResponse.builder()
                            .id(a.getId())
                            .type(a.getType())
                            .message(a.getMessage())
                            .actorName(a.getActor() != null ? a.getActor().getName() : "System")
                            .createdAt(a.getCreatedAt())
                            .build())
                    .collect(Collectors.toList());
            builder.activity(activity);
        }

        return builder.build();
    }
}
