package ph.trackph.milestone.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ph.trackph.milestone.model.Milestone;
import ph.trackph.milestone.model.Project;
import ph.trackph.milestone.repository.MilestoneRepository;
import ph.trackph.milestone.repository.ProjectRepository;
import ph.trackph.milestone.service.StatusService;
import java.time.LocalDate;
import java.util.List;

@Component
public class AlertScheduler {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AlertScheduler.class);
    private final MilestoneRepository milestoneRepository;
    private final StatusService statusService;
    private final ProjectRepository projectRepository;

    /**
     * Runs daily at midnight. Finds newly overdue milestones and fires alerts.
     */
    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void checkOverdueMilestones() {
        log.info("Running overdue milestone check");
        List<Milestone> newlyOverdue = milestoneRepository.findNewlyOverdue(LocalDate.now());
        int flagged = 0;
        for (Milestone m : newlyOverdue) {
            // StatusService raises the DEADLINE_MISSED alert itself. A proof may have arrived since the last
            // refresh, in which case the milestone comes back COMPLETED and nothing is raised.
            statusService.refreshMilestone(m.getId());
            if (m.getStatus() == Milestone.Status.OVERDUE) flagged++;
        }
        // Passing a project's end date changes its status with no milestone event to trigger it, so re-derive those too.
        projectRepository.findByEndDateBeforeAndStatusNot(LocalDate.now(), Project.Status.CANCELLED)
                .forEach(p -> statusService.refreshProject(p.getId()));
        log.info("Overdue check complete — {} milestone(s) flagged", flagged);
    }

    /**
     * Dev-only trigger for testing without waiting for midnight.
     */
    public void triggerNow() {
        checkOverdueMilestones();
    }

    public AlertScheduler(final MilestoneRepository milestoneRepository, final StatusService statusService, final ProjectRepository projectRepository) {
        this.milestoneRepository = milestoneRepository;
        this.statusService = statusService;
        this.projectRepository = projectRepository;
    }
}
