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

    @PostMapping
    @Operation(summary = "Create a new project")
    public ResponseEntity<ApiResponse<ProjectResponse>> create(@Valid @RequestBody ProjectRequest request, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Project created", projectService.create(userId, request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a project")
    public ResponseEntity<ApiResponse<ProjectResponse>> update(@PathVariable Long id, @Valid @RequestBody ProjectRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Updated", projectService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a project")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        projectService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Deleted", null));
    }

    public ProjectController(final ProjectService projectService) {
        this.projectService = projectService;
    }
}
