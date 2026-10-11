package ph.trackph.milestone.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ph.trackph.milestone.dto.request.AdminCreateUserRequest;
import ph.trackph.milestone.dto.request.ResetPasswordRequest;
import ph.trackph.milestone.dto.response.UserResponse;
import ph.trackph.milestone.model.User;
import ph.trackph.milestone.service.AuthService;
import ph.trackph.milestone.util.ApiResponse;

import java.util.List;

// Path falls under /api/v1/admin/**, which SecurityConfig already restricts to ROLE_ADMIN —
// no separate security rule needed here.
@RestController
@RequestMapping("/api/v1/admin/users")
@Tag(name = "Admin Users", description = "Admin-provisioned staff and agency accounts")
public class AdminUserController {
    private static final List<User.Role> DEFAULT_ROLES = List.of(User.Role.ADMIN, User.Role.AGENCY);
    private final AuthService authService;

    @GetMapping
    @Operation(summary = "List accounts, staff (ADMIN/AGENCY) by default — pass ?roles= to filter, e.g. roles=PUBLIC")
    public ResponseEntity<ApiResponse<List<UserResponse>>> list(@RequestParam(required = false) List<User.Role> roles) {
        List<User.Role> filter = (roles == null || roles.isEmpty()) ? DEFAULT_ROLES : roles;
        return ResponseEntity.ok(ApiResponse.success(authService.listUsers(filter)));
    }

    @PostMapping
    @Operation(summary = "Create a staff or agency account")
    public ResponseEntity<ApiResponse<UserResponse>> create(@Valid @RequestBody AdminCreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Account created", authService.createByAdmin(request)));
    }

    @PatchMapping("/{id}/password")
    @Operation(summary = "Reset a non-admin account's password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@PathVariable Long id, @Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(id, request);
        return ResponseEntity.ok(ApiResponse.success("Password reset", null));
    }

    @PatchMapping("/{id}/active")
    @Operation(summary = "Activate or deactivate a non-admin account")
    public ResponseEntity<ApiResponse<UserResponse>> setActive(@PathVariable Long id, @RequestParam boolean active) {
        return ResponseEntity.ok(ApiResponse.success(authService.setActive(id, active)));
    }

    public AdminUserController(final AuthService authService) {
        this.authService = authService;
    }
}
