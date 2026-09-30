package ph.trackph.milestone.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ph.trackph.milestone.dto.request.MilestoneRequest;
import ph.trackph.milestone.dto.response.MilestoneResponse;
import ph.trackph.milestone.exception.ResourceNotFoundException;
import ph.trackph.milestone.model.Alert;
import ph.trackph.milestone.model.Milestone;
import ph.trackph.milestone.model.Project;
import ph.trackph.milestone.repository.MilestoneRepository;
import ph.trackph.milestone.repository.ProjectRepository;
import java.time.LocalDate;
import java.util.List;

@Service
public class MilestoneService {
    private final MilestoneRepository milestoneRepository;
    private final ProjectRepository projectRepository;
    private final AlertService alertService;

    public List<MilestoneResponse> findByProject(Long projectId) {
        return milestoneRepository.findByProjectIdOrderBySequenceOrderAsc(projectId).stream().map(MilestoneResponse::from).toList();
    }

    public MilestoneResponse findById(Long id) {
        return MilestoneResponse.from(getOrThrow(id));
    }

    @Transactional
    public MilestoneResponse create(Long projectId, MilestoneRequest request) {
        Project project = projectRepository.findById(projectId).orElseThrow(() -> new ResourceNotFoundException("Project", projectId));
        Milestone milestone = new Milestone();
        milestone.setProject(project);
        applyRequest(milestone, request);
        return MilestoneResponse.from(milestoneRepository.save(milestone));
    }

    @Transactional
    public MilestoneResponse update(Long id, MilestoneRequest request) {
        Milestone milestone = getOrThrow(id);
        Milestone.Status previousStatus = milestone.getStatus();
        applyRequest(milestone, request);
        if (request.status() != null && request.status() != previousStatus) {
            alertService.createAlert(milestone.getProject(), milestone, Alert.AlertType.STATUS_CHANGE, "Milestone \'" + milestone.getTitle() + "\' changed to " + milestone.getStatus(), Alert.Severity.LOW);
        }
        return MilestoneResponse.from(milestoneRepository.save(milestone));
    }

    /**
     * Called from the "Submit Report" flow — marks progress and flags delays.
     */
    @Transactional
    public MilestoneResponse submitReport(Long id, MilestoneRequest request) {
        Milestone milestone = getOrThrow(id);
        applyRequest(milestone, request);
        if (milestone.getStatus() == Milestone.Status.COMPLETED) {
            milestone.setCompletedDate(LocalDate.now());
        }
        alertService.createAlert(milestone.getProject(), milestone, Alert.AlertType.REPORT_SUBMITTED, "Progress report submitted for milestone \'" + milestone.getTitle() + "\'", Alert.Severity.LOW);
        return MilestoneResponse.from(milestoneRepository.save(milestone));
    }

    private void applyRequest(Milestone milestone, MilestoneRequest req) {
        milestone.setTitle(req.title());
        if (req.description() != null) milestone.setDescription(req.description());
        milestone.setDueDate(req.dueDate());
        milestone.setSequenceOrder(req.sequenceOrder());
        if (req.status() != null) milestone.setStatus(req.status());
        if (req.completedDate() != null) milestone.setCompletedDate(req.completedDate());
    }

    private Milestone getOrThrow(Long id) {
        return milestoneRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Milestone", id));
    }

    public MilestoneService(final MilestoneRepository milestoneRepository, final ProjectRepository projectRepository, final AlertService alertService) {
        this.milestoneRepository = milestoneRepository;
        this.projectRepository = projectRepository;
        this.alertService = alertService;
    }
}
