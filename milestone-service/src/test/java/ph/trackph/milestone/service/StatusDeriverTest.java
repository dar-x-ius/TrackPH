package ph.trackph.milestone.service;

import org.junit.jupiter.api.Test;
import ph.trackph.milestone.model.Milestone;
import ph.trackph.milestone.model.Project;
import ph.trackph.milestone.service.StatusDeriver.MilestoneFacts;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ph.trackph.milestone.model.Milestone.Claim.COMPLETED;
import static ph.trackph.milestone.model.Milestone.Status.COMPLETED_LATE;
import static ph.trackph.milestone.model.Milestone.Status.IN_PROGRESS;
import static ph.trackph.milestone.model.Milestone.Status.OVERDUE;
import static ph.trackph.milestone.model.Milestone.Status.PENDING;
import static ph.trackph.milestone.service.StatusDeriver.deriveMilestone;
import static ph.trackph.milestone.service.StatusDeriver.deriveProject;
import static ph.trackph.milestone.service.StatusDeriver.milestoneNeedsReview;
import static ph.trackph.milestone.service.StatusDeriver.projectNeedsReview;

class StatusDeriverTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 6);

    private static LocalDate d(int offsetDays) { return TODAY.plusDays(offsetDays); }

    private static MilestoneFacts f(Milestone.Status status, LocalDate due, LocalDate completed) {
        return new MilestoneFacts(status, due, completed);
    }

    // --- milestone ---

    @Test void noProofPastDueIsOverdue()      { assertEquals(OVERDUE, deriveMilestone(d(-1), TODAY, false, null, null)); }
    @Test void noProofNotDueIsPending()       { assertEquals(PENDING, deriveMilestone(d(5), TODAY, false, null, null)); }
    @Test void proofWithoutClaimIsIgnored()   { assertEquals(PENDING, deriveMilestone(d(5), TODAY, true, null, null)); }

    @Test void completedClaimBeforeDueIsCompleted() {
        assertEquals(Milestone.Status.COMPLETED, deriveMilestone(d(5), TODAY, true, COMPLETED, TODAY));
    }

    @Test void completedClaimOnDueDateIsCompleted() {
        assertEquals(Milestone.Status.COMPLETED, deriveMilestone(TODAY, TODAY, true, COMPLETED, TODAY));
    }

    @Test void completedClaimAfterDueIsCompletedLate() {
        assertEquals(COMPLETED_LATE, deriveMilestone(d(-3), TODAY, true, COMPLETED, TODAY));
    }

    @Test void inProgressClaimBeforeDueIsInProgress() {
        assertEquals(IN_PROGRESS, deriveMilestone(d(5), TODAY, true, Milestone.Claim.IN_PROGRESS, null));
    }

    @Test void inProgressClaimPastDueIsOverdue() {
        assertEquals(OVERDUE, deriveMilestone(d(-1), TODAY, true, Milestone.Claim.IN_PROGRESS, null));
    }

    @Test void milestoneAsteriskRules() {
        assertTrue(milestoneNeedsReview(Milestone.Status.COMPLETED, 1));
        assertTrue(milestoneNeedsReview(COMPLETED_LATE, 1));
        assertTrue(milestoneNeedsReview(IN_PROGRESS, 2));
        assertFalse(milestoneNeedsReview(Milestone.Status.COMPLETED, 0));
        assertFalse(milestoneNeedsReview(OVERDUE, 3));
        assertFalse(milestoneNeedsReview(PENDING, 1));
    }

    // --- project ---

    @Test void cancelledSticks() {
        assertEquals(Project.Status.CANCELLED, deriveProject(Project.Status.CANCELLED, d(10), TODAY,
                List.of(f(Milestone.Status.COMPLETED, d(10), d(-1)))));
    }

    @Test void noMilestonesIsPlanned() {
        assertEquals(Project.Status.PLANNED, deriveProject(Project.Status.COMPLETED, d(10), TODAY, List.of()));
    }

    @Test void oneFinishedMilestoneShortOfEndDateIsInProgress() {
        assertEquals(Project.Status.IN_PROGRESS, deriveProject(Project.Status.PLANNED, d(60), TODAY,
                List.of(f(Milestone.Status.COMPLETED, d(-10), d(-12)))));
    }

    @Test void allDoneEarlyBecomesCompletedOnceTheEndDatePasses() {
        assertEquals(Project.Status.COMPLETED, deriveProject(Project.Status.IN_PROGRESS, d(-1), TODAY,
                List.of(f(Milestone.Status.COMPLETED, d(-10), d(-12)))));
    }

    @Test void allDoneEarlyButFinishedAfterTheEndDateIsCompletedLate() {
        assertEquals(Project.Status.COMPLETED_LATE, deriveProject(Project.Status.IN_PROGRESS, d(-5), TODAY,
                List.of(f(Milestone.Status.COMPLETED, d(-10), d(-1)))));
    }

    @Test void allDoneLastDueOnEndDateIsCompleted() {
        assertEquals(Project.Status.COMPLETED, deriveProject(Project.Status.PLANNED, d(10), TODAY,
                List.of(f(Milestone.Status.COMPLETED, d(-5), d(-6)), f(Milestone.Status.COMPLETED, d(10), d(0)))));
    }

    @Test void allDoneButFinishedAfterEndDateIsCompletedLate() {
        assertEquals(Project.Status.COMPLETED_LATE, deriveProject(Project.Status.PLANNED, d(-2), TODAY,
                List.of(f(COMPLETED_LATE, d(-2), TODAY))));
    }

    @Test void milestoneDueAfterEndDateMakesCompletionLate() {
        assertEquals(Project.Status.COMPLETED_LATE, deriveProject(Project.Status.PLANNED, d(5), TODAY,
                List.of(f(Milestone.Status.COMPLETED, d(20), TODAY))));
    }

    @Test void completedProjectNeverShowsDelayedEvenPastEndDate() {
        assertEquals(Project.Status.COMPLETED_LATE, deriveProject(Project.Status.PLANNED, d(-30), TODAY,
                List.of(f(Milestone.Status.COMPLETED, d(-30), d(-1)))));
    }

    @Test void anyOverdueIsDelayed() {
        assertEquals(Project.Status.DELAYED, deriveProject(Project.Status.PLANNED, d(60), TODAY,
                List.of(f(Milestone.Status.COMPLETED, d(-10), d(-12)), f(OVERDUE, d(-1), null), f(PENDING, d(60), null))));
    }

    @Test void pastEndDateAndNotDoneIsDelayed() {
        assertEquals(Project.Status.DELAYED, deriveProject(Project.Status.PLANNED, d(-1), TODAY,
                List.of(f(PENDING, d(-1), null))));
    }

    @Test void milestoneDueAfterEndDateIsDelayedNow() {
        assertEquals(Project.Status.DELAYED, deriveProject(Project.Status.PLANNED, d(30), TODAY,
                List.of(f(PENDING, d(40), null))));
    }

    @Test void recoveredProjectIsInProgress() {
        assertEquals(Project.Status.IN_PROGRESS, deriveProject(Project.Status.DELAYED, d(30), TODAY,
                List.of(f(COMPLETED_LATE, d(-5), TODAY), f(PENDING, d(30), null))));
    }

    @Test void startedIsInProgress() {
        assertEquals(Project.Status.IN_PROGRESS, deriveProject(Project.Status.PLANNED, d(30), TODAY,
                List.of(f(IN_PROGRESS, d(10), null), f(PENDING, d(30), null))));
    }

    @Test void nothingStartedIsPlanned() {
        assertEquals(Project.Status.PLANNED, deriveProject(Project.Status.IN_PROGRESS, d(30), TODAY,
                List.of(f(PENDING, d(10), null), f(PENDING, d(30), null))));
    }

    @Test void nullEndDateIgnoresEndDateRules() {
        assertEquals(Project.Status.COMPLETED, deriveProject(Project.Status.PLANNED, null, TODAY,
                List.of(f(Milestone.Status.COMPLETED, d(-5), d(-6)))));
        assertEquals(Project.Status.PLANNED, deriveProject(Project.Status.PLANNED, null, TODAY,
                List.of(f(PENDING, d(5), null))));
    }

    @Test void projectAsteriskRules() {
        assertTrue(projectNeedsReview(Project.Status.COMPLETED, true));
        assertTrue(projectNeedsReview(Project.Status.COMPLETED_LATE, true));
        assertTrue(projectNeedsReview(Project.Status.IN_PROGRESS, true));
        assertFalse(projectNeedsReview(Project.Status.DELAYED, true));
        assertFalse(projectNeedsReview(Project.Status.PLANNED, true));
        assertFalse(projectNeedsReview(Project.Status.CANCELLED, true));
        assertFalse(projectNeedsReview(Project.Status.COMPLETED, false));
    }
}
