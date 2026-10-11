package ph.trackph.milestone.dto.response;

import ph.trackph.milestone.model.AuditLog;
import java.time.LocalDateTime;

public record AuditLogResponse(Long id, Long userId, String username, String action, String entityType, Long entityId, String details, String ipAddress, LocalDateTime createdAt) {

    public static AuditLogResponse from(AuditLog log) {
        return new AuditLogResponse(log.getId(), log.getUser() != null ? log.getUser().getId() : null, log.getUser() != null ? log.getUser().getUsername() : "system", log.getAction(), log.getEntityType(), log.getEntityId(), log.getDetails(), log.getIpAddress(), log.getCreatedAt());
    }

    /** Same accountability trail (who did what, when) minus IP address — that's network metadata, not something the public needs to hold an agency accountable. */
    public static AuditLogResponse publicView(AuditLog log) {
        AuditLogResponse full = from(log);
        return new AuditLogResponse(full.id(), full.userId(), full.username(), full.action(), full.entityType(), full.entityId(), full.details(), null, full.createdAt());
    }
}
