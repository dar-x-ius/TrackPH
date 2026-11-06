package ph.trackph.milestone.scheduler;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import ph.trackph.milestone.service.StatusService;

/**
 * Recomputes every status at startup so existing data catches up after this change and any drift heals itself.
 * The midnight check runs first: if the app was down at midnight, the milestones that went overdue then still get
 * their alert, and only the leftover (silent) catch-up is left for the backfill.
 */
@Component
public class StatusBackfillRunner implements ApplicationRunner {
    private final AlertScheduler alertScheduler;
    private final StatusService statusService;

    @Override
    public void run(ApplicationArguments args) {
        alertScheduler.checkOverdueMilestones();
        statusService.refreshAllQuietly();
    }

    public StatusBackfillRunner(final AlertScheduler alertScheduler, final StatusService statusService) {
        this.alertScheduler = alertScheduler;
        this.statusService = statusService;
    }
}
