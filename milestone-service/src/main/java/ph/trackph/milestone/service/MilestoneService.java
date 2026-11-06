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
import java.util.List;

@Service
public class MilestoneService {
    private final MilestoneRepository milestoneRepository;
    private final ProjectRepository projectRepository;
    private final AlertService alertService;
    private final AuditContext auditContext;
    private final StatusService statusService;

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
        Milestone saved = milestoneRepository.save(milestone);
        statusService.refreshMilestone(saved.getId());
        MilestoneResponse created = MilestoneResponse.from(saved);
        auditContext.recordChange("Milestone", created.id(), null, created);
        return created;
    }

    @Transactional
    public MilestoneResponse update(Long id, MilestoneRequest request, Long requesterId, boolean requesterIsAdmin) {
        Milestone milestone = getOrThrow(id);
        requireOwnerOrAdmin(milestone.getProject(), requesterId, requesterIsAdmin);
        MilestoneResponse before = MilestoneResponse.from(milestone);
        applyRequest(milestone, request);
        Milestone saved = milestoneRepository.save(milestone);
        statusService.refreshMilestone(saved.getId());
        MilestoneResponse after = MilestoneResponse.from(saved);
        auditContext.recordChange("Milestone", id, before, after);
        return after;
    }

    /**
     * Called from the "Submit Report" flow — marks progress and flags delays.
     */
    @Transactional
    public MilestoneResponse submitReport(Long id, MilestoneRequest request, Long requesterId, boolean requesterIsAdmin) {
        Milestone milestone = getOrThrow(id);
        requireOwnerOrAdmin(milestone.getProject(), requesterId, requesterIsAdmin);
        MilestoneResponse before = MilestoneResponse.from(milestone);
        applyRequest(milestone, request);
        alertService.createAlert(milestone.getProject(), milestone, Alert.AlertType.REPORT_SUBMITTED, "Progress report submitted for milestone \'" + milestone.getTitle() + "\'", Alert.Severity.LOW);
        Milestone saved = milestoneRepository.save(milestone);
        statusService.refreshMilestone(saved.getId());
        MilestoneResponse after = MilestoneResponse.from(saved);
        auditContext.recordChange("Milestone", id, before, after);
        return after;
    }

    private void applyRequest(Milestone milestone, MilestoneRequest req) {
        milestone.setTitle(req.title());
        if (req.description() != null) milestone.setDescription(req.description());
        milestone.setDueDate(req.dueDate());
        milestone.setSequenceOrder(req.sequenceOrder());
    }

    private void requireOwnerOrAdmin(Project project, Long requesterId, boolean requesterIsAdmin) {
        if (!requesterIsAdmin && !project.getAgency().getId().equals(requesterId)) {
            throw new UnauthorizedException("Only the owning agency or an admin can modify this project's milestones");
        }
    }

    private Milestone getOrThrow(Long id) {
        return milestoneRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Milestone", id));
    }

    public MilestoneService(final MilestoneRepository milestoneRepository, final ProjectRepository projectRepository, final AlertService alertService, final AuditContext auditContext, final StatusService statusService) {
        this.milestoneRepository = milestoneRepository;
        this.projectRepository = projectRepository;
        this.alertService = alertService;
        this.auditContext = auditContext;
        this.statusService = statusService;
    }
}
