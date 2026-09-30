package ph.trackph.milestone.dto.response;

import ph.trackph.milestone.model.Alert;
import java.time.LocalDateTime;

public record AlertResponse(Long id, Long projectId, Long milestoneId, String type, String message, String severity, boolean read, LocalDateTime triggeredAt) {

    public static AlertResponse from(Alert a) {
        return new AlertResponse(a.getId(), a.getProject().getId(), a.getMilestone() != null ? a.getMilestone().getId() : null, a.getType().name(), a.getMessage(), a.getSeverity().name(), a.isRead(), a.getTriggeredAt());
    }
}
