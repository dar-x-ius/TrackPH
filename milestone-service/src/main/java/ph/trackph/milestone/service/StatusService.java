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
        long counting = attachmentRepository.countByMilestoneId(milestone.getId());
        Milestone.Status derived = StatusDeriver.deriveMilestone(
                milestone.getDueDate(), LocalDate.now(), (int) counting, StatusDeriver.DEFAULT_REQUIRED_PROOFS);
        if (derived == milestone.getStatus()) return;

        milestone.setStatus(derived);
        if (derived == Milestone.Status.COMPLETED) {
            if (milestone.getCompletedDate() == null) milestone.setCompletedDate(LocalDate.now());
        } else {
            milestone.setCompletedDate(null);
        }
        milestoneRepository.save(milestone);

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
        List<Milestone.Status> statuses = milestoneRepository.findByProjectIdOrderBySequenceOrderAsc(project.getId())
                .stream().map(Milestone::getStatus).toList();
        Project.Status derived = StatusDeriver.deriveProject(project.getStatus(), statuses);
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
