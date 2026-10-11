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
- **Submit Report** — per-milestone toggle form (progress notes) that POSTs to `/api/v1/milestones/{id}/report`. Returns 403 for roles outside AGENCY/ADMIN, same as the API already enforced — the UI surfaces the error rather than hiding the button, since there's no existing client-side role-check pattern in this codebase to hide it with. (Superseded by §6/§7 below — the UI now hides these controls by role instead of just letting the API reject them.)

All five verified live in the browser against the real local MySQL DB (not just compiled) — see the auth bug below, found *because* of that testing.

### Real bug found and fixed while testing: login never populated `localStorage`

`register.html` set both the `trackph-jwt` cookie and `localStorage.trackph-token`; `login.html` only set the cookie. Every page's `fetch()` calls read the token from `localStorage` (`Authorization: Bearer ...`), which `JwtAuthFilter` checks *before* falling back to the cookie — so logging in as a different user after any earlier session left every API call silently authenticated as the **old** `localStorage` user, regardless of who the cookie (and the server-rendered nav) said was logged in. Caught this because "Submit Report" 403'd even as an ADMIN test account. Fixed in two places:

- `auth/login.html` — now also does `localStorage.setItem('trackph-token', ...)` on success, matching `register.html`.
- `layout.html`'s "Sign out" link now also does `localStorage.removeItem('trackph-token')` — logout previously only cleared the cookie, leaving a valid token sitting in `localStorage` that would keep authenticating `fetch()` calls as the signed-out user until it naturally expired.

(Supersedes the earlier "cookie is dead code" note below — it wasn't dead, it was actively fighting with `localStorage` for which user's token actually got used.)

## 5. Known follow-ups (not blocking, not started)

- **Hardcoded dev DB password fallback** — `application.yml` has `password: ${DB_PASSWORD:1234}`. Harmless for local dev (overridden by the `DB_PASSWORD` env var), but don't let that fallback value follow this service into any real deployment.
- **No delete endpoint for milestones or users** — noticed while testing the new UI (had to leave some test milestones/accounts in place since there's no way to remove them via the API). Add if this ever becomes a real need, not before.
- ~~Milestone/Submit-Report role gating isn't visible client-side~~ — fixed in §7: `PUT /api/v1/milestones/{id}` and `/report` are both now AGENCY/ADMIN-only server-side, and `detail.html` hides the controls for anyone else instead of just letting the API reject them.

## 6. Proof attachments, seed data, and related fixes — DONE (2026-10-11)

- **Proof attachments** — milestones can now have images/documents attached as evidence instead of relying on free-text reports. `V7__create_milestone_attachments.sql` adds `milestone_attachments`; `AttachmentController`/`AttachmentService`/`FileStorageService` handle upload (AGENCY only), public list/download, and uploader-or-ADMIN delete. Files are stored on local disk under `trackph.attachments.dir` (see `application.yml`), not in the database and not under `static/`.
- **Seed data for a fresh machine** — `V8__seed_demo_data.sql` seeds 3 demo users and 7 demo projects covering every `Project.status` value (PLANNED, IN_PROGRESS ×2, DELAYED ×2, COMPLETED, CANCELLED) with 2–4 milestones each, deliberately varied across PENDING/IN_PROGRESS/COMPLETED/OVERDUE. See dev-guide.md's "Database Migrations" section for the general seed-migration convention. Resolves the "no seed data on a fresh clone" gap noted below previously. (The MANAGER demo account was later repurposed — see §7.)
- **Audit Logs page 403'd for AGENCY users** — the page route `/admin/audit-logs` was swept into the generic `/admin/**` → ADMIN-only rule in `SecurityConfig`, even though the nav link and the `/api/v1/audit-logs/**` API were both already correctly scoped to ADMIN+AGENCY. Fixed with a specific `/admin/audit-logs` → `hasAnyRole("ADMIN","AGENCY")` rule ahead of the `/admin/**` catch-all.
- **Navbar now shows who's signed in** — username + role badge next to Sign out, populated client-side from `localStorage` (set at login/register alongside the token). Sessions with a token from before this change show a blank badge until they sign out and back in once.

## 7. Role model cut to three roles + ownership checks + public audit trail — DONE (2026-10-11)

The research paper's DFD only ever named two external entities, Agency and User (§3.2.2: "The User refers to government staff, project managers, or stakeholders..."); `MANAGER` was an implementation-time addition that never appeared in the paper, and after the ownership-check fix below its permission set had shrunk to almost nothing distinct from AGENCY. Rather than keep a role with no clear identity, cut it:

- **`User.Role` enum** — now `ADMIN, AGENCY, PUBLIC`. `V9__remove_manager_role.sql` (new migration — V1/V8 are already applied elsewhere and can't be edited per the dev-guide's Flyway rule) reassigns any existing MANAGER row to PUBLIC and narrows the DB `ENUM` to match. The seeded `pm_manager` account becomes `citizen_demo` (PUBLIC), so there's now a seeded login for the public/citizen experience, which there wasn't before.
- **Project create/edit/delete had no role or ownership check at all** — any authenticated user, any role, could edit or delete *any* project or milestone (`anyRequest().authenticated()` was the only gate). Found while explaining the role model to a reviewer. Fixed: `SecurityConfig` now requires AGENCY/ADMIN for project CRUD and milestone add/edit; `ProjectService`/`MilestoneService` additionally reject an AGENCY user touching a project/milestone they don't own (`UnauthorizedException` → 403), ADMIN bypasses. `/report` still has no ownership check (any AGENCY/ADMIN can report on any project) — smaller, pre-existing gap, left alone for now.
- **Audit logs are now public** (minus IP address) — `GET /api/v1/audit-logs` is `permitAll()`; non-ADMIN/AGENCY callers get `AuditLogResponse.publicView()` (same action/entity/user/timestamp, `ipAddress` nulled out). Rationale: this is a transparency platform, and an audit trail only AGENCY/ADMIN can see doesn't actually constrain AGENCY — nothing stops a corrupt official from deleting evidence with no external witness. `GET /api/v1/audit-logs/user/{id}` and `/report` (the compiled activity-ranking endpoint) stay ADMIN/AGENCY-only — those are account-review tools, not the public accountability trail. No public-facing *page* was built for this yet, just the API; the admin audit-logs UI (`/admin/audit-logs`) is still ADMIN/AGENCY-gated.
- **`PUT /api/v1/projects/{id}` silently nulled out any field the caller didn't send** — `ProjectService.applyRequest()` unconditionally overwrote `description`/`startDate`/`endDate`/`location`/`budget` from the request, so a partial update (e.g. `{name, status}` only, which is all the status-dropdown fix above needed to test) wiped the rest. Caught this live — a verification call against the real seed DB nulled project 1's fields; restored from the known seed values via direct SQL. Fixed by only setting a field when it's non-null in the request, matching the pattern `MilestoneService.applyRequest()` already used for its own optional `description` field. Known limitation this inherits: there's now no way to *intentionally* clear one of these fields back to null through this endpoint — acceptable since the actual edit form always sends every field.

## 8. Soft deletes + before/after diff capture — DONE (2026-10-11)

Prompted by a direct question: does any of the above actually stop a corrupt official from hiding their tracks? No — two real gaps, both closed now:

- **Deletes were hard deletes.** `ProjectService.delete()` called `projectRepository.deleteById()`, and `milestones.project_id`/`milestone_attachments.milestone_id` both have `ON DELETE CASCADE` — so deleting a project silently wiped its milestones *and* their proof photos, unrecoverable outside a DB backup. `V10__add_soft_delete.sql` adds `deleted_at` to `projects`/`milestones`/`milestone_attachments` (not `audit_logs` — that one must never be deletable). `Project`/`Milestone`/`Attachment` get `@SQLRestriction("deleted_at IS NULL")` (Hibernate 6.5, confirmed on the classpath) so every existing query transparently excludes soft-deleted rows with no per-query changes needed. `ProjectService.delete()` now sets `deletedAt` instead of calling `deleteById`, and hand-cascades to the project's milestones and their attachments (the JPA `cascade=ALL` on `Project.milestones` would otherwise still issue a real `DELETE` on save). `AttachmentService.delete()` sets `deletedAt` and **deliberately stops calling `fileStorageService.delete()`** — the file stays on disk; "deleted" means hidden from the public view, not gone. No restore/"view archived" UI was built — soft-deleted rows are invisible through the app entirely right now (even to ADMIN), just no longer erasable. Natural follow-up if that's ever needed.
- **Audit logs recorded that something changed, never what.** `AuditLog.details` existed in the schema but `AuditInterceptor` never set it. Rather than make services write their own `AuditLog` rows (a second writer, contradicting the dev-guide's "interceptor handles it" rule), added `util/AuditContext` — a request-scoped helper a service calls to attach `entityType`/`entityId`/`details` (a before/after JSON snapshot, built by reusing the existing `ProjectResponse`/`MilestoneResponse`/`AttachmentResponse` DTOs) to the request it's handling. `AuditInterceptor` reads those attributes, if present, when it writes the row — still exactly one writer, now capturing real detail when a service provides it. Wired into `ProjectService` (create/update/delete) and `MilestoneService` (create/update/submitReport) and `AttachmentService` (upload/delete). Since this reuses the now-public `GET /api/v1/audit-logs` from §7, the diff/snapshot is visible to the public too, not just ADMIN/AGENCY — e.g. deleting an attachment now leaves a public record of exactly which file, uploaded by whom, that used to be there.

Verified live against the real seeded DB: edited project 1's budget → audit entry shows `budget: 4500000.00 → 4600000` (then restored it back). Uploaded and soft-deleted a test attachment → gone from the list/download (404), file still present on disk (confirmed via its `stored_filename` on the filesystem), audit entry publicly shows the full attachment snapshot. Created a throwaway project+milestone+attachment and deleted the project → project/milestone/attachment all 404 through the API but still exist in the DB with `deleted_at` set, audit entry shows `milestonesArchived`/`attachmentsArchived` counts.

## 9. "View Archived" restore UI — DONE (2026-10-11)

Closes the follow-up flagged at the end of §8: soft-deleted rows were invisible through the app, even to ADMIN, with no way back short of a direct SQL `UPDATE`. ADMIN only, since restoring something someone deleted is itself a privileged action.

- **Listing archived rows bypasses `@SQLRestriction` on purpose.** Hibernate's restriction is unconditional — there's no per-query "ignore it this once" flag, and a native query mapped back to the entity still goes through the same entity loader, so it'd still be filtered. Used plain `JdbcTemplate` queries instead (already on the classpath via `spring-boot-starter-data-jpa`, no new dependency) returning raw `List<Map<String,Object>>` — no entity hydration, so nothing filters it. `ProjectService.findArchived()` and `AttachmentService.findArchivedByMilestone()`.
- **Restoring** is a `@Modifying` native `UPDATE ... SET deleted_at = NULL` in `ProjectRepository`/`MilestoneRepository`/`AttachmentRepository` — an `UPDATE` was never subject to the SELECT-time restriction anyway, so no special-casing needed there. `ProjectService.restore()` cascades the same way `delete()` cascaded: restoring a project also restores its milestones and their attachments, scoped by `project_id`/`milestone_id`, not by "deleted at the same instant" (good enough — there's no path today for a milestone to be archived independently of its project). `AttachmentService.restore()` is the standalone case, for an attachment deleted on its own.
- **New endpoints, ADMIN-only in `SecurityConfig`:** `GET /api/v1/projects/archived`, `POST /api/v1/projects/{id}/restore`, `GET /api/v1/milestones/{id}/attachments/archived`, `POST /api/v1/milestones/{id}/attachments/{id}/restore`. Caught a real ordering bug while testing this: Spring Security's `requestMatchers` list is evaluated top to bottom and the pre-existing `GET /api/v1/projects/{id} → permitAll()` rule treats `{id}` as "any path segment," so it silently matched `/api/v1/projects/archived` too — the new ADMIN-only rule had no effect until moved earlier in the chain, ahead of that wildcard. Verified both ways: before the fix, a non-admin agency token got a 200 reading the archived list; after, it correctly 403s.
- **Front end** — per request, not in the navbar. Projects list page (`/projects`) gets an ADMIN-only "View Archived" toggle showing archived projects with a Restore button. Project detail page gets a "View Archived Attachments" toggle per milestone, same idea. Both call `confirm()`/plain `fetch()`, matching the existing delete-button pattern on the same pages.
- Audit trail logs the restore too (`recordChange` with `before: null`), so "someone brought this back" is itself a visible, public event, not a silent undo.

Verified live: soft-deleted a throwaway project (cascading to its milestone and attachment, per §8) → showed up in `/api/v1/projects/archived` for ADMIN, 403 for AGENCY and anonymous → restored → project, milestone, and attachment all reappeared through the normal (filtered) endpoints, archived list emptied back out. Same round trip for a standalone attachment delete+restore. Confirmed in the actual browser UI too: both toggle buttons render, the archived rows list correctly (including a real pre-existing archived attachment from earlier §8 testing that had never been restored).
