package ph.trackph.milestone.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ph.trackph.milestone.dto.response.AuditLogResponse;
import ph.trackph.milestone.service.AuditLogService;
import ph.trackph.milestone.util.ApiResponse;

@RestController
@RequestMapping("/api/v1/audit-logs")
@RequiredArgsConstructor
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
    public ResponseEntity<ApiResponse<Page<AuditLogResponse>>> byUser(@PathVariable Long userId,
                                                                        Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(auditLogService.findByUser(userId, pageable)));
    }
}
