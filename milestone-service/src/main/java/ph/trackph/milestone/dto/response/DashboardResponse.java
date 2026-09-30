package ph.trackph.milestone.dto.response;

public class DashboardResponse {
    private Long userId;
    private String widgetConfig;
    private long totalProjects;
    private long inProgressProjects;
    private long delayedProjects;
    private long unreadAlerts;

    public Long getUserId() {
        return this.userId;
    }

    public String getWidgetConfig() {
        return this.widgetConfig;
    }

    public long getTotalProjects() {
        return this.totalProjects;
    }

    public long getInProgressProjects() {
        return this.inProgressProjects;
    }

    public long getDelayedProjects() {
        return this.delayedProjects;
    }

    public long getUnreadAlerts() {
        return this.unreadAlerts;
    }

    public DashboardResponse(final Long userId, final String widgetConfig, final long totalProjects, final long inProgressProjects, final long delayedProjects, final long unreadAlerts) {
        this.userId = userId;
        this.widgetConfig = widgetConfig;
        this.totalProjects = totalProjects;
        this.inProgressProjects = inProgressProjects;
        this.delayedProjects = delayedProjects;
        this.unreadAlerts = unreadAlerts;
    }
}
