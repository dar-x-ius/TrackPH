package ph.trackph.milestone.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ph.trackph.milestone.scheduler.AlertScheduler;
import ph.trackph.milestone.util.ApiResponse;

/** Dev-only endpoints — only available when spring.profiles.active=dev */
@RestController
@RequestMapping("/api/v1/dev")
@RequiredArgsConstructor
@Profile("dev")
@Tag(name = "Dev Tools", description = "Development-only utilities (not available in production)")
public class DevController {

    private final AlertScheduler alertScheduler;

    @PostMapping("/alerts/trigger")
    @Operation(summary = "Manually trigger the overdue milestone alert scan")
    public ResponseEntity<ApiResponse<String>> triggerAlerts() {
        alertScheduler.triggerNow();
        return ResponseEntity.ok(ApiResponse.success("Alert scan triggered"));
    }
}
