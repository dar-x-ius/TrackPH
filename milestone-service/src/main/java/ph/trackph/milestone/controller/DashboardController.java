package ph.trackph.milestone.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ph.trackph.milestone.dto.request.DashboardSettingsRequest;
import ph.trackph.milestone.dto.response.DashboardResponse;
import ph.trackph.milestone.service.DashboardService;
import ph.trackph.milestone.util.ApiResponse;

@RestController
@RequestMapping("/api/v1/dashboard")
@Tag(name = "Dashboard", description = "Personalized user dashboard")
public class DashboardController {
    private final DashboardService dashboardService;

    @GetMapping
    @Operation(summary = "Get dashboard data (aggregate stats are public; widget config is personalized only when logged in)")
    public ResponseEntity<ApiResponse<DashboardResponse>> get(Authentication auth) {
        Long userId = auth != null && auth.getPrincipal() instanceof Long id ? id : null;
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getDashboard(userId)));
    }

    @PutMapping("/settings")
    @Operation(summary = "Update dashboard widget configuration")
    public ResponseEntity<ApiResponse<DashboardResponse>> updateSettings(@RequestBody DashboardSettingsRequest request, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success("Settings saved", dashboardService.updateSettings(userId, request)));
    }

    public DashboardController(final DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }
}
