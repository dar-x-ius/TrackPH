package ph.trackph.milestone.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ph.trackph.milestone.dto.response.AuditActivityReportResponse;
import ph.trackph.milestone.dto.response.AuditLogResponse;
import ph.trackph.milestone.service.AuditLogService;
import ph.trackph.milestone.util.ApiResponse;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/audit-logs")
@Tag(name = "Audit Logs", description = "Immutable user action trail (ADMIN/AGENCY only)")
public class AuditLogController {
    private final AuditLogService auditLogService;

    @GetMapping
    @Operation(summary = "List all audit logs, newest first")
    public ResponseEntity<ApiResponse<Page<AuditLogResponse>>> list(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(auditLogService.findAll(pageable)));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "List audit logs for a specific user")
    public ResponseEntity<ApiResponse<Page<AuditLogResponse>>> byUser(@PathVariable Long userId, Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(auditLogService.findByUser(userId, pageable)));
    }

    @GetMapping("/report")
    @Operation(summary = "Compiled activity report: action counts by user, with a most-active-users ranking")
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
