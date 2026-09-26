package ph.trackph.milestone.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import ph.trackph.milestone.model.Alert;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
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
        return new AlertResponse(
                a.getId(),
                a.getProject().getId(),
                a.getMilestone() != null ? a.getMilestone().getId() : null,
                a.getType().name(),
                a.getMessage(),
                a.getSeverity().name(),
                a.isRead(),
                a.getTriggeredAt()
        );
    }
}
