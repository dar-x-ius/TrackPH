package ph.trackph.milestone.dto.response;

public record DashboardResponse(Long userId, String widgetConfig, long totalProjects, long inProgressProjects, long delayedProjects, long unreadAlerts) {
}
