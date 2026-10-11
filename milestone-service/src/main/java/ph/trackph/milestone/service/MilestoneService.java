package ph.trackph.milestone.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ph.trackph.milestone.dto.request.MilestoneRequest;
import ph.trackph.milestone.dto.response.MilestoneResponse;
import ph.trackph.milestone.exception.ResourceNotFoundException;
import ph.trackph.milestone.exception.UnauthorizedException;
import ph.trackph.milestone.model.Alert;
import ph.trackph.milestone.model.Milestone;
import ph.trackph.milestone.model.Project;
import ph.trackph.milestone.repository.MilestoneRepository;
import ph.trackph.milestone.repository.ProjectRepository;
import ph.trackph.milestone.util.AuditContext;
import java.time.LocalDate;
import java.util.List;

@Service
public class MilestoneService {
    private final MilestoneRepository milestoneRepository;
    private final ProjectRepository projectRepository;
    private final AlertService alertService;
    private final AuditContext auditContext;

    public List<MilestoneResponse> findByProject(Long projectId) {
        return milestoneRepository.findByProjectIdOrderBySequenceOrderAsc(projectId).stream().map(MilestoneResponse::from).toList();
    }

    public MilestoneResponse findById(Long id) {
        return MilestoneResponse.from(getOrThrow(id));
    }

    @Transactional
    public MilestoneResponse create(Long projectId, MilestoneRequest request, Long requesterId, boolean requesterIsAdmin) {
        Project project = projectRepository.findById(projectId).orElseThrow(() -> new ResourceNotFoundException("Project", projectId));
        requireOwnerOrAdmin(project, requesterId, requesterIsAdmin);
        Milestone milestone = new Milestone();
        milestone.setProject(project);
        applyRequest(milestone, request);
        MilestoneResponse created = MilestoneResponse.from(milestoneRepository.save(milestone));
        auditContext.recordChange("Milestone", created.id(), null, created);
        return created;
    }

    @Transactional
    public MilestoneResponse update(Long id, MilestoneRequest request, Long requesterId, boolean requesterIsAdmin) {
        Milestone milestone = getOrThrow(id);
        requireOwnerOrAdmin(milestone.getProject(), requesterId, requesterIsAdmin);
        MilestoneResponse before = MilestoneResponse.from(milestone);
        Milestone.Status previousStatus = milestone.getStatus();
        applyRequest(milestone, request);
        if (request.status() != null && request.status() != previousStatus) {
            alertService.createAlert(milestone.getProject(), milestone, Alert.AlertType.STATUS_CHANGE, "Milestone \'" + milestone.getTitle() + "\' changed to " + milestone.getStatus(), Alert.Severity.LOW);
        }
        MilestoneResponse after = MilestoneResponse.from(milestoneRepository.save(milestone));
        auditContext.recordChange("Milestone", id, before, after);
        return after;
    }

    /**
     * Called from the "Submit Report" flow — marks progress and flags delays.
     */
    @Transactional
    public MilestoneResponse submitReport(Long id, MilestoneRequest request) {
        Milestone milestone = getOrThrow(id);
        MilestoneResponse before = MilestoneResponse.from(milestone);
        applyRequest(milestone, request);
        if (milestone.getStatus() == Milestone.Status.COMPLETED) {
            milestone.setCompletedDate(LocalDate.now());
        }
        alertService.createAlert(milestone.getProject(), milestone, Alert.AlertType.REPORT_SUBMITTED, "Progress report submitted for milestone \'" + milestone.getTitle() + "\'", Alert.Severity.LOW);
        MilestoneResponse after = MilestoneResponse.from(milestoneRepository.save(milestone));
        auditContext.recordChange("Milestone", id, before, after);
        return after;
    }

    private void applyRequest(Milestone milestone, MilestoneRequest req) {
        milestone.setTitle(req.title());
        if (req.description() != null) milestone.setDescription(req.description());
        milestone.setDueDate(req.dueDate());
        milestone.setSequenceOrder(req.sequenceOrder());
        if (req.status() != null) milestone.setStatus(req.status());
        if (req.completedDate() != null) milestone.setCompletedDate(req.completedDate());
    }

    private void requireOwnerOrAdmin(Project project, Long requesterId, boolean requesterIsAdmin) {
        if (!requesterIsAdmin && !project.getAgency().getId().equals(requesterId)) {
            throw new UnauthorizedException("Only the owning agency or an admin can modify this project's milestones");
        }
    }

    private Milestone getOrThrow(Long id) {
        return milestoneRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Milestone", id));
    }

    public MilestoneService(final MilestoneRepository milestoneRepository, final ProjectRepository projectRepository, final AlertService alertService, final AuditContext auditContext) {
        this.milestoneRepository = milestoneRepository;
        this.projectRepository = projectRepository;
        this.alertService = alertService;
        this.auditContext = auditContext;
    }
}
