package ph.trackph.milestone.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ph.trackph.milestone.dto.response.AuditActivityReportResponse;
import ph.trackph.milestone.dto.response.AuditLogResponse;
import ph.trackph.milestone.repository.AuditLogRepository;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AuditLogService {
    private final AuditLogRepository auditLogRepository;

    public Page<AuditLogResponse> findAll(Pageable pageable) {
        return auditLogRepository.findAll(pageable).map(AuditLogResponse::from);
    }

    // Sign-ins and account creation are account metadata, not project/milestone activity —
    // keep them out of the public trail but still visible to ADMIN via findAll().
    public Page<AuditLogResponse> findAllPublic(Pageable pageable) {
        return auditLogRepository.findByActionNotContaining("/api/v1/auth/", pageable).map(AuditLogResponse::publicView);
    }

    public Page<AuditLogResponse> findByUser(Long userId, Pageable pageable) {
        return auditLogRepository.findByUserId(userId, pageable).map(AuditLogResponse::from);
    }

    public Page<AuditLogResponse> findByEntity(String entityType, Long entityId, Pageable pageable) {
        return auditLogRepository.findByEntityTypeAndEntityId(entityType, entityId, pageable).map(AuditLogResponse::from);
    }

    public Page<AuditLogResponse> findByEntityPublic(String entityType, Long entityId, Pageable pageable) {
        return auditLogRepository.findByEntityTypeAndEntityId(entityType, entityId, pageable).map(AuditLogResponse::publicView);
    }

    public AuditActivityReportResponse getActivityReport(LocalDateTime from, LocalDateTime to, int topN) {
        List<AuditActivityReportResponse.ActionCount> actionCounts = auditLogRepository.countByUserAndActionBetween(from, to).stream()
                .map(r -> new AuditActivityReportResponse.ActionCount((Long) r[0], (String) r[1], (String) r[2], (Long) r[3]))
                .toList();

        Map<Long, String> usernames = new HashMap<>();
        Map<Long, Long> totals = new HashMap<>();
        for (var ac : actionCounts) {
            usernames.put(ac.userId(), ac.username());
            totals.merge(ac.userId(), ac.count(), Long::sum);
        }
        List<AuditActivityReportResponse.UserActivity> topUsers = totals.entrySet().stream()
                .map(e -> new AuditActivityReportResponse.UserActivity(e.getKey(), usernames.get(e.getKey()), e.getValue()))
                .sorted(Comparator.comparingLong(AuditActivityReportResponse.UserActivity::totalActions).reversed())
                .limit(topN)
                .toList();

        return new AuditActivityReportResponse(actionCounts, topUsers);
    }

    public AuditLogService(final AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }
}
