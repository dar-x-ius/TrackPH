# Proof-driven status for projects and milestones

Status: draft for approval, 2026-11-06. Applies to `milestone-service`.

## Why we're doing this

Our reviewers pointed at one weakness: status is a thing a person types in, and the proofs sit off to the side. Today a milestone has its own status dropdown, a project has another, and an attachment is just a file with a name. Nothing connects them. You can mark a project COMPLETED while its milestones are still PENDING, and you can mark a milestone COMPLETED with nothing attached. The map's Delays lens and the dashboard's delayed count both read a project status that no code ever sets to DELAYED on its own.

We were asked to automate this, which for us means streamlining it. The direction is: proofs decide the milestone status, and milestones decide the project status. Nobody picks a status by hand.

We can't parse arbitrary proofs (photos, PDFs, scans, certificates), and we aren't going to try. The system never reads a file. It reads what the agency says the file is (its category), what the milestone says it needs, and the due date. Everything the computer decides comes from those labels, so the labels have to be structured and hard to abuse.

Labels can be abused, so the design has a second layer: a human review. Status updates immediately and automatically. Anything that rests on a proof nobody has checked yet is marked with an asterisk until a reviewer verifies it.

## Goals

- One piece of code, `StatusDeriver`, works out every milestone and project status from facts (proofs, review state, due dates, milestones). It is the only code that writes those statuses.
- The two status dropdowns go away. The server also ignores any status sent by hand.
- Status updates the moment a proof is attached, rejected, deleted, restored or reviewed, and when a due date passes. Nobody waits on a reviewer to see progress.
- A status that rests on an unreviewed proof carries an asterisk, everywhere it's shown.
- Reviewers get a queue that puts the suspicious proofs first, so review effort goes where fraud is likely.
- Rejections are public, with a reason. This is a transparency platform; hiding what got rejected would recreate the blind spot the platform exists to close.

## Non-goals

- Reading, OCR-ing or "understanding" proof files. Out of scope on purpose.
- Citizens disputing or flagging proofs. Worth a later look, not in this design.
- Multiple review tiers, assignment of reviewers, or notifications to reviewers.
- Changing the alert system beyond what's listed below.

## Anti-goals

These would make the work a failure even if it technically runs.

- Any way for a status to be set by hand, apart from cancelling a project.
- Anyone voting on a proof from their own agency, or on one they uploaded.
- Review blocking publication. Slowing agencies down is the thing we're avoiding.
- The asterisk disappearing when a label is cut off by small screens or narrow table columns.

## The rules

These are decided. The user agreed to the milestone and project rules and to the asterisk idea in the planning conversation.

### Milestone status

A *counting proof* is an attachment that is not deleted and not rejected. A *required proof* is a slot the milestone says it needs (see phase 2). Evaluate top to bottom, first match wins:

1. **COMPLETED**: every required proof is filled by a counting proof.
2. **OVERDUE**: not completed, and the due date has passed.
3. **IN_PROGRESS**: not completed, not overdue, and at least one counting proof exists.
4. **PENDING**: anything else.

### Project status

Archived (soft-deleted) milestones are ignored. Evaluate top to bottom:

1. **CANCELLED**: only if a person cancelled it, which needs a written reason. This is the one manual status.
2. **PLANNED**: the project has no milestones.
3. **COMPLETED**: every milestone is COMPLETED.
4. **DELAYED**: any milestone is OVERDUE.
5. **IN_PROGRESS**: any milestone is IN_PROGRESS or COMPLETED.
6. **PLANNED**: anything else.

### The asterisk

A status gets an asterisk when it is COMPLETED or IN_PROGRESS and at least one proof it depends on is UNREVIEWED. A project gets one when any milestone feeding it has one. PENDING, PLANNED, OVERDUE, DELAYED and CANCELLED never get one, since they don't rest on a proof.

The asterisk is derived each time. It is not a stored field and not a status of its own. When a reviewer verifies the last unreviewed proof behind a status, the asterisk disappears.

Display format is `*LABEL`, with the asterisk in front. Cut-off text nearly always loses the end of a label, so the front survives. (This is my call. The user offered `*LABEL` or `*LABEL*` and asked only that the asterisk survive bad scaling.) The page carries a short legend: "* counted automatically, awaiting human check". The legend and the asterisk appear on the project list, project detail, dashboard and map popups.

### Rejection

A rejected proof stops counting. The status recalculates and can drop back (COMPLETED to IN_PROGRESS, for example). The votes, the voters and the reasons are visible to the public. The existing audit log records the review action.

## The phases

Each phase leaves the app working. Ship them in order.

### Phase 1: derive status, drop the dropdowns, show the asterisk

This alone fixes the contradiction the user found.

- Add `StatusDeriver` with `deriveMilestone` and `deriveProject` (names are mine; pick better ones if they read badly).
- Call it after every event that can change a status: proof attach, proof delete or restore, milestone create, edit, archive or restore, and the midnight job. The midnight job (`AlertScheduler`) changes from "set OVERDUE" to "ask `StatusDeriver` for time-based changes", and now also moves projects to DELAYED.
- Keep storing `status` on both tables. Dashboards, the map's Delays lens and the sort columns already query it. `StatusDeriver` just becomes its only writer.
- Remove the project status dropdown in `detail.html` and the per-milestone status dropdown. Both `applyRequest` methods stop reading `status`. `submitReport` stops taking a status from the request and keeps its alert and note behaviour.
- Cancelling a project becomes its own small action with a required reason, owner or ADMIN only.
- `completedDate` is set when a milestone first becomes COMPLETED and cleared if it drops back, so it stays truthful after a rejection. (My call.)
- Keep the existing STATUS_CHANGE alert, now fired whenever the derived status actually changes.
- **Stand-in rule for this phase only:** categories and required-proof lists don't exist yet, so every milestone requires one proof and any attachment counts. A milestone with one or more attachments is `*COMPLETED`; with none it is PENDING or OVERDUE. All proofs are UNREVIEWED, because review doesn't exist yet, so every completed status carries the asterisk until phase 3.
- Known limit of the stand-in: IN_PROGRESS can't be reached in phase 1, since there's no partial state with one required proof. Agencies lose the ability to say "started" until phase 2. This is accepted for the sake of shipping the fix early.
- Seed data (`V8`) currently hand-writes a spread of statuses. After this change they would all collapse. The seed needs matching attachment rows so the demo still shows every status. Placeholder files bundled with the app are fine. (My call.)
- Migration: existing attachments become UNREVIEWED. Existing statuses get recomputed once on first run.

### Phase 2: categories and required proofs

- Attachments get a category. Starter set, placeholders to refine with the real documents agencies use: PROGRESS_REPORT, SITE_PHOTO, INSPECTION_REPORT, CERTIFICATE_OF_ACCEPTANCE, OTHER.
- Each category defines allowed file types and required fields (issue date, reference number, issuing office where relevant). The upload form shows only that category's fields. Mismatches are rejected at upload, not left for a reviewer.
- A milestone declares what it needs, for example one certificate of acceptance and three site photos. Store as rows of (milestone, category, minimum count).
- The stand-in rule from phase 1 goes away. IN_PROGRESS becomes reachable.
- A milestone that declares no required proofs falls back to the phase 1 default: one proof of any category completes it. Existing milestones keep working without anyone editing them. (My call.)
- Add an info button on the upload form and on the milestone page that explains the steps. Generate its text from the category table, so the guide can't drift from what the code does.

### Phase 3: review

- Review is a vote. Each vote records the proof, the voter, up or down, a reason and a timestamp. A proof's review state (UNREVIEWED, VERIFIED, REJECTED) comes from its votes through one rule in one place:
  - Fewer than `minVotes` votes: UNREVIEWED.
  - Enough votes and upvotes beat downvotes: VERIFIED.
  - Enough votes and downvotes beat upvotes: REJECTED.
  - A tie: UNREVIEWED.
- `minVotes` is one setting, editable by ADMIN. At 1 it behaves as a single reviewer. At 3 or more it behaves as consensus. Both options the user raised are the same system with a different number. It starts at 1 (my call), and the current value is visible to the public.
- Who may vote: ADMIN accounts, new REVIEWER accounts, and AGENCY accounts. A REVIEWER account works like a regular account and an ADMIN grants it by hand. Nobody can self-register as one.
- Who may not vote on a given proof: any account belonging to the agency that owns the project (not only the uploader), and the account that uploaded the proof (this covers an ADMIN who uploaded it). One vote per account per proof.
- A voter can change their vote. The change is audit-logged. The outcome is recalculated after every vote, including late ones. (My call.)
- Votes are public: the public page shows each voter, their vote and their reason. The user chose this for accountability and accepts the pressure it can put on reviewers.
- Review queue page, sorted by suspicion. Starting flags:
  - The same file (by SHA-256 hash, stored at upload) is already attached to another milestone.
  - File type doesn't match the category.
  - Uploaded in the last day before or any time after the due date.
  - Photo GPS far from the project's map pin. A missing GPS tag is not a flag, since many tools strip it. This one can come last.
- Proofs with no flag go to the normal queue, oldest first.
- Deleting or restoring a verified proof recomputes status and is audit-logged. (Soft delete already exists.)

## Constraints from the existing code

- Audit logging happens in `AuditInterceptor` for `/api/**` mutations. Status changes made by the midnight job aren't covered, same as OVERDUE today. The STATUS_CHANGE alert is the record there.
- `GET /api/v1/projects` and project detail are public. Status and the asterisk must be visible to PUBLIC users. Proof review state and rejection reasons are public too.
- Flyway migrations only, never edit an applied one. Next free number is V12.
- The docs rule applies: update `dev-guide.md` and the service `CLAUDE.md` in the same change as each phase.

## Still open for the user

- Nothing that blocks planning. One thing to watch once real data exists: agencies review each other and compete for budgets, so vote trading is possible. Public votes and the audit log are the defence. Raising `minVotes` makes it harder.

## Left to the builder

- Final names for `StatusDeriver` and its methods.
- Exact category list and fields in phase 2, once real agency documents are on the table.
- Queue scoring weights in phase 3.
- Whether the derived asterisk is computed in the response DTO or in a query.
