const API = "/api";

const CATEGORIES = ["FEES", "ATTENDANCE", "ID_CARD", "DOCUMENTS", "CERTIFICATES", "OTHER"];
const PRIORITIES = ["LOW", "MEDIUM", "HIGH", "URGENT"];
const STATUSES = ["OPEN", "IN_PROGRESS", "PENDING_STUDENT", "RESOLVED", "CLOSED"];

let users = [];
let currentUser = null;

// ---------- init ----------

document.addEventListener("DOMContentLoaded", async () => {
  populateSelect("fStatus", STATUSES, true);
  populateSelect("fPriority", PRIORITIES, true);
  populateSelect("fCategory", CATEGORIES, true);
  populateSelect("ntCategory", CATEGORIES, false);
  populateSelect("ntPriority", PRIORITIES, false);

  await loadUsers();
  setupTabs();
  setupNewTicketForm();
  document.getElementById("refreshBtn").addEventListener("click", loadTickets);
  document.getElementById("fBreached").addEventListener("change", loadTickets);
  document.getElementById("fMine").addEventListener("change", loadTickets);
  document.getElementById("fStatus").addEventListener("change", loadTickets);
  document.getElementById("fPriority").addEventListener("change", loadTickets);
  document.getElementById("fCategory").addEventListener("change", loadTickets);
  document.getElementById("closeModal").addEventListener("click", closeModal);
  document.getElementById("detailModal").addEventListener("click", (e) => {
    if (e.target.id === "detailModal") closeModal();
  });

  await loadTickets();
  await loadDashboard();
});

function populateSelect(id, values, withAll) {
  const sel = document.getElementById(id);
  values.forEach((v) => {
    const opt = document.createElement("option");
    opt.value = v;
    opt.textContent = prettify(v);
    sel.appendChild(opt);
  });
}

function prettify(v) {
  return v.replaceAll("_", " ").toLowerCase().replace(/\b\w/g, (c) => c.toUpperCase());
}

// ---------- users / "acting as" ----------

async function loadUsers() {
  const res = await fetch(`${API}/users`);
  users = await res.json();
  const sel = document.getElementById("userSelect");
  sel.innerHTML = "";
  users.forEach((u) => {
    const opt = document.createElement("option");
    opt.value = u.id;
    opt.textContent = `${u.name} (${u.role})`;
    sel.appendChild(opt);
  });
  currentUser = users[0];
  sel.addEventListener("change", () => {
    currentUser = users.find((u) => u.id == sel.value);
    loadTickets();
  });
}

function staffAndManagers() {
  return users.filter((u) => u.role === "STAFF" || u.role === "MANAGER");
}

// ---------- tabs ----------

function setupTabs() {
  document.querySelectorAll(".tab-btn").forEach((btn) => {
    btn.addEventListener("click", () => {
      document.querySelectorAll(".tab-btn").forEach((b) => b.classList.remove("active"));
      document.querySelectorAll(".tab-panel").forEach((p) => p.classList.remove("active"));
      btn.classList.add("active");
      document.getElementById(`tab-${btn.dataset.tab}`).classList.add("active");
      if (btn.dataset.tab === "dashboard") loadDashboard();
    });
  });
}

// ---------- ticket list ----------

async function loadTickets() {
  const params = new URLSearchParams();
  const status = document.getElementById("fStatus").value;
  const priority = document.getElementById("fPriority").value;
  const category = document.getElementById("fCategory").value;
  const breachedOnly = document.getElementById("fBreached").checked;
  const mineOnly = document.getElementById("fMine").checked;

  if (status) params.set("status", status);
  if (priority) params.set("priority", priority);
  if (category) params.set("category", category);
  if (breachedOnly) params.set("breachedOnly", "true");

  if (mineOnly && currentUser) {
    if (currentUser.role === "STUDENT") params.set("raisedById", currentUser.id);
    else params.set("assignedToId", currentUser.id);
  }

  const res = await fetch(`${API}/tickets?${params.toString()}`);
  const tickets = await res.json();
  renderTicketList(tickets);
}

function renderTicketList(tickets) {
  const container = document.getElementById("ticketList");
  container.innerHTML = "";
  if (tickets.length === 0) {
    container.innerHTML = `<p style="color:var(--muted)">No tickets match these filters.</p>`;
    return;
  }
  tickets.forEach((t) => {
    const div = document.createElement("div");
    div.className = "ticket-card" + (t.breached ? " breached" : "");
    div.innerHTML = `
      <div class="ticket-main">
        <div class="ticket-title">#${t.id} ${escapeHtml(t.title)}</div>
        <div class="ticket-meta">
          <span class="badge ${t.priority}">${t.priority}</span>
          <span class="status-pill">${prettify(t.status)}</span>
          <span>${prettify(t.category)}</span>
          <span>Raised by ${escapeHtml(t.raisedByName || "—")}</span>
          <span>Assigned: ${escapeHtml(t.assignedToName || "Unassigned")}</span>
          <span>${t.ageHours}h old</span>
          ${t.breached ? '<span class="breach-tag">SLA BREACHED</span>' : ""}
        </div>
      </div>
    `;
    div.addEventListener("click", () => openTicketDetail(t.id));
    container.appendChild(div);
  });
}

// ---------- create ticket ----------

function setupNewTicketForm() {
  document.getElementById("newTicketForm").addEventListener("submit", async (e) => {
    e.preventDefault();
    const msgEl = document.getElementById("newTicketMsg");
    msgEl.textContent = "";

    const body = {
      title: document.getElementById("ntTitle").value.trim(),
      description: document.getElementById("ntDescription").value.trim(),
      category: document.getElementById("ntCategory").value,
      priority: document.getElementById("ntPriority").value,
      raisedById: currentUser.id,
    };

    const res = await fetch(`${API}/tickets`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
    });

    if (res.ok) {
      const ticket = await res.json();
      msgEl.style.color = "green";
      msgEl.textContent = `Ticket #${ticket.id} raised successfully.`;
      document.getElementById("newTicketForm").reset();
      loadTickets();
      loadDashboard();
    } else {
      const err = await res.json();
      msgEl.style.color = "crimson";
      msgEl.textContent = "Error: " + (err.title || err.error || JSON.stringify(err));
    }
  });
}

// ---------- ticket detail modal ----------

async function openTicketDetail(id) {
  const res = await fetch(`${API}/tickets/${id}`);
  const t = await res.json();
  renderDetail(t);
  document.getElementById("detailModal").classList.add("open");
}

function closeModal() {
  document.getElementById("detailModal").classList.remove("open");
}

function renderDetail(t) {
  const el = document.getElementById("detailContent");

  const staffOptions = staffAndManagers()
    .map((u) => `<option value="${u.id}" ${t.assignedToId == u.id ? "selected" : ""}>${escapeHtml(u.name)}</option>`)
    .join("");

  const statusOptions = STATUSES
    .map((s) => `<option value="${s}" ${t.status === s ? "selected" : ""}>${prettify(s)}</option>`)
    .join("");

  const activityHtml = (t.activity || [])
    .map(
      (a) => `
      <div class="activity-item">
        <div>${escapeHtml(a.message || "")}</div>
        <div class="a-meta">${prettify(a.type)} · ${escapeHtml(a.actorName || "System")} · ${formatDate(a.createdAt)}</div>
      </div>`
    )
    .join("");

  el.innerHTML = `
    <h2>#${t.id} ${escapeHtml(t.title)}</h2>
    <p>${escapeHtml(t.description || "")}</p>
    <div class="ticket-meta">
      <span class="badge ${t.priority}">${t.priority}</span>
      <span class="status-pill">${prettify(t.status)}</span>
      <span>${prettify(t.category)}</span>
      ${t.breached ? '<span class="breach-tag">SLA BREACHED</span>' : ""}
    </div>
    <p style="font-size:13px;color:var(--muted)">
      Raised by ${escapeHtml(t.raisedByName || "—")} · Assigned to ${escapeHtml(t.assignedToName || "Unassigned")}<br/>
      Created ${formatDate(t.createdAt)} · Due ${formatDate(t.dueAt)}
    </p>

    <div class="detail-actions">
      <select id="assignSelect"><option value="">Assign to…</option>${staffOptions}</select>
      <button id="assignBtn">Assign</button>
    </div>
    <div class="detail-actions">
      <select id="statusSelect">${statusOptions}</select>
      <button id="statusBtn">Update Status</button>
    </div>

    <h3>Activity History</h3>
    <div id="activityList">${activityHtml || "<p style='color:var(--muted)'>No activity yet.</p>"}</div>

    <div class="comment-box">
      <input type="text" id="commentInput" placeholder="Add a comment…" />
      <button id="commentBtn">Post</button>
    </div>
  `;

  document.getElementById("assignBtn").addEventListener("click", async () => {
    const assignedToId = document.getElementById("assignSelect").value;
    if (!assignedToId) return;
    await fetch(`${API}/tickets/${t.id}/assign`, {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ assignedToId, actorId: currentUser.id }),
    });
    openTicketDetail(t.id);
    loadTickets();
    loadDashboard();
  });

  document.getElementById("statusBtn").addEventListener("click", async () => {
    const status = document.getElementById("statusSelect").value;
    const res = await fetch(`${API}/tickets/${t.id}/status`, {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ status, actorId: currentUser.id }),
    });
    if (res.ok) {
      openTicketDetail(t.id);
      loadTickets();
      loadDashboard();
    } else {
      const err = await res.json();
      alert(err.error || "Could not update status");
    }
  });

  document.getElementById("commentBtn").addEventListener("click", async () => {
    const message = document.getElementById("commentInput").value.trim();
    if (!message) return;
    await fetch(`${API}/tickets/${t.id}/comments`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ actorId: currentUser.id, message }),
    });
    openTicketDetail(t.id);
    loadTickets();
  });
}

// ---------- dashboard ----------

async function loadDashboard() {
  const res = await fetch(`${API}/dashboard/stats`);
  const stats = await res.json();

  const cards = document.getElementById("dashboardCards");
  cards.innerHTML = `
    ${dashCard(stats.totalTickets, "Total Tickets")}
    ${dashCard(stats.breachedCount, "SLA Breached", stats.breachedCount > 0)}
    ${dashCard(stats.openOlderThan48h, "Open > 48h", stats.openOlderThan48h > 0)}
    ${dashCard(stats.avgResolutionHours + "h", "Avg Resolution Time")}
  `;

  renderMiniTable("byStatusTable", stats.byStatus, STATUSES);
  renderMiniTable("byPriorityTable", stats.byPriority, PRIORITIES);
  renderMiniTable("byCategoryTable", stats.byCategory, CATEGORIES);
}

function dashCard(num, label, alert) {
  return `<div class="dash-card ${alert ? "alert" : ""}"><div class="num">${num}</div><div class="label">${label}</div></div>`;
}

function renderMiniTable(id, dataMap, order) {
  const el = document.getElementById(id);
  el.innerHTML = order
    .map((k) => `<div class="mini-row"><span>${prettify(k)}</span><span>${dataMap[k] || 0}</span></div>`)
    .join("");
}

// ---------- utils ----------

function escapeHtml(str) {
  const div = document.createElement("div");
  div.textContent = str ?? "";
  return div.innerHTML;
}

function formatDate(iso) {
  if (!iso) return "—";
  const d = new Date(iso);
  return d.toLocaleString();
}
