package ph.trackph.milestone.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import ph.trackph.milestone.dto.request.MilestoneRequest;
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
        statusService.refreshMilestone(m.getId());

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
        statusService.refreshMilestone(m.getId());
        entityManager.flush();
        entityManager.clear();

        assertTrue(MilestoneResponse.from(milestoneRepository.findById(m.getId()).orElseThrow()).needsReview());
        Project reloaded = projectRepository.findById(p.getId()).orElseThrow();
        assertTrue(ProjectResponse.from(reloaded).needsReview());
        assertTrue(ProjectResponse.publicView(reloaded).needsReview());
    }
}
