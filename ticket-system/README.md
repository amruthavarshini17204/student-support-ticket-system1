# Student Support & Ticket Management System

Edumerge Solutions — Pre-Drive Product Engineering Assignment (Assignment 4)

A working prototype for the problem: students raise requests (fees, attendance, ID
cards, documents, certificates, other), and staff own, prioritize, process, and
resolve them, with SLA/ageing visibility for managers.

## Tech Stack

- **Backend:** Java 17, Spring Boot 3.2 (Spring Web, Spring Data JPA, Bean Validation)
- **Database:** H2 (file-based, zero setup) by default; MySQL config included and
  commented out in `application.properties` for production use
- **Frontend:** Plain HTML/CSS/JavaScript (no framework), served as static resources
  by the same Spring Boot app — one process, no CORS to configure
- **Build:** Maven

This stack was chosen to match a Java Full Stack profile (Spring Boot, Servlet/JDBC,
REST API, MySQL) while keeping the submission trivial to run for evaluation.

## How to Run

```bash
mvn spring-boot:run
```

Then open **http://localhost:8080**. The app seeds 5 demo users (2 students, 2 staff,
1 manager) and 4 sample tickets on first run so the UI isn't empty. Data persists in
`./data/ticketdb` between restarts (delete that folder to reseed from scratch).

To use MySQL instead of H2: create a database, then in
`src/main/resources/application.properties` comment out the H2 block and uncomment
the MySQL block (update credentials). `spring.jpa.hibernate.ddl-auto=update` will
create the tables automatically on first run.

Run tests: `mvn test` (covers SLA computation, breach detection, and auto-escalation).

## Users, Roles & Workflow

There's no login system (see Assumptions) — a dropdown in the header ("Acting as")
lets you switch between seeded users to simulate different roles:

- **Student** — raises tickets, sees their own tickets, comments on them
- **Staff** — sees tickets assigned to them (or all, via filters), gets assigned
  tickets, updates status, comments
- **Manager** — same as staff, plus the dashboard gives ageing/SLA visibility across
  all tickets

**Ticket lifecycle:** `OPEN → IN_PROGRESS → (PENDING_STUDENT ⇄ IN_PROGRESS) →
RESOLVED → CLOSED`. `RESOLVED` can be reopened back to `IN_PROGRESS`; `CLOSED` is
terminal. Invalid transitions (e.g. `CLOSED → OPEN`) are rejected by the API with a
400 error — this is enforced server-side, not just hidden in the UI, since the API
is the actual contract.

## Data Model

- **User** — id, name, email, role (STUDENT/STAFF/MANAGER), department
- **Ticket** — title, description, category, priority, status, raisedBy, assignedTo,
  createdAt, updatedAt, dueAt, resolvedAt, closedAt
- **TicketActivity** — an append-only audit log per ticket (created, comment,
  status change, assignment, escalation) with actor and timestamp — this is what
  powers the "activity history" requirement and gives full traceability of who did
  what and when, rather than just the current state.

## SLA, Ageing & Escalation (the "product thinking" part)

Rather than just storing a status, the system computes:

- **`dueAt`** at creation time, from priority: Urgent = 4h, High = 24h,
  Medium = 72h, Low = 120h (see `SlaService`)
- **`breached`** — true if an open ticket has passed its due date, or if it was
  resolved after its due date (so breach history survives resolution — useful for
  reporting on how often SLAs are actually met)
- **`ageHours`** — how long the ticket has been open, shown on every card
- **Auto-escalation** — once a ticket passes 80% of its SLA window unresolved, its
  priority bumps up one level (Low→Medium→High→Urgent), so at-risk tickets surface
  to staff *before* they breach rather than only after. This is currently computed
  on read (`escalateIfNeeded` in `SlaService`); a production version would run it as
  a scheduled job that also writes an `ESCALATED` activity entry.
- The dashboard aggregates by status/priority/category, total breached count, tickets
  open >48h, and average resolution time.

Ticket lists sort breached tickets first, then by priority, so staff naturally see
what's on fire first instead of a plain chronological feed.

## Assumptions

- **No authentication.** Real auth (login, JWT/session, password reset) was
  out of scope for a time-boxed prototype; the "Acting as" selector simulates
  identity so role-based behavior is still demonstrable. Flagged here explicitly
  rather than silently skipped.
- **Fixed SLA policy per priority**, not configurable per category or department.
  A real system would likely let managers configure SLA windows.
- **Single-institution, single-tenant.** No multi-college/multi-tenant partitioning.
- **Escalation is computed on read**, not via a background scheduler — acceptable
  for a prototype, but a real deployment should run it as a Spring `@Scheduled` job
  so escalation activity is logged even if nobody happens to view the ticket.
- **No email/SMS notifications** on assignment, status change, or breach — noted as
  a natural next step, not built here.
- **Category and priority are fixed enums** rather than admin-configurable lookup
  tables, to keep the data model simple for the prototype.

## Edge Cases Handled

- Invalid status transitions are rejected (enforced in `TicketService.isValidTransition`)
- A ticket resolved *after* its due date is still flagged as breached (not just
  "currently overdue") — breach is a historical fact, not just a live state
- A student commenting on a `PENDING_STUDENT` ticket automatically moves it back to
  `IN_PROGRESS`, so tickets don't silently stall waiting on staff to notice a reply
- Filtering supports combinations (status + priority + category + assignee +
  breached-only) via JPA Specifications rather than one query per filter combo
- Validation errors (missing title/category) return field-level 400 errors instead
  of a generic 500

## Known Limitations / What I'd Add With More Time

- Authentication & authorization (route-level, not just UI-level role simulation)
- Scheduled escalation job + notifications
- Pagination on the ticket list (fine for a demo dataset, not for 5,000 students)
- Configurable SLA policy via an admin screen
- File attachments on tickets (e.g. a photo of a fee receipt)

## AI Usage

See `AI_USAGE_REPORT.md`. Fill in the "AI output that was wrong" / "How I identified
it" / "How I fixed it" sections from your own run — I built this in a sandbox
without live access to Maven Central, so run `mvn spring-boot:run` yourself, poke at
the UI, and note anything you had to correct.
