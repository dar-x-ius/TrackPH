package ph.trackph.milestone.service;

import ph.trackph.milestone.model.Milestone;
import ph.trackph.milestone.model.Project;

import java.time.LocalDate;
import java.util.List;

/**
 * Every rule that turns facts (proofs, due dates, milestones) into a status lives here, and
 * nowhere else. Pure and static on purpose: no database, no clock, so each rule is easy to test.
 * {@link StatusService} loads the facts and is the only code that writes the result.
 */
public final class StatusDeriver {
    /** Phase 1 stand-in: until milestones declare their own required proofs, one proof completes one. */
    public static final int DEFAULT_REQUIRED_PROOFS = 1;

    private StatusDeriver() {}

    /** First match wins: COMPLETED, then OVERDUE, then IN_PROGRESS, then PENDING. */
    public static Milestone.Status deriveMilestone(LocalDate dueDate, LocalDate today, int countingProofs, int requiredProofs) {
        int required = Math.max(1, requiredProofs);
        if (countingProofs >= required) return Milestone.Status.COMPLETED;
        if (dueDate.isBefore(today)) return Milestone.Status.OVERDUE;
        if (countingProofs > 0) return Milestone.Status.IN_PROGRESS;
        return Milestone.Status.PENDING;
    }

    public static boolean milestoneNeedsReview(Milestone.Status status, int unreviewedProofs) {
        return (status == Milestone.Status.COMPLETED || status == Milestone.Status.IN_PROGRESS) && unreviewedProofs > 0;
    }

    /** {@code current} only matters for CANCELLED, the one status a person sets by hand. */
    public static Project.Status deriveProject(Project.Status current, List<Milestone.Status> milestoneStatuses) {
        if (current == Project.Status.CANCELLED) return Project.Status.CANCELLED;
        // The empty check must come first: allMatch on an empty list is true, which would mark a project with no milestones COMPLETED.
        if (milestoneStatuses.isEmpty()) return Project.Status.PLANNED;
        if (milestoneStatuses.stream().allMatch(s -> s == Milestone.Status.COMPLETED)) return Project.Status.COMPLETED;
        if (milestoneStatuses.contains(Milestone.Status.OVERDUE)) return Project.Status.DELAYED;
        if (milestoneStatuses.contains(Milestone.Status.IN_PROGRESS) || milestoneStatuses.contains(Milestone.Status.COMPLETED)) {
            return Project.Status.IN_PROGRESS;
        }
        return Project.Status.PLANNED;
    }

    public static boolean projectNeedsReview(Project.Status status, boolean anyMilestoneNeedsReview) {
        return (status == Project.Status.COMPLETED || status == Project.Status.IN_PROGRESS) && anyMilestoneNeedsReview;
    }
}
