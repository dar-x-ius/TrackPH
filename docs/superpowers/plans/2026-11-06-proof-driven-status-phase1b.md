# Claimed Status (Phase 1b) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the phase 1 stand-in ("one proof completes a milestone") with agency-claimed milestone status that needs a proof, adds COMPLETED_LATE, derives project status from the milestones and the project end date, and records the original end date so extensions are visible.

**Architecture:** A milestone stores one `claim` (IN_PROGRESS or COMPLETED) that exists only while a live proof exists. `StatusDeriver` stays the pure rulebook and gets new signatures. `StatusService` stays the only writer of `status`, `claim`-clearing and `completedDate`. The project gets an immutable `originalEndDate`. The pages gain a claim choice on upload, the COMPLETED_LATE badge, an "extended" flag and a caution when a milestone is due after the project end date.

**Tech Stack:** Spring Boot 3.3.4, Spring Data JPA, Flyway (MySQL), H2 in the `dev` profile for tests, Thymeleaf with plain JS, JUnit 5. Tests run with `mvn test` from `milestone-service/`.

**Spec:** `docs/superpowers/specs/2026-11-06-proof-driven-status-design.md`, section "Revision: claimed status (phase 1b)". It overrides the phase 1 stand-in and project rules in the same file.

## Global Constraints

- Next free Flyway number is `V14`. Never edit an applied migration. Flyway is off in the `dev` profile, so migrations get no automated test.
- `StatusService` is the only code that writes `Milestone.status`, `Milestone.claim` (clearing) and `Milestone.completedDate`, and `Project.status` except CANCELLED. `StatusDeriver` stays static and pure: no database, no clock argument other than `today`.
- Milestone statuses: PENDING, IN_PROGRESS, COMPLETED, COMPLETED_LATE, OVERDUE. Project statuses: PLANNED, IN_PROGRESS, DELAYED, COMPLETED, COMPLETED_LATE, CANCELLED. "Done" means COMPLETED or COMPLETED_LATE.
- A proof is mandatory for any claim. Proofs are soft deleted only. Never expose a deleted proof's file name or content.
- The completion date is the system date when the COMPLETED claim is first made. It is never taken from a request.
- The `*` is derived per response and never stored. It applies to IN_PROGRESS, COMPLETED and COMPLETED_LATE.
- Do not commit or push. The user commits when they ask. Each task ends at its test run, not at a commit.
- Chat replies stay in Simplified Technical English. Docs and code comments follow the humanizer style: comment only for a non-obvious why.

## Review Focus

1. A project with one finished milestone that is due before the project end date must stay IN_PROGRESS, not COMPLETED.
2. A COMPLETED_LATE milestone must not be flagged again by the midnight job (`findNewlyOverdue`).
3. Deleting the last proof after a COMPLETED claim must clear the claim and the completion date, and a new COMPLETED claim must get a fresh completion date.
4. Editing a project end date must recalculate the project status (forward extension clears DELAYED), and must never change the original end date.
5. A project with no end date (the request allows it) must not crash the rules or stay stuck.

---

### Task 1: New status rules in StatusDeriver

**Files:**
- Modify: `milestone-service/src/main/java/ph/trackph/milestone/model/Milestone.java` (Status enum, new Claim enum)
- Modify: `milestone-service/src/main/java/ph/trackph/milestone/model/Project.java` (Status enum)
- Modify: `milestone-service/src/main/java/ph/trackph/milestone/service/StatusDeriver.java`
- Test: `milestone-service/src/test/java/ph/trackph/milestone/service/StatusDeriverTest.java` (rewrite)

**Interfaces:**
- Produces:
  - `Milestone.Status` gains `COMPLETED_LATE`. `Project.Status` gains `COMPLETED_LATE`.
  - `public enum Milestone.Claim { IN_PROGRESS, COMPLETED }`
  - `public record StatusDeriver.MilestoneFacts(Milestone.Status status, LocalDate dueDate, LocalDate completedDate)`
  - `static Milestone.Status deriveMilestone(LocalDate dueDate, LocalDate today, boolean hasProof, Milestone.Claim claim, LocalDate completedDate)`
  - `static Project.Status deriveProject(Project.Status current, LocalDate endDate, LocalDate today, List<MilestoneFacts> milestones)`
  - `static boolean isDone(Milestone.Status s)`
  - `milestoneNeedsReview` and `projectNeedsReview` keep their signatures. `DEFAULT_REQUIRED_PROOFS` is removed.

- [ ] **Step 1: Rewrite `StatusDeriverTest` with these tests (fixed `TODAY = 2026-11-06`)**

Milestone, one line each, using `deriveMilestone(due, TODAY, hasProof, claim, completedDate)`:
  - `noProofPastDueIsOverdue`: `(TODAY-1, false, null, null)` gives OVERDUE.
  - `noProofNotDueIsPending`: `(TODAY+5, false, null, null)` gives PENDING.
  - `proofWithoutClaimIsIgnored`: `(TODAY+5, true, null, null)` gives PENDING.
  - `completedClaimBeforeDueIsCompleted`: `(TODAY+5, true, COMPLETED, TODAY)` gives COMPLETED.
  - `completedClaimOnDueDateIsCompleted`: `(TODAY, true, COMPLETED, TODAY)` gives COMPLETED.
  - `completedClaimAfterDueIsCompletedLate`: `(TODAY-3, true, COMPLETED, TODAY)` gives COMPLETED_LATE.
  - `inProgressClaimBeforeDueIsInProgress`: `(TODAY+5, true, IN_PROGRESS, null)` gives IN_PROGRESS.
  - `inProgressClaimPastDueIsOverdue`: `(TODAY-1, true, IN_PROGRESS, null)` gives OVERDUE.

Project, using `deriveProject(current, end, TODAY, facts)` with a helper `f(status, due, completed)`:
  - `cancelledSticks`: CANCELLED with all milestones done stays CANCELLED.
  - `noMilestonesIsPlanned`.
  - `oneFinishedMilestoneShortOfEndDateIsInProgress` (the hole): end `TODAY+60`, one COMPLETED milestone due `TODAY-10`, completed `TODAY-12`, gives IN_PROGRESS.
  - `allDoneLastDueOnEndDateIsCompleted`: end `TODAY+10`, milestones due `TODAY-5` and `TODAY+10`, both COMPLETED, completed dates before the end date, gives COMPLETED.
  - `allDoneButFinishedAfterEndDateIsCompletedLate`: end `TODAY-2`, last milestone due `TODAY-2`, COMPLETED_LATE, completed `TODAY`, gives COMPLETED_LATE.
  - `milestoneDueAfterEndDateMakesCompletionLate`: end `TODAY+5`, last milestone due `TODAY+20` and COMPLETED completed `TODAY`, gives COMPLETED_LATE.
  - `completedProjectNeverShowsDelayedEvenPastEndDate`: the case above with end date in the past still gives COMPLETED_LATE.
  - `anyOverdueIsDelayed`.
  - `pastEndDateAndNotDoneIsDelayed`: end `TODAY-1`, milestones PENDING, gives DELAYED.
  - `milestoneDueAfterEndDateIsDelayedNow`: end `TODAY+30`, one PENDING milestone due `TODAY+40`, gives DELAYED.
  - `recoveredProjectIsInProgress`: end `TODAY+30`, one COMPLETED_LATE and one PENDING due `TODAY+30`, gives IN_PROGRESS.
  - `startedIsInProgress`: an IN_PROGRESS milestone, end in the future, gives IN_PROGRESS.
  - `nothingStartedIsPlanned`.
  - `nullEndDateIgnoresEndDateRules`: end `null`, all milestones COMPLETED, gives COMPLETED; end `null` with a PENDING milestone gives PLANNED.

Asterisk:
  - `milestoneAsteriskRules`: `milestoneNeedsReview` is true for IN_PROGRESS, COMPLETED and COMPLETED_LATE with 1 or more proofs, false with 0 proofs, false for OVERDUE and PENDING.
  - `projectAsteriskRules`: `projectNeedsReview(status, true)` is true for COMPLETED, COMPLETED_LATE and IN_PROGRESS, false for DELAYED, PLANNED and CANCELLED; false whenever the second argument is false.

- [ ] **Step 2: Run the test to verify it fails**

Run: `mvn -q test -Dtest=StatusDeriverTest` (from `milestone-service/`)
Expected: compile errors (`COMPLETED_LATE`, `Claim`, `MilestoneFacts` do not exist).

- [ ] **Step 3: Add the enum values and `Milestone.Claim`; rewrite `StatusDeriver`**

Add `COMPLETED_LATE` to both `Status` enums and `public enum Claim { IN_PROGRESS, COMPLETED }` inside `Milestone`. Implement the rules exactly as the spec's two ordered lists say. Milestone: no proof or null claim gives OVERDUE or PENDING by due date; claim COMPLETED gives COMPLETED_LATE when `completedDate` is non-null and after `dueDate`, else COMPLETED; claim IN_PROGRESS gives OVERDUE when past due, else IN_PROGRESS. Project, with `maxDue` the latest due date and `latestCompleted` the latest non-null completed date among the facts: CANCELLED sticks; empty list gives PLANNED (check this before `allMatch`); all done and (`endDate == null` or `maxDue` not before `endDate`) gives COMPLETED_LATE when `endDate != null` and (`maxDue` after `endDate` or `latestCompleted` after `endDate`), else COMPLETED; any OVERDUE, or `endDate != null` and (`today` after `endDate` or `maxDue` after `endDate`), gives DELAYED; any IN_PROGRESS or done gives IN_PROGRESS; else PLANNED. Keep the existing Javadoc comment explaining why the empty check comes first. Extend both `needsReview` helpers to the new statuses.

- [ ] **Step 4: Run the test to verify it passes**

Run: `mvn -q test -Dtest=StatusDeriverTest`
Expected: PASS. (The rest of the module no longer compiles until Task 3. That is expected: run only this class, and expect `mvn test-compile` to fail on `StatusService` until Task 3.)

If Maven compiles the whole module and fails on `StatusService` for the old signatures, apply the minimum edit to `StatusService` to compile (call the new signatures with `hasProof` and the milestone's claim, `Milestone.getClaim()` added in Task 2) and let Task 3 finish the logic. Ledger it as a ruling.

---

### Task 2: Persist the claim and the original end date

**Files:**
- Create: `milestone-service/src/main/resources/db/migration/V14__claimed_status_and_original_end_date.sql`
- Modify: `milestone-service/src/main/java/ph/trackph/milestone/model/Milestone.java` (field `claim`, getter and setter)
- Modify: `milestone-service/src/main/java/ph/trackph/milestone/model/Project.java` (field `originalEndDate`, getter and setter)
- Modify: `milestone-service/src/main/java/ph/trackph/milestone/service/ProjectService.java` (`applyRequest`, `update`)
- Modify: `milestone-service/src/main/java/ph/trackph/milestone/dto/response/ProjectResponse.java`
- Test: `milestone-service/src/test/java/ph/trackph/milestone/service/StatusWiringTest.java`

**Interfaces:**
- Consumes: `Milestone.Claim` from Task 1.
- Produces:
  - `Milestone.getClaim(): Milestone.Claim`, `setClaim(Milestone.Claim)`. Column `claim`, nullable, `@Enumerated(EnumType.STRING)`.
  - `Project.getOriginalEndDate(): LocalDate`, `setOriginalEndDate(LocalDate)`. Column `original_end_date`.
  - `ProjectResponse` gains two trailing components: `LocalDate originalEndDate, boolean endDateExtended`, in both `from` and `publicView`. `endDateExtended` is true when `originalEndDate != null && endDate != null && endDate.isAfter(originalEndDate)`.

- [ ] **Step 1: Write the failing tests in `StatusWiringTest`**
  - `originalEndDateIsSetOnceAndNeverMoves`: create a project through `projectService.create(agency.getId(), request)` with end date `D`; assert `originalEndDate == D` and `endDateExtended == false`. Update it with end `D+30`; assert `originalEndDate == D`, `endDate == D+30`, `endDateExtended == true`. Update again with end `D-5`; assert `originalEndDate == D` and `endDateExtended == false`.
  - `endDateIsSetLaterBecomesTheOriginal`: create with `endDate = null`; update with end `E`; assert `originalEndDate == E`.
  - `extendingTheEndDateClearsDelayed` (Review Focus 4): project end `today-1` with one PENDING milestone due `today+5`. Call `statusService.refreshProject`; assert DELAYED. Update the project through `projectService.update` with end `today+30`; assert the returned `status` is `"PLANNED"` (nothing has started, so the project falls back to PLANNED).

(Use a `ProjectRequest(name, null, start, end, null, null, null, null)` helper. The repository-saved test project gets its `originalEndDate` set in the helper `project(...)` so the setup matches production.)

- [ ] **Step 2: Run to verify failure**

Run: `mvn -q test -Dtest=StatusWiringTest`
Expected: compile or assertion failures on `originalEndDate`.

- [ ] **Step 3: Implement**
  - `V14` migration: widen both `status` ENUM columns to include `COMPLETED_LATE` (milestones: `'PENDING','IN_PROGRESS','COMPLETED','COMPLETED_LATE','OVERDUE'`; projects: `'PLANNED','IN_PROGRESS','DELAYED','COMPLETED','COMPLETED_LATE','CANCELLED'`); `ALTER TABLE milestones ADD COLUMN claim ENUM('IN_PROGRESS','COMPLETED') NULL`; `ALTER TABLE projects ADD COLUMN original_end_date DATE NULL`; `UPDATE projects SET original_end_date = end_date`; `UPDATE milestones m SET m.claim = 'COMPLETED' WHERE EXISTS (SELECT 1 FROM milestone_attachments a WHERE a.milestone_id = m.id AND a.deleted_at IS NULL)` with a comment: phase 1 completed every milestone that had a proof, so those proofs become COMPLETED claims.
  - Entity fields and accessors as in Interfaces.
  - `ProjectService.applyRequest`: after the `endDate` assignment, `if (project.getOriginalEndDate() == null && project.getEndDate() != null) project.setOriginalEndDate(project.getEndDate());` (covers create and a first end date added later).
  - `ProjectService.update`: after `applyRequest`, save, call `statusService.refreshProject(id)`, then build `after` from the refreshed project (flush and re-read through `getOrThrow(id)` so the response shows the new status).
  - `ProjectResponse`: add the two components and fill them in `from` and `publicView`.

- [ ] **Step 4: Run to verify they pass**

Run: `mvn -q test -Dtest=StatusWiringTest`
Expected: the three new tests pass. Older tests in the file may still fail until Task 3 and Task 4 finish. Note any ledger-worthy ones; do not edit them yet.

---

### Task 3: StatusService works from the claim

**Files:**
- Modify: `milestone-service/src/main/java/ph/trackph/milestone/service/StatusService.java`
- Modify: `milestone-service/src/main/java/ph/trackph/milestone/repository/MilestoneRepository.java` (`findNewlyOverdue`)
- Test: `milestone-service/src/test/java/ph/trackph/milestone/service/StatusServiceTest.java`
- Test: `milestone-service/src/test/java/ph/trackph/milestone/service/StatusWiringTest.java` (adapt older tests)

**Interfaces:**
- Consumes: new `StatusDeriver` signatures (Task 1), `Milestone.claim` and `Project.originalEndDate` (Task 2).
- Produces:
  - `@Transactional public void StatusService.recordClaim(Long milestoneId, Milestone.Claim claim)`: sets the claim; on COMPLETED sets `completedDate` to today only when it is null; on IN_PROGRESS clears `completedDate`; then runs `refreshMilestone(milestoneId)`.
  - `refreshMilestone`, `refreshProject`, `refreshAllQuietly` keep their signatures.

- [ ] **Step 1: Update and add tests in `StatusServiceTest`**

Helper change: `attach(m, uploader)` stays (it only saves a proof). Add `claim(m, claim)` that calls `statusService.recordClaim(m.getId(), claim)`. Rewrite the existing tests to the new rules and add:
  - `completedClaimCompletesAndStartsProject`: two milestones (due +30 and +60), project end `+60`. Attach and claim COMPLETED on the first: milestone COMPLETED with `completedDate == today`, project IN_PROGRESS. Same on the second: project COMPLETED.
  - `inProgressClaimShowsInProgress`.
  - `pastDueMilestoneWithoutProofIsOverdueAndProjectDelayed` (unchanged behaviour).
  - `claimAfterDueDateIsCompletedLate`: milestone due `-3`, proof, claim COMPLETED: status COMPLETED_LATE.
  - `completedLateIsNotFlaggedByTheMidnightJob` (Review Focus 2): after the case above, `milestoneRepository.findNewlyOverdue(LocalDate.now())` does not contain it.
  - `softDeletingTheOnlyProofClearsClaimAndCompletedDate` (Review Focus 3): after a COMPLETED claim, soft delete the proof, `refreshMilestone`; assert PENDING, `getClaim() == null`, `getCompletedDate() == null`. Then attach again and claim COMPLETED; assert `completedDate == today`.
  - `proofWithoutClaimStaysPending`: a proof saved straight to the repository with no claim gives PENDING.
  - `cancelledProjectStaysCancelled`, `refreshAllQuietlyIsIdempotentAndSilent`, `completionRaisesStatusChangeAndOverdueRaisesDeadlineMissed`: keep, adapted to claim calls. The alert test must claim COMPLETED on the "done" milestone, expect one STATUS_CHANGE alert, and expect DEADLINE_MISSED for the overdue one.
  - `projectWithNoEndDateStillDerives` (Review Focus 5): project end date `null`, one milestone claimed COMPLETED: project COMPLETED, no exception.

In `StatusWiringTest`, the older tests that attach through the repository and then expect COMPLETED (`cancelRecordsReasonAndSticksThroughRefresh`, `attachmentDeleteDropsStatusThroughTheService`, `completedMilestoneResponseCarriesAsterisk`) must call `statusService.recordClaim(..., COMPLETED)` after `attach`.

- [ ] **Step 2: Run to verify failure**

Run: `mvn -q test -Dtest=StatusServiceTest+StatusWiringTest`
Expected: FAIL (compile error on `recordClaim`, then assertion failures).

- [ ] **Step 3: Implement**
  - `StatusService.refresh(Milestone, boolean alert)`: `hasProof = attachmentRepository.countByMilestoneId(id) > 0`. If no proof, set claim null and `completedDate` null. Else if claim is COMPLETED and `completedDate` is null, set it to today (covers the startup catch-up). Else if claim is not COMPLETED, clear `completedDate`. Derive with the new signature. Save and alert only when something changed (status, claim or completed date). Alert rules stay: OVERDUE gives DEADLINE_MISSED with severity from the delay days; any other status change gives STATUS_CHANGE (LOW). Keep both existing comments.
  - `StatusService.refresh(Project)`: build `List<MilestoneFacts>` from `findByProjectIdOrderBySequenceOrderAsc`, call `deriveProject(project.getStatus(), project.getEndDate(), LocalDate.now(), facts)`.
  - `recordClaim` as in Interfaces.
  - `MilestoneRepository.findNewlyOverdue`: add `COMPLETED_LATE` to the `NOT IN` list.

- [ ] **Step 4: Run to verify they pass**

Run: `mvn -q test -Dtest=StatusServiceTest+StatusWiringTest+StatusDeriverTest`
Expected: PASS. Then `mvn -q test` for the whole module. Expected: PASS except tests that need Task 4 (none, if the plan order held).

---

### Task 4: Upload requires a claim

**Files:**
- Modify: `milestone-service/src/main/java/ph/trackph/milestone/service/AttachmentService.java`
- Modify: `milestone-service/src/main/java/ph/trackph/milestone/controller/AttachmentController.java`
- Test: `milestone-service/src/test/java/ph/trackph/milestone/service/StatusWiringTest.java`

**Interfaces:**
- Consumes: `StatusService.recordClaim` (Task 3).
- Produces: `AttachmentService.upload(Long milestoneId, MultipartFile file, Milestone.Claim claim, Long uploaderId, boolean uploaderIsAdmin)` and `POST /api/v1/milestones/{id}/attachments` taking a required multipart field `claim` (`IN_PROGRESS` or `COMPLETED`).

- [ ] **Step 1: Write the failing tests in `StatusWiringTest`**
  - `uploadWithoutAClaimIsRejected`: call `attachmentService.upload(m.getId(), file, null, agency.getId(), false)` with a `MockMultipartFile("file", "proof.pdf", "application/pdf", new byte[]{1})`. Expect `IllegalArgumentException`, and `attachmentRepository.countByMilestoneId(m.getId()) == 0`. The claim check runs before the file is stored, so the test writes nothing to disk.
  - `restoringTheOnlyProofBringsBackNothingWithoutAClaim`: soft delete the only proof (claim cleared), restore it with `attachmentService.restore`; assert the milestone stays PENDING (the claim does not come back by itself).

- [ ] **Step 2: Run to verify failure**

Run: `mvn -q test -Dtest=StatusWiringTest`
Expected: FAIL (signature mismatch).

- [ ] **Step 3: Implement**
  - `AttachmentService.upload`: first line rejects `claim == null` with `IllegalArgumentException("Choose a status for this proof: IN_PROGRESS or COMPLETED")`. After saving the attachment, call `statusService.recordClaim(milestoneId, claim)` instead of `refreshMilestone`. `delete` and `restore` keep calling `refreshMilestone`.
  - `AttachmentController.upload`: add `@RequestParam("claim") Milestone.Claim claim` and pass it through. Update the `@Operation` summary to mention the claim.

- [ ] **Step 4: Run to verify they pass**

Run: `mvn -q test`
Expected: PASS for the whole module.

---

### Task 5: Deleted-proof trail without the file name

**Files:**
- Modify: `milestone-service/src/main/java/ph/trackph/milestone/service/AttachmentService.java` (`findArchivedByMilestone`)
- Modify: `milestone-service/src/main/java/ph/trackph/milestone/controller/AttachmentController.java` (`@Operation` text)
- Test: `milestone-service/src/test/java/ph/trackph/milestone/service/StatusWiringTest.java`

**Interfaces:**
- Produces: `findArchivedByMilestone(Long)` rows carry `id`, `uploadedByUsername`, `deletedAt` and no `originalFilename`.

- [ ] **Step 1: Write the failing test**
  - `archivedListingShowsWhoAndWhenButNotTheFileName`: attach a proof named `secret-id-scan.pdf`, delete it through `attachmentService.delete`, `entityManager.flush()`, then read `attachmentService.findArchivedByMilestone(m.getId())`. Assert one row, `containsKey("uploadedByUsername")`, `containsKey("deletedAt")`, and no key named `originalFilename` and no value containing `secret-id-scan`.

- [ ] **Step 2: Run to verify failure**

Run: `mvn -q test -Dtest=StatusWiringTest#archivedListingShowsWhoAndWhenButNotTheFileName`
Expected: FAIL (`originalFilename` is present).

- [ ] **Step 3: Implement**

Drop `a.original_filename AS originalFilename` from the SQL. Change the controller `@Operation` text to say the public list shows who deleted a proof and when, and never the file name.

- [ ] **Step 4: Run to verify it passes**

Run: `mvn -q test`
Expected: PASS.

---

### Task 6: Pages

No automated tests exist for the Thymeleaf pages. Verify in the browser pane after the user agrees to a restart (Task 7, Step 1).

**Files:**
- Modify: `milestone-service/src/main/resources/templates/projects/detail.html`
- Modify: `milestone-service/src/main/resources/templates/projects/list.html`
- Modify: `milestone-service/src/main/resources/templates/projects/archived.html`
- Modify: `milestone-service/src/main/resources/templates/index.html`

**Interfaces:**
- Consumes: the upload `claim` field (Task 4), `status` value `COMPLETED_LATE`, `originalEndDate` and `endDateExtended` on project responses (Task 2), the archived list shape (Task 5).

- [ ] **Step 1: Claim choice on upload (`detail.html`)**

In the per-milestone upload form add `<select id="attach-claim-${m.id}" class="form-control" style="width:auto;" required>` with options `IN_PROGRESS` ("Work in progress") and `COMPLETED` ("Completed"). `uploadAttachment` appends `formData.append('claim', ...)`. Add one line of helper text beside it: "Your choice shows with a * until someone checks the proof."

- [ ] **Step 2: COMPLETED_LATE everywhere a status is shown**

Add `COMPLETED_LATE: 'badge-warning'` to every `statusBadge` map (project and milestone maps in `detail.html`, plus `list.html`, `archived.html`, `index.html`). In `list.html` and `index.html` count COMPLETED_LATE in the "Completed" tile: `['COMPLETED','COMPLETED_LATE'].includes(p.status)`.

- [ ] **Step 3: Extended end date flag**

In `list.html`'s timeline cell and in the `detail.html` project header, when `p.endDateExtended` is true show `${p.endDate}` followed by a small `Extended` badge and `title`/text "was ${p.originalEndDate}". (Find the header spot where the project dates render in `loadProject()`.)

- [ ] **Step 4: Caution for a milestone due after the project end date**

In `addMilestone` and `saveMilestone`, compare the chosen due date with the loaded project's `endDate` (keep it in a variable set by `loadProject()`). When the due date is after it, call `confirm('This milestone is due after the project end date. The project will show DELAYED and finish as COMPLETED_LATE. If the delay is real, extend the project end date instead. Save anyway?')` and stop on Cancel.

- [ ] **Step 5: Deleted-proof note**

In `loadAttachments`, also fetch `/attachments/archived` and, when the count is above zero, append `<p class="text-sm text-muted">N proof(s) deleted. See archived attachments below.</p>`. In `loadArchivedAttachments`, replace the file name in each row with `Proof deleted by ${a.uploadedByUsername} on ${a.deletedAt}`.

- [ ] **Step 6: Static check**

Run: `grep -rn "status === 'COMPLETED'" milestone-service/src/main/resources/templates`
Expected: no matches left.

---

### Task 7: Run it, check the seed data, update docs and diagram

**Files:**
- Modify: `trackph-microservice-template/docs/dev-guide.md` ("Derived Status (milestone-service)" section)
- Modify: `milestone-service/TODO.md` (section 11)
- Modify: `milestone-service/CLAUDE.md` (local file, gitignored)
- Modify: `docs/derived-status-flow.svg`, regenerate `docs/derived-status-flow.png`

- [ ] **Step 1: Ask the user before restarting the app on port 8081**

A preview of `milestone-service` is already running there against the real MySQL demo database. The new migration (`V14`) runs on restart. Ask before restarting, and say so. After the yes, restart through `preview_start` and read `preview_logs` for a clean Flyway run to V14.
Expected: no errors.

- [ ] **Step 2: Check the seed data against the new rules**

Read `/api/v1/projects?size=50` and compare each project's status with what the rules give for its milestones and end date. List every project whose status changed since before the migration. If a seeded project changed in a way that breaks the demo (for example COMPLETED to IN_PROGRESS because its last milestone is due before its end date), report it to the user and ask before editing any data.

- [ ] **Step 3: Check the pages in the browser pane**

On a project detail page confirm: the claim select on the upload form, the `*` legend, the Extended flag (edit a throwaway project's end date forward), the COMPLETED_LATE badge, the caution on a milestone due after the end date, and the deleted-proof note. Check `read_console_messages` for new errors. Remember the Edit, Delete and Cancel buttons use `sec:authorize` and do not render in the pane. Test those through the API.

- [ ] **Step 4: Update the docs**

`dev-guide.md`: rewrite the "Derived Status" section to the new rules (claim, COMPLETED_LATE, project rules, original end date, deleted-proof trail, the `findNewlyOverdue` exclusion). `TODO.md` section 11: record phase 1b, remove the "IN_PROGRESS unreachable" limit, and add: the V14 migration has no automated test; the phase 1c extension approval workflow (owner applies, ADMIN approves) is next; an owner can still edit the end date directly until then. `CLAUDE.md`: update the derived-status note. Run the humanizer skill guidance over every prose change.

- [ ] **Step 5: Redraw the diagram**

Edit `docs/derived-status-flow.svg` so the rules boxes match the new milestone and project rules and the triggers box says "Proof uploaded with a claim". Render the PNG again with headless Edge (`--force-device-scale-factor=2 --window-size=1620,800`) and read it back to check the layout.

- [ ] **Step 6: Final full test run**

Run: `mvn -q test`
Expected: PASS, with the new total test count reported.
