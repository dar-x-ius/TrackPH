package ph.trackph.milestone.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DashboardResponse {
    private Long userId;
    private String widgetConfig;
    private long totalProjects;
    private long inProgressProjects;
    private long delayedProjects;
    private long unreadAlerts;
}
