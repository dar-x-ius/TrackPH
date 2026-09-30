package ph.trackph.milestone.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ph.trackph.milestone.dto.response.AuditLogResponse;
import ph.trackph.milestone.repository.AuditLogRepository;

@Service
public class AuditLogService {
    private final AuditLogRepository auditLogRepository;

    public Page<AuditLogResponse> findAll(Pageable pageable) {
        return auditLogRepository.findAll(pageable).map(AuditLogResponse::from);
    }

    public Page<AuditLogResponse> findByUser(Long userId, Pageable pageable) {
        return auditLogRepository.findByUserId(userId, pageable).map(AuditLogResponse::from);
    }

    public Page<AuditLogResponse> findByEntity(String entityType, Long entityId, Pageable pageable) {
        return auditLogRepository.findByEntityTypeAndEntityId(entityType, entityId, pageable).map(AuditLogResponse::from);
    }

    public AuditLogService(final AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }
}
