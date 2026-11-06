package ph.trackph.milestone.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import ph.trackph.milestone.dto.request.MilestoneRequest;
import ph.trackph.milestone.dto.request.ProjectRequest;
import ph.trackph.milestone.dto.response.MilestoneResponse;
import ph.trackph.milestone.dto.response.ProjectResponse;
import ph.trackph.milestone.exception.UnauthorizedException;
import ph.trackph.milestone.model.Attachment;
import ph.trackph.milestone.model.Milestone;
import ph.trackph.milestone.model.Project;
import ph.trackph.milestone.model.User;
import ph.trackph.milestone.repository.AlertRepository;
import ph.trackph.milestone.repository.AttachmentRepository;
import ph.trackph.milestone.scheduler.StatusBackfillRunner;
import ph.trackph.milestone.repository.MilestoneRepository;
import ph.trackph.milestone.repository.ProjectRepository;
import ph.trackph.milestone.repository.UserRepository;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = "spring.profiles.active=dev")
@Transactional
class StatusWiringTest {
    @Autowired ObjectMapper objectMapper;
    @Autowired MilestoneService milestoneService;
    @Autowired AttachmentService attachmentService;
    @Autowired StatusService statusService;
    @Autowired ProjectService projectService;
    @Autowired StatusBackfillRunner backfillRunner;
    @Autowired ph.trackph.milestone.scheduler.AlertScheduler alertScheduler;
    @Autowired AlertRepository alertRepository;
    @Autowired UserRepository userRepository;
    @Autowired ProjectRepository projectRepository;
    @Autowired MilestoneRepository milestoneRepository;
    @Autowired AttachmentRepository attachmentRepository;
    @Autowired EntityManager entityManager;

    @BeforeEach
    void bindRequest() {
        // AuditContext is request-scoped; services that audit-log need a request bound outside MVC.
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
    }

    @AfterEach
    void unbindRequest() {
        RequestContextHolder.resetRequestAttributes();
    }

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

    private Project project(User agency) {
        Project p = new Project();
        p.setName("Test project");
        p.setAgency(agency);
        p.setStartDate(LocalDate.now().minusDays(100));
        p.setEndDate(LocalDate.now().plusDays(100));
        p.setOriginalEndDate(p.getEndDate());
        return projectRepository.save(p);
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

    @Test
    void startupBackfillAlertsForAMissedMidnightOnlyOnce() {
        User agency = agency();
        Project p = project(agency);
        Milestone late = milestone(p, -4, 1);
        long before = alertRepository.count();

        backfillRunner.run(null);
        assertEquals(before + 1, alertRepository.count());
        assertEquals("OVERDUE", MilestoneResponse.from(milestoneRepository.findById(late.getId()).orElseThrow()).status());

        backfillRunner.run(null);
        assertEquals(before + 1, alertRepository.count());
    }

    @Test
    void cancelRecordsReasonAndSticksThroughRefresh() {
        User agency = agency();
        Project p = project(agency);
        Milestone m = milestone(p, 30, 1);
        attach(m, agency);

        ProjectResponse cancelled = projectService.cancel(p.getId(), "Budget reallocated", agency.getId(), false);
        statusService.refreshMilestone(m.getId());
        statusService.refreshProject(p.getId());

        assertEquals("CANCELLED", cancelled.status());
        assertEquals("Budget reallocated", cancelled.cancelReason());
        entityManager.flush();
        entityManager.clear();
        assertEquals(Project.Status.CANCELLED, projectRepository.findById(p.getId()).orElseThrow().getStatus());
    }

    @Test
    void cancelNeedsAReason() {
        User agency = agency();
        Project p = project(agency);

        assertThrows(IllegalArgumentException.class, () -> projectService.cancel(p.getId(), "  ", agency.getId(), false));
    }

    @Test
    void cancelRejectsOtherAgencies() {
        Project p = project(agency());
        User other = agency();

        assertThrows(UnauthorizedException.class, () -> projectService.cancel(p.getId(), "Not mine", other.getId(), false));
    }

    private ProjectRequest request(String name, LocalDate end) {
        return new ProjectRequest(name, null, LocalDate.now().minusDays(100), end, null, null, null, null);
    }

    @Test
    void originalEndDateIsSetOnceAndNeverMoves() {
        User agency = agency();
        LocalDate end = LocalDate.now().plusDays(100);

        ProjectResponse created = projectService.create(agency.getId(), request("P", end));
        assertEquals(end, created.originalEndDate());
        assertFalse(created.endDateExtended());

        ProjectResponse extended = projectService.update(created.id(), request("P", end.plusDays(30)), agency.getId(), false);
        assertEquals(end, extended.originalEndDate());
        assertEquals(end.plusDays(30), extended.endDate());
        assertTrue(extended.endDateExtended());

        ProjectResponse shortened = projectService.update(created.id(), request("P", end.minusDays(5)), agency.getId(), false);
        assertEquals(end, shortened.originalEndDate());
        assertFalse(shortened.endDateExtended());
    }

    @Test
    void endDateAddedLaterBecomesTheOriginal() {
        User agency = agency();
        ProjectResponse created = projectService.create(agency.getId(), request("P", null));
        LocalDate end = LocalDate.now().plusDays(50);

        ProjectResponse updated = projectService.update(created.id(), request("P", end), agency.getId(), false);

        assertEquals(end, updated.originalEndDate());
        assertFalse(updated.endDateExtended());
    }

    @Test
    void extendingTheEndDateClearsDelayed() {
        User agency = agency();
        Project p = project(agency);
        p.setEndDate(LocalDate.now().minusDays(1));
        projectRepository.save(p);
        milestone(p, 5, 1);
        statusService.refreshProject(p.getId());
        assertEquals(Project.Status.DELAYED, projectRepository.findById(p.getId()).orElseThrow().getStatus());

        ProjectResponse updated = projectService.update(p.getId(), request(p.getName(), LocalDate.now().plusDays(30)), agency.getId(), false);

        assertEquals("PLANNED", updated.status());
    }

    @Test
    void uploadWithoutAClaimIsRejected() {
        User agency = agency();
        Project p = project(agency);
        Milestone m = milestone(p, 30, 1);
        MockMultipartFile file = new MockMultipartFile("file", "proof.pdf", "application/pdf", new byte[]{1});

        assertThrows(IllegalArgumentException.class,
                () -> attachmentService.upload(m.getId(), file, null, agency.getId(), false));
        assertEquals(0, attachmentRepository.countByMilestoneId(m.getId()));
    }

    @Test
    void restoringTheOnlyProofDoesNotBringTheClaimBack() {
        User agency = agency();
        Project p = project(agency);
        Milestone m = milestone(p, 30, 1);
        Attachment proof = attach(m, agency);
        statusService.recordClaim(m.getId(), Milestone.Claim.COMPLETED);
        attachmentService.delete(m.getId(), proof.getId(), agency.getId(), false);

        attachmentService.restore(m.getId(), proof.getId());

        entityManager.flush();
        entityManager.clear();
        Milestone after = milestoneRepository.findById(m.getId()).orElseThrow();
        assertEquals(Milestone.Status.PENDING, after.getStatus());
        assertNull(after.getClaim());
    }

    @Test
    void archivedListingShowsWhoAndWhenButNotTheFileName() {
        User agency = agency();
        Project p = project(agency);
        Milestone m = milestone(p, 30, 1);
        Attachment proof = attach(m, agency);
        proof.setOriginalFilename("secret-id-scan.pdf");
        attachmentRepository.save(proof);
        attachmentService.delete(m.getId(), proof.getId(), agency.getId(), false);
        entityManager.flush();

        var rows = attachmentService.findArchivedByMilestone(m.getId());

        assertEquals(1, rows.size());
        assertTrue(rows.get(0).containsKey("uploadedByUsername"));
        assertTrue(rows.get(0).containsKey("deletedAt"));
        assertFalse(rows.get(0).containsKey("originalFilename"));
        assertFalse(rows.get(0).toString().contains("secret-id-scan"));
    }

    @Test
    void auditTrailOfADeletedProofNeverCarriesTheFileName() {
        User agency = agency();
        Project p = project(agency);
        Milestone m = milestone(p, 30, 1);
        Attachment proof = attach(m, agency);
        proof.setOriginalFilename("secret-id-scan.pdf");
        attachmentRepository.save(proof);

        attachmentService.delete(m.getId(), proof.getId(), agency.getId(), false);

        Object details = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                .getRequest().getAttribute("auditDetails");
        assertFalse(String.valueOf(details).contains("secret-id-scan"));
    }

    @Test
    void midnightJobRederivesAProjectOnceItsEndDatePasses() {
        User agency = agency();
        Project p = project(agency);
        Milestone m = milestone(p, -20, 1);
        milestone(p, 5, 2);
        attach(m, agency);
        statusService.recordClaim(m.getId(), Milestone.Claim.COMPLETED);
        // The project was last refreshed while its end date was still ahead, so it was stored as IN_PROGRESS.
        p.setEndDate(LocalDate.now().minusDays(1));
        p.setStatus(Project.Status.IN_PROGRESS);
        projectRepository.save(p);
        entityManager.flush();

        alertScheduler.checkOverdueMilestones();

        entityManager.flush();
        entityManager.clear();
        assertEquals(Project.Status.DELAYED, projectRepository.findById(p.getId()).orElseThrow().getStatus());
    }

    @Test
    void staleClientStatusFieldIsIgnored() {
        String json = "{\"title\":\"t\",\"dueDate\":\"2027-01-01\",\"sequenceOrder\":1,\"status\":\"COMPLETED\",\"completedDate\":\"2026-01-01\"}";
        assertDoesNotThrow(() -> objectMapper.readValue(json, MilestoneRequest.class));
    }

    @Test
    void milestoneCreateStartsPending() {
        User agency = agency();
        Project p = project(agency);

        MilestoneResponse created = milestoneService.create(p.getId(),
                new MilestoneRequest("t", null, LocalDate.now().plusDays(10), 1), agency.getId(), false);

        assertEquals("PENDING", created.status());
        assertFalse(created.needsReview());
    }

    @Test
    void milestoneCreatedPastDueIsOverdueAndProjectDelayed() {
        User agency = agency();
        Project p = project(agency);

        MilestoneResponse created = milestoneService.create(p.getId(),
                new MilestoneRequest("t", null, LocalDate.now().minusDays(1), 1), agency.getId(), false);

        assertEquals("OVERDUE", created.status());
        entityManager.flush();
        entityManager.clear();
        assertEquals(Project.Status.DELAYED, projectRepository.findById(p.getId()).orElseThrow().getStatus());
    }

    @Test
    void attachmentDeleteDropsStatusThroughTheService() {
        User agency = agency();
        Project p = project(agency);
        Milestone m = milestone(p, 30, 1);
        Attachment proof = attach(m, agency);
        statusService.recordClaim(m.getId(), Milestone.Claim.COMPLETED);

        attachmentService.delete(m.getId(), proof.getId(), agency.getId(), false);

        entityManager.flush();
        entityManager.clear();
        assertEquals(Milestone.Status.PENDING, milestoneRepository.findById(m.getId()).orElseThrow().getStatus());
    }

    @Test
    void completedMilestoneResponseCarriesAsterisk() {
        User agency = agency();
        Project p = project(agency);
        Milestone m = milestone(p, 30, 1);
        attach(m, agency);
        statusService.recordClaim(m.getId(), Milestone.Claim.COMPLETED);
        entityManager.flush();
        entityManager.clear();

        assertTrue(MilestoneResponse.from(milestoneRepository.findById(m.getId()).orElseThrow()).needsReview());
        Project reloaded = projectRepository.findById(p.getId()).orElseThrow();
        assertTrue(ProjectResponse.from(reloaded).needsReview());
        assertTrue(ProjectResponse.publicView(reloaded).needsReview());
    }
}
