# Proof-Driven Status, Phase 1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Milestone and project status are derived by one piece of code from attachments, due dates and milestones. The two status dropdowns are gone, and a status resting on an unreviewed proof shows a leading asterisk.

**Architecture:** `StatusDeriver` is a pure static class holding every rule. `StatusService` loads the facts, asks `StatusDeriver`, and is the only writer of `status` on both tables. Every event that can change a status (attach, delete, restore, milestone create or edit, midnight job, startup) calls `StatusService`. The asterisk is derived in the response DTOs, never stored.

**Tech Stack:** Spring Boot 3.3.4, Spring Data JPA, Flyway (MySQL), H2 for the `dev` profile, JUnit 5, Thymeleaf pages with plain JS.

**Spec:** `docs/superpowers/specs/2026-11-06-proof-driven-status-design.md` (phase 1 only). Read it first.

All paths below are relative to `D:\Programming Projects\TrackPH` unless they start with `milestone-service/`. Run Maven from `milestone-service/`. There is no `mvnw`, use `mvn`.

## Global Constraints

- Flyway migrations only, never edit an applied one. Next free number is V12 (this plan uses V12 and V13).
- Display format is `*LABEL`, with the asterisk in front. Legend text is exactly: `* counted automatically, awaiting human check`.
- Status and the asterisk must be visible to PUBLIC users, so both go in `publicView` responses too.
- Stand-in rule for phase 1 only: every milestone requires one proof and any attachment counts. All proofs are UNREVIEWED.
- Keep the STATUS_CHANGE alert, now fired whenever the derived status actually changes (except into OVERDUE, where the scheduler's DEADLINE_MISSED alert already covers it).
- `CANCELLED` is the only manual status. It needs a written reason.
- Update `trackph-microservice-template/docs/dev-guide.md` and the service `CLAUDE.md` in the same change.
- Never commit on your own. Every "Checkpoint" step means: ask the user for a go-ahead, then commit with a plain, human-sounding message and no Claude or Anthropic attribution. Never push without being asked.
- Do not start a dev server on port 8081 without checking `netstat` and asking. A preview server may already be running (see Task 7).

## Review Focus

Failure modes the spec implies. Each has a test in the task named in brackets.

1. A milestone due today with no proof is PENDING, not OVERDUE. [Task 1]
2. A project with zero milestones is PLANNED, never COMPLETED (watch the empty "all completed" trap). [Task 1]
3. A CANCELLED project stays CANCELLED through every refresh, even when all milestones complete. [Tasks 1, 3]
4. Deleting the only proof of a COMPLETED milestone drops it back and clears `completedDate`. [Task 3]
5. An old client that still sends `status` or `completedDate` in a PUT body is ignored, not rejected. [Task 4]
6. The startup backfill is idempotent and creates no alerts. [Task 3]

---

### Task 1: `StatusDeriver` (pure rules)

**Files:**
- Create: `milestone-service/src/main/java/ph/trackph/milestone/service/StatusDeriver.java`
- Test: `milestone-service/src/test/java/ph/trackph/milestone/service/StatusDeriverTest.java`

**Interfaces:**
- Produces (all `public static`, class is `final` with a private constructor):
  - `Milestone.Status deriveMilestone(LocalDate dueDate, LocalDate today, int countingProofs, int requiredProofs)`
  - `boolean milestoneNeedsReview(Milestone.Status status, int unreviewedProofs)`
  - `Project.Status deriveProject(Project.Status current, List<Milestone.Status> milestoneStatuses)`
  - `boolean projectNeedsReview(Project.Status status, boolean anyMilestoneNeedsReview)`
  - `int DEFAULT_REQUIRED_PROOFS = 1` (the phase 1 stand-in)

- [ ] **Step 1: Write the failing test.** Constant `TODAY = LocalDate.of(2026, 11, 6)`. Static-import `Milestone.Status.*` as needed. Tests and assertions:

```java
@Test void enoughProofsIsCompleted()        { assertEquals(COMPLETED, deriveMilestone(TODAY.plusDays(5), TODAY, 1, 1)); }
@Test void completedWinsEvenWhenPastDue()   { assertEquals(COMPLETED, deriveMilestone(TODAY.minusDays(30), TODAY, 1, 1)); }
@Test void pastDueWithoutProofsIsOverdue()  { assertEquals(OVERDUE, deriveMilestone(TODAY.minusDays(1), TODAY, 0, 1)); }
@Test void dueTodayIsNotYetOverdue()        { assertEquals(PENDING, deriveMilestone(TODAY, TODAY, 0, 1)); }          // Review Focus 1
@Test void someButNotAllProofsIsInProgress(){ assertEquals(IN_PROGRESS, deriveMilestone(TODAY.plusDays(5), TODAY, 1, 3)); }
@Test void partialProofsPastDueIsOverdue()  { assertEquals(OVERDUE, deriveMilestone(TODAY.minusDays(1), TODAY, 1, 3)); }
@Test void noProofsNotDueIsPending()        { assertEquals(PENDING, deriveMilestone(TODAY.plusDays(5), TODAY, 0, 1)); }
@Test void zeroRequiredStillNeedsOneProof() { assertEquals(PENDING, deriveMilestone(TODAY.plusDays(5), TODAY, 0, 0)); }
@Test void milestoneAsteriskRules() {
    assertTrue(milestoneNeedsReview(COMPLETED, 1));  assertTrue(milestoneNeedsReview(IN_PROGRESS, 2));
    assertFalse(milestoneNeedsReview(COMPLETED, 0)); assertFalse(milestoneNeedsReview(OVERDUE, 3));
    assertFalse(milestoneNeedsReview(PENDING, 1));
}
@Test void noMilestonesIsPlanned()          { assertEquals(Project.Status.PLANNED, deriveProject(Project.Status.COMPLETED, List.of())); }   // Review Focus 2
@Test void allCompletedIsCompleted()        { assertEquals(Project.Status.COMPLETED, deriveProject(Project.Status.PLANNED, List.of(COMPLETED, COMPLETED))); }
@Test void anyOverdueIsDelayed()            { assertEquals(Project.Status.DELAYED, deriveProject(Project.Status.PLANNED, List.of(COMPLETED, OVERDUE, PENDING))); }
@Test void startedIsInProgress() {
    assertEquals(Project.Status.IN_PROGRESS, deriveProject(Project.Status.PLANNED, List.of(COMPLETED, PENDING)));
    assertEquals(Project.Status.IN_PROGRESS, deriveProject(Project.Status.PLANNED, List.of(IN_PROGRESS, PENDING)));
}
@Test void nothingStartedIsPlanned()        { assertEquals(Project.Status.PLANNED, deriveProject(Project.Status.IN_PROGRESS, List.of(PENDING, PENDING))); }
@Test void cancelledSticks()                { assertEquals(Project.Status.CANCELLED, deriveProject(Project.Status.CANCELLED, List.of(COMPLETED, COMPLETED))); }  // Review Focus 3
@Test void projectAsteriskRules() {
    assertTrue(projectNeedsReview(Project.Status.COMPLETED, true));  assertTrue(projectNeedsReview(Project.Status.IN_PROGRESS, true));
    assertFalse(projectNeedsReview(Project.Status.DELAYED, true));   assertFalse(projectNeedsReview(Project.Status.CANCELLED, true));
    assertFalse(projectNeedsReview(Project.Status.COMPLETED, false));
}
```

- [ ] **Step 2: Run it, confirm it fails.** From `milestone-service/`: `mvn test -Dtest=StatusDeriverTest`. Expected: compilation error, `StatusDeriver` does not exist.
- [ ] **Step 3: Implement `StatusDeriver`** with the signatures above. Rules, first match wins.
  - Milestone: `required = Math.max(1, requiredProofs)`; `countingProofs >= required` is COMPLETED; else `dueDate.isBefore(today)` is OVERDUE; else `countingProofs > 0` is IN_PROGRESS; else PENDING.
  - Milestone asterisk: status is COMPLETED or IN_PROGRESS and `unreviewedProofs > 0`.
  - Project: `current == CANCELLED` gives CANCELLED; empty list gives PLANNED; all COMPLETED gives COMPLETED; any OVERDUE gives DELAYED; any IN_PROGRESS or COMPLETED gives IN_PROGRESS; else PLANNED.
  - Project asterisk: status is COMPLETED or IN_PROGRESS and `anyMilestoneNeedsReview`.
- [ ] **Step 4: Run the test, confirm it passes.** Same command. Expected: `Tests run: 16, Failures: 0, Errors: 0`, BUILD SUCCESS.
- [ ] **Step 5: Checkpoint** (ask first). Files: both above. Message idea: "Add StatusDeriver, the one place status rules live".

---

### Task 2: Persistence groundwork

**Files:**
- Create: `milestone-service/src/main/resources/db/migration/V12__add_project_cancel_reason.sql`
- Modify: `milestone-service/src/main/java/ph/trackph/milestone/model/Project.java`, `model/Milestone.java`, `repository/AttachmentRepository.java`

**Interfaces:**
- Produces: `Project.getCancelReason(): String` / `setCancelReason(String)`; `Milestone.getAttachments(): List<Attachment>`; `AttachmentRepository.countByMilestoneId(Long): long`.

- [ ] **Step 1:** Create `V12` with one statement: `ALTER TABLE projects ADD COLUMN cancel_reason TEXT NULL;`. The dev profile builds its schema from entities, so only MySQL needs this.
- [ ] **Step 2:** In `Project.java` add `@Column(name = "cancel_reason", columnDefinition = "TEXT") private String cancelReason;` with getter and setter, matching the file's plain getter/setter style.
- [ ] **Step 3:** In `Milestone.java` add `@OneToMany(mappedBy = "milestone") private List<Attachment> attachments = new ArrayList<>();` and `getAttachments()`. Attachment's `@SQLRestriction` already hides soft-deleted rows from this collection. Add the needed imports.
- [ ] **Step 4:** In `AttachmentRepository` add `long countByMilestoneId(Long milestoneId);`. This is a query, so it flushes pending writes first and respects `@SQLRestriction`. `StatusService` must count through this method, not through the collection, because the collection can be stale inside one transaction.
- [ ] **Step 5: Verify the mapping.** `mvn test -Dtest=MilestoneApplicationTests`. Expected: passes (context loads on H2 with the new fields).
- [ ] **Step 6: Checkpoint** (ask first). Message idea: "Add cancel_reason column and the relations status derivation needs".

---

### Task 3: `StatusService`

**Files:**
- Create: `milestone-service/src/main/java/ph/trackph/milestone/service/StatusService.java`
- Test: `milestone-service/src/test/java/ph/trackph/milestone/service/StatusServiceTest.java`

**Interfaces:**
- Consumes: Task 1 `StatusDeriver`, Task 2 `countByMilestoneId`, existing `AlertService.createAlert(Project, Milestone, Alert.AlertType, String, Alert.Severity)`.
- Produces (all `@Transactional`):
  - `void refreshMilestone(Long milestoneId)`: recompute that milestone, then its project.
  - `void refreshProject(Long projectId)`
  - `void refreshAllQuietly()`: every project and milestone, no alerts.

- [ ] **Step 1: Write the failing tests.** `@SpringBootTest(properties = "spring.profiles.active=dev")` and `@Transactional`. Inject the repositories, `StatusService` and an `EntityManager`. Add helpers that save a `User` (role AGENCY, unique username and email, any password hash), a `Project`, a `Milestone` (due date in `LocalDate.now()` plus N days) and an `Attachment` (stored filename unique per call). Tests:

```java
attachingProofCompletesMilestoneAndStartsProject:
    // m1 due +30, m2 due +60; save one attachment on m1; statusService.refreshMilestone(m1.getId())
    // assert m1 COMPLETED, m1.getCompletedDate() equals LocalDate.now(), project IN_PROGRESS
    // add an attachment to m2, refreshMilestone(m2) -> project COMPLETED
pastDueMilestoneWithoutProofIsOverdueAndProjectDelayed:
    // m due -3, no attachment; refreshMilestone -> OVERDUE (getDelayDays() == 3), project DELAYED
softDeletingTheOnlyProofDropsMilestoneBack:            // Review Focus 4
    // complete a milestone as above, then attachment.setDeletedAt(LocalDateTime.now()); save; refreshMilestone
    // assert status PENDING and getCompletedDate() == null
cancelledProjectStaysCancelled:                        // Review Focus 3
    // project.setStatus(CANCELLED); complete all milestones; refreshProject -> still CANCELLED
refreshAllQuietlyIsIdempotentAndSilent:                // Review Focus 6
    // create a completed and an overdue milestone, record alertRepository.count(), run refreshAllQuietly() twice
    // statuses are the same after both runs and alertRepository.count() is unchanged
statusChangeFiresAlertExceptIntoOverdue:
    // completing a milestone adds one Alert of type STATUS_CHANGE; an overdue one adds none from refreshMilestone
```

After saving fixtures call `entityManager.flush()`, and `entityManager.clear()` before re-reading entities so assertions see database state.

- [ ] **Step 2: Run, confirm failure.** `mvn test -Dtest=StatusServiceTest`. Expected: compilation error, `StatusService` missing.
- [ ] **Step 3: Implement `StatusService`** (constructor injection, matching the codebase). `refreshMilestone`: load the milestone (`ResourceNotFoundException` if missing); `counting = attachmentRepository.countByMilestoneId(id)`; derive with `StatusDeriver.DEFAULT_REQUIRED_PROOFS` and `LocalDate.now()`. If the status changed: set it; set `completedDate` to the existing value or today when new status is COMPLETED, else null; fire the STATUS_CHANGE alert (LOW severity, message `Milestone '<title>' changed to <STATUS>`) unless the new status is OVERDUE. Save, then call `refreshProject`. `refreshProject`: load project, read `milestoneRepository.findByProjectIdOrderBySequenceOrderAsc(id)` for the statuses, derive, set if changed, save. `refreshAllQuietly`: iterate `projectRepository.findAll()` and each project's milestones with the same derive logic and no alert path (share a private method with an `alert` flag).
- [ ] **Step 4: Run, confirm pass.** Same command. Expected: all `StatusServiceTest` tests pass.
- [ ] **Step 5: Checkpoint** (ask first). Message idea: "Add StatusService, the only writer of milestone and project status".

---

### Task 4: Wire it in, remove manual status

**Files (all under `milestone-service/src/main/java/ph/trackph/milestone/`):**
- Modify: `dto/request/MilestoneRequest.java`, `dto/request/ProjectRequest.java`, `dto/response/MilestoneResponse.java`, `dto/response/ProjectResponse.java`
- Modify: `service/MilestoneService.java`, `service/AttachmentService.java`, `service/ProjectService.java`, `scheduler/AlertScheduler.java`, `controller/MilestoneController.java`
- Create: `scheduler/StatusBackfillRunner.java`
- Test: add to `StatusServiceTest.java` (new class `StatusWiringTest.java` is fine if the file gets long)

**Interfaces:**
- Consumes: Task 3 `StatusService`.
- Produces: `MilestoneResponse` gains trailing `boolean needsReview`; `ProjectResponse` gains trailing `boolean needsReview, String cancelReason`; `MilestoneRequest` becomes `(title, description, dueDate, sequenceOrder)`; `ProjectRequest` loses `status`.

- [ ] **Step 1: Write the failing tests.** Same Spring setup as Task 3, plus a `@BeforeEach` that binds a request so `AuditContext` works: `RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()))`, and an `@AfterEach` that calls `RequestContextHolder.resetRequestAttributes()`. Tests:

```java
staleClientStatusFieldIsIgnored:                       // Review Focus 5
    // autowire the app's ObjectMapper (not new ObjectMapper(); the Boot one ignores unknown fields)
    // read {"title":"t","dueDate":"2027-01-01","sequenceOrder":1,"status":"COMPLETED","completedDate":"2026-01-01"}
    // into MilestoneRequest and assert it does not throw
milestoneCreateStartsPending:
    // milestoneService.create(projectId, new MilestoneRequest("t", null, today.plusDays(10), 1), agencyId, false)
    // assert response.status() equals "PENDING" and response.needsReview() is false
milestoneCreatedPastDueIsOverdueAndProjectDelayed:
    // same with due date yesterday: status "OVERDUE"; project row is DELAYED afterwards
attachmentDeleteDropsStatusThroughTheService:
    // complete a milestone with a repository-saved attachment + refreshMilestone, then
    // attachmentService.delete(milestoneId, attachmentId, uploaderId, false); assert milestone PENDING
completedMilestoneResponseCarriesAsterisk:
    // after completion, flush/clear, MilestoneResponse.from(milestoneRepository.findById(id).get()).needsReview() is true
    // ProjectResponse.from(project).needsReview() is true and ProjectResponse.publicView(project).needsReview() is true
```

- [ ] **Step 2: Run, confirm failure.** `mvn test -Dtest=StatusWiringTest`. Expected: compilation errors (old record shapes).
- [ ] **Step 3: Request DTOs.** Remove `status` and `completedDate` from `MilestoneRequest`; remove `status` from `ProjectRequest`. Delete the status and completedDate lines from `MilestoneService.applyRequest` and the `if (req.status() != null)` line from `ProjectService.applyRequest`. Jackson drops unknown fields, so old clients keep working.
- [ ] **Step 4: Response DTOs.** `MilestoneResponse.from`: `needsReview = StatusDeriver.milestoneNeedsReview(m.getStatus(), m.getAttachments().size())` (phase 1: every proof is unreviewed). `ProjectResponse.from` and `publicView`: `needsReview = StatusDeriver.projectNeedsReview(p.getStatus(), p.getMilestones().stream().anyMatch(ms -> StatusDeriver.milestoneNeedsReview(ms.getStatus(), ms.getAttachments().size())))` and `cancelReason = p.getCancelReason()`. Add `// ponytail: lazy attachment loading is N+1 across a project list; fine at this scale, batch-fetch if the list slows` where the collection is read. Grep for other `new MilestoneResponse(` / `new ProjectResponse(` calls and fix them.
- [ ] **Step 5: `MilestoneService`.** Inject `StatusService`. In `create` and `update`, call `statusService.refreshMilestone(saved.getId())` after the save, before building the response (rebuild the response from the refreshed entity). Remove the old manual STATUS_CHANGE alert block in `update`. In `submitReport`, drop the status and completedDate handling; it keeps the description update and the REPORT_SUBMITTED alert. Change the controller's `@Operation` summary to "Update milestone details (owning agency or ADMIN only)".
- [ ] **Step 6: `AttachmentService`.** Inject `StatusService`. Call `statusService.refreshMilestone(milestoneId)` at the end of `upload`, `delete` and `restore`.
- [ ] **Step 7: `ProjectService.restore`.** After the milestone and attachment restores, call `statusService.refreshMilestone(id)` for each of the project's milestones (the last call refreshes the project too), then `statusService.refreshProject(id)` so a project with no milestones is also recomputed.
- [ ] **Step 8: `AlertScheduler`.** Inject `StatusService`. In `checkOverdueMilestones`, replace `m.setStatus(OVERDUE)` with `statusService.refreshMilestone(m.getId())`; keep the DEADLINE_MISSED alert, the delay-day severity and the log lines. Compute `getDelayDays()` after the refresh so it sees OVERDUE.
- [ ] **Step 9: `StatusBackfillRunner`.** A `@Component implements ApplicationRunner` that calls `statusService.refreshAllQuietly()` in `run`. One-line javadoc: recomputes every status at startup so existing data catches up after this change and any drift heals itself.
- [ ] **Step 10: Run all tests.** `mvn test`. Expected: all pass, BUILD SUCCESS.
- [ ] **Step 11: Checkpoint** (ask first). Message idea: "Derive milestone and project status from proofs; drop the manual dropdown fields".

---

### Task 5: Cancel a project (the one manual status)

**Files:**
- Create: `milestone-service/src/main/java/ph/trackph/milestone/dto/request/CancelProjectRequest.java`
- Modify: `service/ProjectService.java`, `controller/ProjectController.java`, `config/SecurityConfig.java`
- Test: add to `StatusWiringTest.java`

**Interfaces:**
- Produces: `record CancelProjectRequest(@NotBlank(message = "A reason is required") String reason)`; `ProjectResponse ProjectService.cancel(Long id, String reason, Long requesterId, boolean requesterIsAdmin)`; `POST /api/v1/projects/{id}/cancel`.

- [ ] **Step 1: Write the failing tests.**

```java
cancelRecordsReasonAndSticksThroughRefresh:
    // projectService.cancel(projectId, "Budget reallocated", agencyId, false)
    // response.status() equals "CANCELLED", response.cancelReason() equals "Budget reallocated"
    // statusService.refreshProject(projectId) leaves it CANCELLED
cancelNeedsAReason:
    // assertThrows(IllegalArgumentException.class, () -> projectService.cancel(projectId, "  ", agencyId, false))
cancelRejectsOtherAgencies:
    // a second agency user -> assertThrows(UnauthorizedException.class, ...)
```

- [ ] **Step 2: Run, confirm failure.** `mvn test -Dtest=StatusWiringTest`.
- [ ] **Step 3: Implement.** `ProjectService.cancel`: `requireOwnerOrAdmin`, reject blank reason with `IllegalArgumentException("A reason is required to cancel a project")`, snapshot the "before" `ProjectResponse`, set status CANCELLED and `cancelReason`, save, `auditContext.recordChange("Project", id, before, after)`, return after. Controller: `@PostMapping("/{id}/cancel")` taking `@Valid @RequestBody CancelProjectRequest`. `SecurityConfig`: add `.requestMatchers(HttpMethod.POST, "/api/v1/projects/*/cancel").hasAnyRole("AGENCY", "ADMIN")` before the existing `/api/v1/projects` POST matcher (the file is one long line; insert with a targeted Edit).
- [ ] **Step 4: Run all tests.** `mvn test`. Expected: pass.
- [ ] **Step 5: Checkpoint** (ask first). Message idea: "Add cancelling a project with a required reason".

---

### Task 6: Pages show derived status

**Files (under `milestone-service/src/main/resources/`):**
- Create: `static/js/status-label.js`
- Modify: `templates/layout.html`, `templates/projects/detail.html`, `templates/projects/list.html`, `templates/index.html`

**Interfaces:**
- Produces: global `statusLabel(status, needsReview)` returning `(needsReview ? '*' : '') + status`.

- [ ] **Step 1:** Create `status-label.js` with that function and a one-line comment saying the asterisk leads on purpose so cut-off text keeps it. Add `<script th:src="@{/js/status-label.js}"></script>` in `layout.html` next to the other script tags.
- [ ] **Step 2: `detail.html`.**
  - Delete the "Status" `form-group` (the `edit-status` select), the `edit-status` assignment in `toggleEditProject`, and `status:` in `saveProject`'s body.
  - Project badge: `badge.textContent = statusLabel(currentProject.status, currentProject.needsReview)`. Milestone badge: `statusLabel(m.status, m.needsReview)`.
  - Delete the per-milestone status `<select>` and the whole `updateMilestoneStatus` function.
  - Remove `status` and `completedDate` from the bodies in `saveMilestone`, `submitReport` and `addMilestone`.
  - Add a "Cancel project" button next to Delete (AGENCY/ADMIN only, hidden when status is CANCELLED) calling `cancelProject()`: `prompt('Why is this project being cancelled?')`, abort on empty, POST `{reason}` to `/api/v1/projects/${projectId}/cancel`, then `loadProject()`. When `status === 'CANCELLED'` and `cancelReason` is set, show `Cancelled: <reason>` under the description.
  - Add the legend `<p class="text-sm text-muted">* counted automatically, awaiting human check</p>` under the page header.
- [ ] **Step 3: `list.html`.** Badge text uses `statusLabel(p.status, p.needsReview)`; the map popup status does too; add the same legend above the table. The stat tiles keep counting on `p.status`.
- [ ] **Step 4: `index.html`** line with `p.status.replace('_',' ')`: wrap as `statusLabel(p.status.replace('_',' '), p.needsReview)`.
- [ ] **Step 5: Checkpoint** (ask first). Message idea: "Show derived status with a leading asterisk and drop the status dropdowns from the pages". Verification of the pages happens in Task 8.

---

### Task 7: Seed data that still shows every status

**Files:**
- Create: `milestone-service/src/main/resources/db/migration/V13__seed_placeholder_proofs.sql`, `milestone-service/src/main/resources/seed/placeholder-proof.pdf`
- Modify: `milestone-service/src/main/java/ph/trackph/milestone/service/FileStorageService.java`
- Test: `milestone-service/src/test/java/ph/trackph/milestone/service/FileStorageServiceTest.java`

**Interfaces:**
- Consumes: existing `FileStorageService(String dir)` and `read(String storedFilename): byte[]`.
- Produces: `read` returns the bundled placeholder bytes for any stored filename starting with `seed-proof-`.

- [ ] **Step 1: Write the failing test.** Using `@TempDir Path dir`: `new FileStorageService(dir.toString()).read("seed-proof-42.pdf")` returns bytes whose first four characters are `%PDF`. A second test: reading a real missing non-seed name (`"nope.pdf"`) still throws `IOException`.
- [ ] **Step 2: Run, confirm failure.** `mvn test -Dtest=FileStorageServiceTest`.
- [ ] **Step 3: Implement.** Create `seed/placeholder-proof.pdf` as a tiny valid one-page PDF with the text "Demo placeholder proof". In `FileStorageService.read`, when `storedFilename.startsWith("seed-proof-")`, read the bytes from the classpath resource `/seed/placeholder-proof.pdf` instead of disk. Comment why: seeded demo rows have no file on disk.
- [ ] **Step 4: Run, confirm pass.** Same command.
- [ ] **Step 5: Write V13.** Insert one attachment per seeded COMPLETED milestone of the demo agency, so the demo keeps its spread:

```sql
INSERT INTO milestone_attachments (milestone_id, original_filename, stored_filename, content_type, file_size, uploaded_by)
SELECT m.id, 'placeholder-proof.pdf', CONCAT('seed-proof-', m.id, '.pdf'), 'application/pdf', 1024, u.id
FROM milestones m
JOIN projects p ON p.id = m.project_id
JOIN users u ON u.id = p.agency_id
WHERE m.status = 'COMPLETED' AND m.deleted_at IS NULL AND u.username = 'doh_agency'
  AND NOT EXISTS (SELECT 1 FROM milestone_attachments a WHERE a.milestone_id = m.id);
```

  Add a top comment: seed-only, scoped to the demo agency so a real deployment's data is never touched. Seeded IN_PROGRESS milestones will drop to PENDING in phase 1 (known limit in the spec).
- [ ] **Step 6: Checkpoint** (ask first). Message idea: "Seed placeholder proofs so the demo still shows every status".

---

### Task 8: Docs and end-to-end check

**Files:**
- Modify: `trackph-microservice-template/docs/dev-guide.md`, `milestone-service/CLAUDE.md` (gitignored, local only), `milestone-service/TODO.md`

- [ ] **Step 1: Docs.** Write in a natural voice, not boilerplate. In `dev-guide.md` add a short "Derived Status" section: status is computed by `StatusDeriver`/`StatusService`, the asterisk meaning and where it is derived, why CANCELLED is the one manual status, and that phase 1 uses the one-attachment stand-in. Add one anti-pattern line: don't set `status` anywhere except `StatusService`, and don't add a status dropdown. In the service `CLAUDE.md` update the notes that mention status or the heatmap exception if they now read wrong, and mention `StatusService`. In `TODO.md` add a short entry; do not rewrite the old entries that describe the removed dropdowns.
- [ ] **Step 2: Full test run.** From `milestone-service/`: `mvn test`. Expected: BUILD SUCCESS, zero failures. Then `git status`: delete any stray upload files the tests created, never commit them.
- [ ] **Step 3: Ask before starting the app.** A preview server (serverId `e90a277e-0287-4c72-8b5f-2533742bd428`) may still be running on 8081. Check with `netstat -ano | grep ":8081 "`. Tell the user the restart will run V12 and V13 against their real MySQL demo database, and get a yes first. If yes: `preview_stop` the old one, then `preview_start` with `{name: "milestone-service"}`.
- [ ] **Step 4: Verify in the app.** Check `preview_logs` for Flyway applying V12 and V13 and no startup error. Open `/projects`: statuses show `*` labels where they rest on a proof, the legend is visible, the map popups show the label. Open a project detail page: no status dropdowns, Cancel button present for the agency, no JS console errors (`read_console_messages`). Run in the page console: `fetch('/api/v1/projects').then(r=>r.json())` and confirm `needsReview` and `status` are present for a public (no token) caller. Take a screenshot as proof.
- [ ] **Step 5: Checkpoint** (ask first). Message idea: "Document derived status and update the dev guide". Offer a push only if the user asks.

---

## Self-review notes

- **Spec coverage (phase 1):** `StatusDeriver` (T1), single status writer and recompute events (T3, T4), dropdowns removed in UI and ignored by the server (T4, T6), cancel with reason (T5), STATUS_CHANGE alert on derived change (T3), midnight job through `StatusService` and projects reaching DELAYED (T4), `completedDate` handling (T3), seed data and migration (T7), existing statuses recomputed on first run (T4 runner), asterisk and legend on list, detail, index and map popups (T6), public visibility (T4 `publicView`), docs in the same change (T8). Phases 2 and 3 are out of scope.
- **Types:** `StatusDeriver` signatures in T1 match every later use; `refreshMilestone(Long)`, `refreshProject(Long)`, `refreshAllQuietly()` match T4 to T5; DTO shapes in T4 match what T6 reads (`needsReview`, `cancelReason`).
- **Alert dedupe:** `AlertService.createAlert` skips a second alert with the same project, milestone and type, so a milestone only ever gets one STATUS_CHANGE alert. That is existing behaviour and this plan leaves it alone.
