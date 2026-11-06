package ph.trackph.milestone.service;

import org.junit.jupiter.api.Test;
import ph.trackph.milestone.model.Project;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ph.trackph.milestone.model.Milestone.Status.COMPLETED;
import static ph.trackph.milestone.model.Milestone.Status.IN_PROGRESS;
import static ph.trackph.milestone.model.Milestone.Status.OVERDUE;
import static ph.trackph.milestone.model.Milestone.Status.PENDING;
import static ph.trackph.milestone.service.StatusDeriver.deriveMilestone;
import static ph.trackph.milestone.service.StatusDeriver.deriveProject;
import static ph.trackph.milestone.service.StatusDeriver.milestoneNeedsReview;
import static ph.trackph.milestone.service.StatusDeriver.projectNeedsReview;

class StatusDeriverTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 6);

    @Test void enoughProofsIsCompleted()         { assertEquals(COMPLETED, deriveMilestone(TODAY.plusDays(5), TODAY, 1, 1)); }
    @Test void completedWinsEvenWhenPastDue()    { assertEquals(COMPLETED, deriveMilestone(TODAY.minusDays(30), TODAY, 1, 1)); }
    @Test void pastDueWithoutProofsIsOverdue()   { assertEquals(OVERDUE, deriveMilestone(TODAY.minusDays(1), TODAY, 0, 1)); }
    @Test void dueTodayIsNotYetOverdue()         { assertEquals(PENDING, deriveMilestone(TODAY, TODAY, 0, 1)); }
    @Test void someButNotAllProofsIsInProgress() { assertEquals(IN_PROGRESS, deriveMilestone(TODAY.plusDays(5), TODAY, 1, 3)); }
    @Test void partialProofsPastDueIsOverdue()   { assertEquals(OVERDUE, deriveMilestone(TODAY.minusDays(1), TODAY, 1, 3)); }
    @Test void noProofsNotDueIsPending()         { assertEquals(PENDING, deriveMilestone(TODAY.plusDays(5), TODAY, 0, 1)); }
    @Test void zeroRequiredStillNeedsOneProof()  { assertEquals(PENDING, deriveMilestone(TODAY.plusDays(5), TODAY, 0, 0)); }

    @Test void milestoneAsteriskRules() {
        assertTrue(milestoneNeedsReview(COMPLETED, 1));
        assertTrue(milestoneNeedsReview(IN_PROGRESS, 2));
        assertFalse(milestoneNeedsReview(COMPLETED, 0));
        assertFalse(milestoneNeedsReview(OVERDUE, 3));
        assertFalse(milestoneNeedsReview(PENDING, 1));
    }

    @Test void noMilestonesIsPlanned()           { assertEquals(Project.Status.PLANNED, deriveProject(Project.Status.COMPLETED, List.of())); }
    @Test void allCompletedIsCompleted()         { assertEquals(Project.Status.COMPLETED, deriveProject(Project.Status.PLANNED, List.of(COMPLETED, COMPLETED))); }
    @Test void anyOverdueIsDelayed()             { assertEquals(Project.Status.DELAYED, deriveProject(Project.Status.PLANNED, List.of(COMPLETED, OVERDUE, PENDING))); }

    @Test void startedIsInProgress() {
        assertEquals(Project.Status.IN_PROGRESS, deriveProject(Project.Status.PLANNED, List.of(COMPLETED, PENDING)));
        assertEquals(Project.Status.IN_PROGRESS, deriveProject(Project.Status.PLANNED, List.of(IN_PROGRESS, PENDING)));
    }

    @Test void nothingStartedIsPlanned()         { assertEquals(Project.Status.PLANNED, deriveProject(Project.Status.IN_PROGRESS, List.of(PENDING, PENDING))); }
    @Test void cancelledSticks()                 { assertEquals(Project.Status.CANCELLED, deriveProject(Project.Status.CANCELLED, List.of(COMPLETED, COMPLETED))); }

    @Test void projectAsteriskRules() {
        assertTrue(projectNeedsReview(Project.Status.COMPLETED, true));
        assertTrue(projectNeedsReview(Project.Status.IN_PROGRESS, true));
        assertFalse(projectNeedsReview(Project.Status.DELAYED, true));
        assertFalse(projectNeedsReview(Project.Status.CANCELLED, true));
        assertFalse(projectNeedsReview(Project.Status.COMPLETED, false));
    }
}
