package ph.trackph.milestone.service;

import ph.trackph.milestone.model.Milestone;
import ph.trackph.milestone.model.Project;

import java.time.LocalDate;
import java.util.List;

/**
 * Every rule that turns facts (proofs, claims, due dates, milestones) into a status lives here, and
 * nowhere else. Pure and static on purpose: no database, no clock, so each rule is easy to test.
 * {@link StatusService} loads the facts and is the only code that writes the result.
 */
public final class StatusDeriver {
    /** What the project rules need to know about one milestone. */
    public record MilestoneFacts(Milestone.Status status, LocalDate dueDate, LocalDate completedDate) {}

    private StatusDeriver() {}

    public static boolean isDone(Milestone.Status status) {
        return status == Milestone.Status.COMPLETED || status == Milestone.Status.COMPLETED_LATE;
    }

    /** A claim only counts while a live proof backs it; without one the milestone is just waiting or late. */
    public static Milestone.Status deriveMilestone(LocalDate dueDate, LocalDate today, boolean hasProof,
                                                   Milestone.Claim claim, LocalDate completedDate) {
        boolean pastDue = dueDate.isBefore(today);
        if (!hasProof || claim == null) return pastDue ? Milestone.Status.OVERDUE : Milestone.Status.PENDING;
        if (claim == Milestone.Claim.COMPLETED) {
            return completedDate != null && completedDate.isAfter(dueDate)
                    ? Milestone.Status.COMPLETED_LATE : Milestone.Status.COMPLETED;
        }
        return pastDue ? Milestone.Status.OVERDUE : Milestone.Status.IN_PROGRESS;
    }

    public static boolean milestoneNeedsReview(Milestone.Status status, int unreviewedProofs) {
        return (status == Milestone.Status.IN_PROGRESS || isDone(status)) && unreviewedProofs > 0;
    }

    /** {@code current} only matters for CANCELLED, the one status a person sets by hand. A null {@code endDate} skips the end-date rules. */
    public static Project.Status deriveProject(Project.Status current, LocalDate endDate, LocalDate today, List<MilestoneFacts> milestones) {
        if (current == Project.Status.CANCELLED) return Project.Status.CANCELLED;
        // The empty check must come first: allMatch on an empty list is true, which would mark a project with no milestones COMPLETED.
        if (milestones.isEmpty()) return Project.Status.PLANNED;

        LocalDate lastDue = milestones.stream().map(MilestoneFacts::dueDate).max(LocalDate::compareTo).orElseThrow();
        LocalDate latestCompleted = milestones.stream().map(MilestoneFacts::completedDate)
                .filter(java.util.Objects::nonNull).max(LocalDate::compareTo).orElse(null);
        boolean planPastEnd = endDate != null && lastDue.isAfter(endDate);

        // While the project is running, the plan must reach the end date, or a project with one early milestone
        // finished would count as finished. Once the end date has passed no milestone can still be added in time,
        // so finishing every milestone you have is finishing the project.
        boolean allDone = milestones.stream().allMatch(m -> isDone(m.status()));
        boolean planReachesEnd = endDate == null || !lastDue.isBefore(endDate) || today.isAfter(endDate);
        if (allDone && planReachesEnd) {
            boolean late = endDate != null && (planPastEnd || (latestCompleted != null && latestCompleted.isAfter(endDate)));
            return late ? Project.Status.COMPLETED_LATE : Project.Status.COMPLETED;
        }
        boolean anyOverdue = milestones.stream().anyMatch(m -> m.status() == Milestone.Status.OVERDUE);
        if (anyOverdue || planPastEnd || (endDate != null && today.isAfter(endDate))) return Project.Status.DELAYED;
        if (milestones.stream().anyMatch(m -> m.status() == Milestone.Status.IN_PROGRESS || isDone(m.status()))) {
            return Project.Status.IN_PROGRESS;
        }
        return Project.Status.PLANNED;
    }

    public static boolean projectNeedsReview(Project.Status status, boolean anyMilestoneNeedsReview) {
        return (status == Project.Status.COMPLETED || status == Project.Status.COMPLETED_LATE || status == Project.Status.IN_PROGRESS)
                && anyMilestoneNeedsReview;
    }
}
