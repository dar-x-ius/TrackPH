package ph.trackph.milestone.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ph.trackph.milestone.model.Alert;
import ph.trackph.milestone.model.Milestone;
import ph.trackph.milestone.repository.MilestoneRepository;
import ph.trackph.milestone.service.AlertService;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class AlertScheduler {

    private final MilestoneRepository milestoneRepository;
    private final AlertService alertService;

    /** Runs daily at midnight. Finds newly overdue milestones and fires alerts. */
    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void checkOverdueMilestones() {
        log.info("Running overdue milestone check");
        List<Milestone> newlyOverdue = milestoneRepository.findNewlyOverdue(LocalDate.now());
        for (Milestone m : newlyOverdue) {
            m.setStatus(Milestone.Status.OVERDUE);
            long delayDays = m.getDelayDays();
            Alert.Severity severity = AlertService.severityFromDelayDays(delayDays);
            alertService.createAlert(
                    m.getProject(), m,
                    Alert.AlertType.DEADLINE_MISSED,
                    "Milestone '" + m.getTitle() + "' in project '" + m.getProject().getName()
                            + "' is overdue by " + delayDays + " day(s).",
                    severity
            );
        }
        log.info("Overdue check complete — {} milestone(s) flagged", newlyOverdue.size());
    }

    /** Dev-only trigger for testing without waiting for midnight. */
    public void triggerNow() {
        checkOverdueMilestones();
    }
}
