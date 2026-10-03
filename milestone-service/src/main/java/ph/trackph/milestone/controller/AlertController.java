package ph.trackph.milestone.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import ph.trackph.milestone.dto.response.AlertResponse;
import ph.trackph.milestone.service.AlertService;
import ph.trackph.milestone.util.ApiResponse;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/alerts")
@Tag(name = "Alerts", description = "System alert notifications")
public class AlertController {
    private final AlertService alertService;

    @GetMapping
    @Operation(summary = "List all alerts, newest first")
    public ResponseEntity<ApiResponse<Page<AlertResponse>>> list(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(alertService.findAll(pageable)));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Count unread alerts")
    public ResponseEntity<ApiResponse<Map<String, Long>>> unreadCount() {
        return ResponseEntity.ok(ApiResponse.success(Map.of("count", alertService.countUnread())));
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Live stream of new alerts via Server-Sent Events")
    public SseEmitter stream() {
        return alertService.subscribe();
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "Mark an alert as read")
    public ResponseEntity<ApiResponse<AlertResponse>> markRead(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Marked as read", alertService.markRead(id)));
    }

    public AlertController(final AlertService alertService) {
        this.alertService = alertService;
    }
}
