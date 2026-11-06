package ph.trackph.milestone.service;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import ph.trackph.milestone.model.Alert;
import ph.trackph.milestone.model.Attachment;
import ph.trackph.milestone.model.Milestone;
import ph.trackph.milestone.model.Project;
import ph.trackph.milestone.model.User;
import ph.trackph.milestone.repository.AlertRepository;
import ph.trackph.milestone.repository.AttachmentRepository;
import ph.trackph.milestone.repository.MilestoneRepository;
import ph.trackph.milestone.repository.ProjectRepository;
import ph.trackph.milestone.repository.UserRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

@SpringBootTest(properties = "spring.profiles.active=dev")
@Transactional
class StatusServiceTest {
    @Autowired StatusService statusService;
    @Autowired UserRepository userRepository;
    @Autowired ProjectRepository projectRepository;
    @Autowired MilestoneRepository milestoneRepository;
    @Autowired AttachmentRepository attachmentRepository;
    @Autowired AlertRepository alertRepository;
    @Autowired EntityManager entityManager;

    private User agency() {
        User u = new User();
        String unique = UUID.randomUUID().toString().substring(0, 8);
        u.setUsername("agency-" + unique);
        u.setEmail(unique + "@example.test");
        u.setPasswordHash("not-a-real-hash");
        u.setRole(User.Role.AGENCY);
        u.setFullName("Test Agency");
        return userRepository.save(u);
    }

    private Project project(User agency, int endInDays) {
        Project p = new Project();
        p.setName("Test project");
        p.setAgency(agency);
        p.setStartDate(LocalDate.now().minusDays(100));
        p.setEndDate(LocalDate.now().plusDays(endInDays));
        p.setOriginalEndDate(p.getEndDate());
        return projectRepository.save(p);
    }

    private Project project(User agency) {
        return project(agency, 100);
    }

    private Milestone milestone(Project p, int dueInDays, int order) {
        Milestone m = new Milestone();
        m.setProject(p);
        m.setTitle("Milestone " + order);
        m.setDueDate(LocalDate.now().plusDays(dueInDays));
        m.setSequenceOrder(order);
        return milestoneRepository.save(m);
    }

    private Attachment attach(Milestone m, User uploader) {
        Attachment a = new Attachment();
        a.setMilestone(m);
        a.setUploadedBy(uploader);
        a.setOriginalFilename("proof.pdf");
        a.setStoredFilename(UUID.randomUUID() + ".pdf");
        a.setContentType("application/pdf");
        a.setFileSize(10);
        return attachmentRepository.save(a);
    }

    private void claim(Milestone m, Milestone.Claim claim) {
        statusService.recordClaim(m.getId(), claim);
    }

    private Milestone reload(Milestone m) {
        entityManager.flush();
        entityManager.clear();
        return milestoneRepository.findById(m.getId()).orElseThrow();
    }

    private Project reload(Project p) {
        entityManager.flush();
        entityManager.clear();
        return projectRepository.findById(p.getId()).orElseThrow();
    }

    @Test
    void completedClaimCompletesMilestonesAndFinishesProject() {
        User agency = agency();
        Project p = project(agency, 60);
        Milestone m1 = milestone(p, 30, 1);
        Milestone m2 = milestone(p, 60, 2);

        attach(m1, agency);
        claim(m1, Milestone.Claim.COMPLETED);

        Milestone first = reload(m1);
        assertEquals(Milestone.Status.COMPLETED, first.getStatus());
        assertEquals(LocalDate.now(), first.getCompletedDate());
        assertEquals(Project.Status.IN_PROGRESS, reload(p).getStatus());

        attach(m2, agency);
        claim(m2, Milestone.Claim.COMPLETED);

        assertEquals(Project.Status.COMPLETED, reload(p).getStatus());
    }

    @Test
    void inProgressClaimShowsInProgress() {
        User agency = agency();
        Project p = project(agency);
        Milestone m = milestone(p, 30, 1);

        attach(m, agency);
        claim(m, Milestone.Claim.IN_PROGRESS);

        assertEquals(Milestone.Status.IN_PROGRESS, reload(m).getStatus());
        assertNull(milestoneRepository.findById(m.getId()).orElseThrow().getCompletedDate());
        assertEquals(Project.Status.IN_PROGRESS, reload(p).getStatus());
    }

    @Test
    void pastDueMilestoneWithoutProofIsOverdueAndProjectDelayed() {
        Project p = project(agency());
        Milestone m = milestone(p, -3, 1);

        statusService.refreshMilestone(m.getId());

        Milestone after = reload(m);
        assertEquals(Milestone.Status.OVERDUE, after.getStatus());
        assertEquals(3, after.getDelayDays());
        assertEquals(Project.Status.DELAYED, reload(p).getStatus());
    }

    @Test
    void completedClaimAfterTheDueDateIsCompletedLate() {
        User agency = agency();
        Project p = project(agency);
        Milestone m = milestone(p, -3, 1);

        attach(m, agency);
        claim(m, Milestone.Claim.COMPLETED);

        Milestone after = reload(m);
        assertEquals(Milestone.Status.COMPLETED_LATE, after.getStatus());
        assertEquals(LocalDate.now(), after.getCompletedDate());
    }

    @Test
    void completedLateIsNotFlaggedAgainByTheMidnightJob() {
        User agency = agency();
        Project p = project(agency);
        Milestone m = milestone(p, -3, 1);
        attach(m, agency);
        claim(m, Milestone.Claim.COMPLETED);
        entityManager.flush();

        boolean flagged = milestoneRepository.findNewlyOverdue(LocalDate.now()).stream()
                .anyMatch(found -> found.getId().equals(m.getId()));

        assertFalse(flagged);
    }

    @Test
    void softDeletingTheOnlyProofClearsClaimAndCompletedDate() {
        User agency = agency();
        Project p = project(agency);
        Milestone m = milestone(p, 30, 1);
        Attachment proof = attach(m, agency);
        claim(m, Milestone.Claim.COMPLETED);
        assertEquals(Milestone.Status.COMPLETED, reload(m).getStatus());

        Attachment managed = attachmentRepository.findById(proof.getId()).orElseThrow();
        managed.setDeletedAt(LocalDateTime.now());
        attachmentRepository.save(managed);
        statusService.refreshMilestone(m.getId());

        Milestone after = reload(m);
        assertEquals(Milestone.Status.PENDING, after.getStatus());
        assertNull(after.getClaim());
        assertNull(after.getCompletedDate());

        attach(after, agency);
        claim(after, Milestone.Claim.COMPLETED);
        assertEquals(LocalDate.now(), reload(m).getCompletedDate());
    }

    @Test
    void proofWithoutClaimStaysPending() {
        User agency = agency();
        Project p = project(agency);
        Milestone m = milestone(p, 30, 1);
        attach(m, agency);

        statusService.refreshMilestone(m.getId());

        assertEquals(Milestone.Status.PENDING, reload(m).getStatus());
    }

    @Test
    void cancelledProjectStaysCancelled() {
        User agency = agency();
        Project p = project(agency);
        Milestone m = milestone(p, 30, 1);
        attach(m, agency);
        p.setStatus(Project.Status.CANCELLED);
        projectRepository.save(p);

        claim(m, Milestone.Claim.COMPLETED);
        statusService.refreshProject(p.getId());

        assertEquals(Milestone.Status.COMPLETED, reload(m).getStatus());
        assertEquals(Project.Status.CANCELLED, reload(p).getStatus());
    }

    @Test
    void projectWithNoEndDateStillDerives() {
        User agency = agency();
        Project p = project(agency);
        p.setEndDate(null);
        projectRepository.save(p);
        Milestone m = milestone(p, 30, 1);
        attach(m, agency);

        claim(m, Milestone.Claim.COMPLETED);

        assertEquals(Project.Status.COMPLETED, reload(p).getStatus());
    }

    @Test
    void refreshAllQuietlyIsIdempotentAndSilent() {
        User agency = agency();
        Project p = project(agency);
        Milestone done = milestone(p, 30, 1);
        Milestone late = milestone(p, -5, 2);
        attach(done, agency);
        claim(done, Milestone.Claim.COMPLETED);
        entityManager.flush();
        long alertsBefore = alertRepository.count();

        statusService.refreshAllQuietly();
        Milestone.Status doneFirst = reload(done).getStatus();
        Milestone.Status lateFirst = reload(late).getStatus();
        Project.Status projectFirst = reload(p).getStatus();
        statusService.refreshAllQuietly();

        assertEquals(Milestone.Status.COMPLETED, doneFirst);
        assertEquals(Milestone.Status.OVERDUE, lateFirst);
        assertEquals(Project.Status.DELAYED, projectFirst);
        assertEquals(doneFirst, reload(done).getStatus());
        assertEquals(lateFirst, reload(late).getStatus());
        assertEquals(projectFirst, reload(p).getStatus());
        assertEquals(alertsBefore, alertRepository.count());
    }

    @Test
    void completionRaisesStatusChangeAndOverdueRaisesDeadlineMissed() {
        User agency = agency();
        Project p = project(agency);
        Milestone done = milestone(p, 30, 1);
        Milestone late = milestone(p, -2, 2);
        attach(done, agency);
        long before = alertRepository.count();

        claim(done, Milestone.Claim.COMPLETED);
        assertEquals(before + 1, alertRepository.count());
        assertEquals(Alert.AlertType.STATUS_CHANGE, alertFor(done).getType());

        // Going overdue by any route (edit, delete, restore), not only the midnight job, must still warn once.
        statusService.refreshMilestone(late.getId());
        assertEquals(before + 2, alertRepository.count());
        assertEquals(Alert.AlertType.DEADLINE_MISSED, alertFor(late).getType());
    }

    private Alert alertFor(Milestone m) {
        return alertRepository.findAll().stream()
                .filter(a -> a.getMilestone() != null && a.getMilestone().getId().equals(m.getId()))
                .findFirst().orElseThrow();
    }
}
