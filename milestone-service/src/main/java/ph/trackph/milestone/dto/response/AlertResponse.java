package ph.trackph.milestone.dto.response;

import ph.trackph.milestone.model.Alert;
import java.time.LocalDateTime;

public class AlertResponse {
    private Long id;
    private Long projectId;
    private Long milestoneId;
    private String type;
    private String message;
    private String severity;
    private boolean read;
    private LocalDateTime triggeredAt;

    public static AlertResponse from(Alert a) {
        return new AlertResponse(a.getId(), a.getProject().getId(), a.getMilestone() != null ? a.getMilestone().getId() : null, a.getType().name(), a.getMessage(), a.getSeverity().name(), a.isRead(), a.getTriggeredAt());
    }

    public Long getId() {
        return this.id;
    }

    public Long getProjectId() {
        return this.projectId;
    }

    public Long getMilestoneId() {
        return this.milestoneId;
    }

    public String getType() {
        return this.type;
    }

    public String getMessage() {
        return this.message;
    }

    public String getSeverity() {
        return this.severity;
    }

    public boolean isRead() {
        return this.read;
    }

    public LocalDateTime getTriggeredAt() {
        return this.triggeredAt;
    }

    public AlertResponse(final Long id, final Long projectId, final Long milestoneId, final String type, final String message, final String severity, final boolean read, final LocalDateTime triggeredAt) {
        this.id = id;
        this.projectId = projectId;
        this.milestoneId = milestoneId;
        this.type = type;
        this.message = message;
        this.severity = severity;
        this.read = read;
        this.triggeredAt = triggeredAt;
    }
}
