package ph.trackph.milestone.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.*;
import ph.trackph.milestone.dto.response.AuditActivityReportResponse;
import ph.trackph.milestone.dto.response.AuditLogResponse;
import ph.trackph.milestone.service.AuditLogService;
import ph.trackph.milestone.util.ApiResponse;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/audit-logs")
@Tag(name = "Audit Logs", description = "Immutable action trail — public (IP address withheld); full detail for ADMIN/AGENCY")
public class AuditLogController {
    private final AuditLogService auditLogService;

    @GetMapping
    @Operation(summary = "List all audit logs, newest first (public view withholds IP address)")
    public ResponseEntity<ApiResponse<Page<AuditLogResponse>>> list(Authentication auth, Pageable pageable) {
        Page<AuditLogResponse> page = canSeeIp(auth) ? auditLogService.findAll(pageable) : auditLogService.findAllPublic(pageable);
        return ResponseEntity.ok(ApiResponse.success(page));
    }

    @GetMapping("/entity/{entityType}/{entityId}")
    @Operation(summary = "Edit history for one entity (e.g. a Project or Milestone), oldest first (public view withholds IP address)")
    public ResponseEntity<ApiResponse<Page<AuditLogResponse>>> byEntity(@PathVariable String entityType, @PathVariable Long entityId, Authentication auth, Pageable pageable) {
        Page<AuditLogResponse> page = canSeeIp(auth) ? auditLogService.findByEntity(entityType, entityId, pageable) : auditLogService.findByEntityPublic(entityType, entityId, pageable);
        return ResponseEntity.ok(ApiResponse.success(page));
    }

    /**
     * Full IP is ADMIN-only: AGENCY accounts are usually the ones being held accountable by
     * these logs, so letting one AGENCY see every other agency's and admin's raw IPs would be
     * broader access than the role needs (data-minimization principle, RA 10173).
     */
    private boolean canSeeIp(Authentication auth) {
        return auth != null && auth.isAuthenticated() && auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "List audit logs for a specific user (ADMIN/AGENCY only — internal account review)")
    public ResponseEntity<ApiResponse<Page<AuditLogResponse>>> byUser(@PathVariable Long userId, Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(auditLogService.findByUser(userId, pageable)));
    }

    @GetMapping("/report")
    @Operation(summary = "Compiled activity report: action counts by user, with a most-active-users ranking (ADMIN/AGENCY only)")
    public ResponseEntity<ApiResponse<AuditActivityReportResponse>> report(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "5") int topN) {
        return ResponseEntity.ok(ApiResponse.success(auditLogService.getActivityReport(from, to, topN)));
    }

    public AuditLogController(final AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }
}
