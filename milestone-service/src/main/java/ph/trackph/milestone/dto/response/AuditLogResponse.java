package ph.trackph.milestone.dto.response;

import ph.trackph.milestone.model.AuditLog;
import java.time.LocalDateTime;

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
        return new AuditLogResponse(log.getId(), log.getUser() != null ? log.getUser().getId() : null, log.getUser() != null ? log.getUser().getUsername() : "system", log.getAction(), log.getEntityType(), log.getEntityId(), log.getDetails(), log.getIpAddress(), log.getCreatedAt());
    }

    public Long getId() {
        return this.id;
    }

    public Long getUserId() {
        return this.userId;
    }

    public String getUsername() {
        return this.username;
    }

    public String getAction() {
        return this.action;
    }

    public String getEntityType() {
        return this.entityType;
    }

    public Long getEntityId() {
        return this.entityId;
    }

    public String getDetails() {
        return this.details;
    }

    public String getIpAddress() {
        return this.ipAddress;
    }

    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    public AuditLogResponse(final Long id, final Long userId, final String username, final String action, final String entityType, final Long entityId, final String details, final String ipAddress, final LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.username = username;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.details = details;
        this.ipAddress = ipAddress;
        this.createdAt = createdAt;
    }
}
