package ph.trackph.milestone.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ph.trackph.milestone.exception.ResourceNotFoundException;
import ph.trackph.milestone.model.Alert;
import ph.trackph.milestone.model.Milestone;
import ph.trackph.milestone.model.Project;
import ph.trackph.milestone.repository.AttachmentRepository;
import ph.trackph.milestone.repository.MilestoneRepository;
import ph.trackph.milestone.repository.ProjectRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * The only writer of {@code status} (and {@code completedDate}) on milestones and projects.
 * It loads the facts, asks {@link StatusDeriver} what the status should be, and saves the answer.
 * CANCELLED is the one status a person sets, in {@code ProjectService.cancel}; the rules here leave it alone.
 */
@Service
public class StatusService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(StatusService.class);
    private final MilestoneRepository milestoneRepository;
    private final ProjectRepository projectRepository;
    private final AttachmentRepository attachmentRepository;
    private final AlertService alertService;

    /** Recomputes one milestone, then the project it belongs to. */
    @Transactional
    public void refreshMilestone(Long milestoneId) {
        Milestone milestone = milestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new ResourceNotFoundException("Milestone", milestoneId));
        refresh(milestone, true);
        refreshProject(milestone.getProject().getId());
    }

    /** Called when a proof is uploaded: the agency says what state the milestone is in, and the proof backs it. */
    @Transactional
    public void recordClaim(Long milestoneId, Milestone.Claim claim) {
        Milestone milestone = milestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new ResourceNotFoundException("Milestone", milestoneId));
        milestone.setClaim(claim);
        milestoneRepository.save(milestone);
        refreshMilestone(milestoneId);
    }

    @Transactional
    public void refreshProject(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));
        refresh(project);
    }

    /**
     * Startup catch-up for existing data. Raises no alerts, so it is safe to run on every boot; each change is logged
     * instead, since nothing else records it. Run the midnight check first (see StatusBackfillRunner) so a missed
     * deadline still alerts.
     */
    @Transactional
    public void refreshAllQuietly() {
        for (Project project : projectRepository.findAll()) {
            for (Milestone milestone : milestoneRepository.findByProjectIdOrderBySequenceOrderAsc(project.getId())) {
                refresh(milestone, false);
            }
            refresh(project);
        }
    }

    private void refresh(Milestone milestone, boolean alert) {
        boolean hasProof = attachmentRepository.countByMilestoneId(milestone.getId()) > 0;
        // A claim only lives as long as a proof backs it. Deleting the last proof withdraws it.
        Milestone.Claim claim = hasProof ? milestone.getClaim() : null;
        LocalDate completedDate = milestone.getCompletedDate();
        if (claim != Milestone.Claim.COMPLETED) {
            completedDate = null;
        } else if (completedDate == null) {
            completedDate = LocalDate.now();
        }
        Milestone.Status derived = StatusDeriver.deriveMilestone(milestone.getDueDate(), LocalDate.now(), hasProof, claim, completedDate);

        boolean statusChanged = derived != milestone.getStatus();
        if (!statusChanged && claim == milestone.getClaim() && Objects.equals(completedDate, milestone.getCompletedDate())) return;

        milestone.setStatus(derived);
        milestone.setClaim(claim);
        milestone.setCompletedDate(completedDate);
        milestoneRepository.save(milestone);
        if (!statusChanged) return;

        if (!alert) {
            log.info("Status catch-up: milestone {} is now {}", milestone.getId(), derived);
        } else if (derived == Milestone.Status.OVERDUE) {
            // Raised here rather than only in the midnight job: an edit, a deleted proof or a restore can also
            // turn a milestone overdue, and the job only looks at milestones that aren't overdue yet.
            long delayDays = milestone.getDelayDays();
            alertService.createAlert(milestone.getProject(), milestone, Alert.AlertType.DEADLINE_MISSED,
                    "Milestone '" + milestone.getTitle() + "' in project '" + milestone.getProject().getName() + "' is overdue by " + delayDays + " day(s).",
                    AlertService.severityFromDelayDays(delayDays));
        } else {
            alertService.createAlert(milestone.getProject(), milestone, Alert.AlertType.STATUS_CHANGE,
                    "Milestone '" + milestone.getTitle() + "' changed to " + derived, Alert.Severity.LOW);
        }
    }

    private void refresh(Project project) {
        List<StatusDeriver.MilestoneFacts> facts = milestoneRepository.findByProjectIdOrderBySequenceOrderAsc(project.getId())
                .stream().map(m -> new StatusDeriver.MilestoneFacts(m.getStatus(), m.getDueDate(), m.getCompletedDate())).toList();
        Project.Status derived = StatusDeriver.deriveProject(project.getStatus(), project.getEndDate(), LocalDate.now(), facts);
        if (derived != project.getStatus()) {
            project.setStatus(derived);
            projectRepository.save(project);
        }
    }

    public StatusService(final MilestoneRepository milestoneRepository, final ProjectRepository projectRepository, final AttachmentRepository attachmentRepository, final AlertService alertService) {
        this.milestoneRepository = milestoneRepository;
        this.projectRepository = projectRepository;
        this.attachmentRepository = attachmentRepository;
        this.alertService = alertService;
    }
}
