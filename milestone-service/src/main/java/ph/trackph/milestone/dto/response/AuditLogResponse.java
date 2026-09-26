package ph.trackph.milestone.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import ph.trackph.milestone.model.AuditLog;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class AuditLogResponse {
    private Long id;
    private Long userId;
    private String username;
    private String action;
    private String entityType;
    private Long entityId;
    private String details;
    private String ipAddress;
    private LocalDateTime createdAt;

    public static AuditLogResponse from(AuditLog log) {
        return new AuditLogResponse(
                log.getId(),
                log.getUser() != null ? log.getUser().getId() : null,
                log.getUser() != null ? log.getUser().getUsername() : "system",
                log.getAction(), log.getEntityType(), log.getEntityId(),
                log.getDetails(), log.getIpAddress(), log.getCreatedAt()
        );
    }
}
