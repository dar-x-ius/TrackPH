package ph.trackph.milestone.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ph.trackph.milestone.dto.request.MilestoneRequest;
import ph.trackph.milestone.dto.response.MilestoneResponse;
import ph.trackph.milestone.service.MilestoneService;
import ph.trackph.milestone.util.ApiResponse;
import java.util.List;

@RestController
@Tag(name = "Milestones", description = "Milestone tracking and progress reporting")
public class MilestoneController {
    private final MilestoneService milestoneService;

    @GetMapping("/api/v1/projects/{projectId}/milestones")
    @Operation(summary = "List milestones for a project")
    public ResponseEntity<ApiResponse<List<MilestoneResponse>>> listByProject(@PathVariable Long projectId) {
        return ResponseEntity.ok(ApiResponse.success(milestoneService.findByProject(projectId)));
    }

    @PostMapping("/api/v1/projects/{projectId}/milestones")
    @Operation(summary = "Add a milestone to a project")
    public ResponseEntity<ApiResponse<MilestoneResponse>> create(@PathVariable Long projectId, @Valid @RequestBody MilestoneRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Milestone created", milestoneService.create(projectId, request)));
    }

    @GetMapping("/api/v1/milestones/{id}")
    @Operation(summary = "Get milestone by ID")
    public ResponseEntity<ApiResponse<MilestoneResponse>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(milestoneService.findById(id)));
    }

    @PutMapping("/api/v1/milestones/{id}")
    @Operation(summary = "Update milestone status or details")
    public ResponseEntity<ApiResponse<MilestoneResponse>> update(@PathVariable Long id, @Valid @RequestBody MilestoneRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Updated", milestoneService.update(id, request)));
    }

    @PostMapping("/api/v1/milestones/{id}/report")
    @Operation(summary = "Submit a progress report for a milestone (MANAGER/AGENCY only)")
    public ResponseEntity<ApiResponse<MilestoneResponse>> submitReport(@PathVariable Long id, @Valid @RequestBody MilestoneRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Report submitted", milestoneService.submitReport(id, request)));
    }

    public MilestoneController(final MilestoneService milestoneService) {
        this.milestoneService = milestoneService;
    }
}
