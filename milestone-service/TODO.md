# TODO

## 1. Tech-stack cleanup — DONE (2026-10-03)

Audited: `pom.xml` (Java 21, Spring Boot 3.3.4, MySQL+Flyway, Thymeleaf+security6, jjwt, SpringDoc, H2 dev-only), Eclipse `.classpath`/`.settings` (pinned to JavaSE-21, no stray 17), `application.yml`/`application-dev.yml` (MySQL/Flyway prod vs. H2/create-drop dev), and leftover-template check (no `Sample*` classes, no `ph.trackph.template` refs). Everything already matches the intended stack — nothing to change.

Unrelated minor note, not a stack issue: `application.yml` has a hardcoded dev fallback DB password (`${DB_PASSWORD:1234}`). Fine for local dev, worth revisiting before any real deployment.

## 2. Close gaps vs. the research paper's DFD — DONE (2026-10-03)

Compared `research-paper/FINAL (Compiled) - Milestone & Timeline Monitoring.pdf` (Level 0/1 DFDs, §3.2.2-3.2.3) against the actual code. Two sub-processes were drawn in the paper's diagrams but didn't exist yet — both implemented now:

### 3.3 "Analyze Behavior Patterns" (Figure 3.3, Log User Actions)

Paper shows audit logs feeding a pattern/trend-analysis step that compiles reports for AGENCY. Implemented as action counts per user + per action type over a date range, plus a most-active-users ranking:

- `AuditLogRepository.countByUserAndActionBetween()` — aggregate `@Query`, grouped by user/action, filtered by `createdAt BETWEEN`.
- `AuditLogService.getActivityReport(from, to, topN)` — runs the aggregate query, sums per-user totals, ranks the top-N.
- `AuditActivityReportResponse` DTO — action counts + ranked top users.
- `GET /api/v1/audit-logs/report?from=&to=&topN=` — same ADMIN/AGENCY-only restriction as the rest of `/api/v1/audit-logs/**` (enforced in `SecurityConfig`, no controller change needed).

### 5.2 "Notify Stakeholders" (Figure 3.5, Generate Alerts)

Paper draws active push arrows to both USER and AGENCY — alerts used to just sit in the table for polling. Implemented as Server-Sent Events, no new dependency (`SseEmitter` is already in `spring-web`):

- `AlertService` holds a `CopyOnWriteArrayList<SseEmitter>`; `createAlert()` broadcasts the new `AlertResponse` to all of them after save.
- `GET /api/v1/alerts/stream` (`AlertController`) registers and returns the emitter.
- `EventSource` can't set an `Authorization` header, so `JwtAuthFilter` accepts the JWT as a `?token=` query param, but only for this one stream path — every other endpoint still requires the header or the (currently-unused) cookie.
- Frontend: `layout.html`'s navbar badge and `alerts/list.html`'s table both open an `EventSource` against the stream and refresh themselves on each pushed alert, alongside the existing poll/initial-load fetches.
- No per-user targeting exists on `Alert` — broadcasts go to every connected client, matching the diagram's blanket push to USER and AGENCY.

Minor, lower priority, not planned yet: 4.3 "Flag Delays or Changes" (Figure 3.4) is its own diagrammed sub-process feeding AGENCY directly, but in code that logic is folded into `AlertScheduler.checkOverdueMilestones()` under process 5.0 instead of kept distinct. Functionally present, just merged differently than the diagram shows.

Everything else (1.0, 2.0, 3.1/3.2, 4.1/4.2, 5.1/5.3) matches the diagrams and prose closely — see `AuthController`/`ProjectController`, `DashboardController`, `AuditInterceptor`, `MilestoneController`'s `/report` endpoint, `AlertScheduler`.

## 4. Missing UI for existing backend CRUD — DONE (2026-10-03)

Backend already supported editing/deleting projects and managing milestones (`ProjectController` PUT/DELETE, `MilestoneController` create/update/report) — the UI just never exposed any of it, and the paper's §3.2 objective ("update milestone status in real time") and the 4.0 Submit Project Report DFD both call for it. Added to `projects/detail.html`:

- **Edit / Delete project** — buttons next to the status badge. Edit toggles an inline form (prefilled from the already-loaded project) that PUTs on save; Delete confirms, then DELETEs and redirects to `/projects`.
- **Add Milestone** — toggleable inline form above the timeline, POSTs to `/api/v1/projects/{id}/milestones`.
- **Update milestone status** — a `<select>` per milestone card that PUTs immediately on change (no separate save step); picking `COMPLETED` auto-sets `completedDate` to today.
- **Submit Report** — per-milestone toggle form (progress notes) that POSTs to `/api/v1/milestones/{id}/report`. Returns 403 for roles outside MANAGER/AGENCY/ADMIN, same as the API already enforced — the UI surfaces the error rather than hiding the button, since there's no existing client-side role-check pattern in this codebase to hide it with.

All five verified live in the browser against the real local MySQL DB (not just compiled) — see the auth bug below, found *because* of that testing.

### Real bug found and fixed while testing: login never populated `localStorage`

`register.html` set both the `trackph-jwt` cookie and `localStorage.trackph-token`; `login.html` only set the cookie. Every page's `fetch()` calls read the token from `localStorage` (`Authorization: Bearer ...`), which `JwtAuthFilter` checks *before* falling back to the cookie — so logging in as a different user after any earlier session left every API call silently authenticated as the **old** `localStorage` user, regardless of who the cookie (and the server-rendered nav) said was logged in. Caught this because "Submit Report" 403'd even as an ADMIN test account. Fixed in two places:

- `auth/login.html` — now also does `localStorage.setItem('trackph-token', ...)` on success, matching `register.html`.
- `layout.html`'s "Sign out" link now also does `localStorage.removeItem('trackph-token')` — logout previously only cleared the cookie, leaving a valid token sitting in `localStorage` that would keep authenticating `fetch()` calls as the signed-out user until it naturally expired.

(Supersedes the earlier "cookie is dead code" note below — it wasn't dead, it was actively fighting with `localStorage` for which user's token actually got used.)

## 5. Known follow-ups (not blocking, not started)

- **Hardcoded dev DB password fallback** — `application.yml` has `password: ${DB_PASSWORD:1234}`. Harmless for local dev (overridden by the `DB_PASSWORD` env var), but don't let that fallback value follow this service into any real deployment.
- **No delete endpoint for milestones or users** — noticed while testing the new UI (had to leave some test milestones/accounts in place since there's no way to remove them via the API). Add if this ever becomes a real need, not before.
- **Milestone/Submit-Report role gating isn't visible client-side** — `PUT /api/v1/milestones/{id}` has no role restriction (any authenticated role, including PUBLIC, can change status), while `/report` is MANAGER/AGENCY/ADMIN-only. The new UI shows both controls to everyone and lets the API 403 if not permitted, rather than hiding buttons by role — there's no existing pattern in this codebase for reading the JWT role client-side to do that. Fine for now; revisit if PUBLIC-role milestone editing turns out to be unintended.
- **No seed data on a fresh clone** — migrations V1 through V6 only build the schema. A checkout against real MySQL has zero rows until someone registers through the UI by hand. Worth a seed migration if that gets in the way often enough.
