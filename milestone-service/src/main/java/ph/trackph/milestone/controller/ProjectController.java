package ph.trackph.milestone.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.*;
import ph.trackph.milestone.dto.request.ProjectRequest;
import ph.trackph.milestone.dto.response.ProjectResponse;
import ph.trackph.milestone.service.ProjectService;
import ph.trackph.milestone.util.ApiResponse;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/projects")
@Tag(name = "Projects", description = "Government project management")
public class ProjectController {
    private final ProjectService projectService;

    @GetMapping
    @Operation(summary = "List all projects (public view for guests, full view for auth users)")
    public ResponseEntity<ApiResponse<Page<ProjectResponse>>> list(Authentication auth, Pageable pageable) {
        boolean isPublic = auth == null || !auth.isAuthenticated() || auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_PUBLIC"));
        Page<ProjectResponse> page = isPublic ? projectService.findAllPublic(pageable) : projectService.findAll(pageable);
        return ResponseEntity.ok(ApiResponse.success(page));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get project by ID")
    public ResponseEntity<ApiResponse<ProjectResponse>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(projectService.findById(id)));
    }

    @GetMapping("/archived")
    @Operation(summary = "List soft-deleted projects (public — transparency over deletions, ADMIN only restores them)")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> archived() {
        return ResponseEntity.ok(ApiResponse.success(projectService.findArchived()));
    }

    @PostMapping("/{id}/restore")
    @Operation(summary = "Restore a soft-deleted project, its milestones and their attachments (ADMIN only)")
    public ResponseEntity<ApiResponse<Void>> restore(@PathVariable Long id) {
        projectService.restore(id);
        return ResponseEntity.ok(ApiResponse.success("Restored", null));
    }

    @PostMapping
    @Operation(summary = "Create a new project")
    public ResponseEntity<ApiResponse<ProjectResponse>> create(@Valid @RequestBody ProjectRequest request, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Project created", projectService.create(userId, request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a project (owning agency or ADMIN only)")
    public ResponseEntity<ApiResponse<ProjectResponse>> update(@PathVariable Long id, @Valid @RequestBody ProjectRequest request, Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success("Updated", projectService.update(id, request, (Long) auth.getPrincipal(), isAdmin(auth))));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a project (owning agency or ADMIN only)")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id, Authentication auth) {
        projectService.delete(id, (Long) auth.getPrincipal(), isAdmin(auth));
        return ResponseEntity.ok(ApiResponse.success("Deleted", null));
    }

    private boolean isAdmin(Authentication auth) {
        return auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    public ProjectController(final ProjectService projectService) {
        this.projectService = projectService;
    }
}
