package com.edumerge.ticketsystem.config;

import com.edumerge.ticketsystem.entity.*;
import com.edumerge.ticketsystem.repository.TicketActivityRepository;
import com.edumerge.ticketsystem.repository.TicketRepository;
import com.edumerge.ticketsystem.repository.UserRepository;
import com.edumerge.ticketsystem.service.SlaService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;
    private final TicketActivityRepository activityRepository;
    private final SlaService slaService;

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) return; // already seeded (H2 file persists across restarts)

        User priya = userRepository.save(User.builder().name("Priya Shetty").email("priya.student@college.edu").role(Role.STUDENT).department("CSE").build());
        User rahul = userRepository.save(User.builder().name("Rahul Nair").email("rahul.student@college.edu").role(Role.STUDENT).department("ECE").build());
        User staff1 = userRepository.save(User.builder().name("Kavya Rao").email("kavya.staff@college.edu").role(Role.STAFF).department("Accounts").build());
        User staff2 = userRepository.save(User.builder().name("Suresh Kumar").email("suresh.staff@college.edu").role(Role.STAFF).department("Academics").build());
        userRepository.save(User.builder().name("Meena Iyer").email("meena.manager@college.edu").role(Role.MANAGER).department("Administration").build());

        seedTicket(priya, staff1, "Fee receipt not generated after payment", Ticket.Category.FEES, Ticket.Priority.HIGH, Ticket.Status.IN_PROGRESS, 30);
        seedTicket(rahul, staff2, "Attendance shows absent despite being present", Ticket.Category.ATTENDANCE, Ticket.Priority.MEDIUM, Ticket.Status.OPEN, 5);
        seedTicket(priya, null, "Lost ID card, need reissue", Ticket.Category.ID_CARD, Ticket.Priority.LOW, Ticket.Status.OPEN, 1);
        seedTicket(rahul, staff1, "Bonafide certificate request for internship", Ticket.Category.CERTIFICATES, Ticket.Priority.URGENT, Ticket.Status.PENDING_STUDENT, 6);
    }

    private void seedTicket(User raisedBy, User assignedTo, String title, Ticket.Category category,
                             Ticket.Priority priority, Ticket.Status status, long hoursAgo) {
        LocalDateTime created = LocalDateTime.now().minusHours(hoursAgo);
        Ticket ticket = Ticket.builder()
                .title(title)
                .description(title + " \u2014 sample seeded ticket for demo purposes.")
                .category(category)
                .priority(priority)
                .status(status)
                .raisedBy(raisedBy)
                .assignedTo(assignedTo)
                .createdAt(created)
                .updatedAt(created)
                .dueAt(slaService.computeDueAt(created, priority))
                .build();
        ticket = ticketRepository.save(ticket);

        activityRepository.save(TicketActivity.builder()
                .ticket(ticket).actor(raisedBy).type(TicketActivity.ActivityType.CREATED)
                .message("Ticket raised: " + title).createdAt(created).build());

        if (assignedTo != null) {
            activityRepository.save(TicketActivity.builder()
                    .ticket(ticket).actor(assignedTo).type(TicketActivity.ActivityType.ASSIGNMENT)
                    .message("Assigned to " + assignedTo.getName()).createdAt(created.plusMinutes(30)).build());
        }
    }
}
