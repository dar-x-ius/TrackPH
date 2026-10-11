package ph.trackph.milestone.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ph.trackph.milestone.dto.request.AdminCreateUserRequest;
import ph.trackph.milestone.dto.response.UserResponse;
import ph.trackph.milestone.service.AuthService;
import ph.trackph.milestone.util.ApiResponse;

import java.util.List;

// Path falls under /api/v1/admin/**, which SecurityConfig already restricts to ROLE_ADMIN —
// no separate security rule needed here.
@RestController
@RequestMapping("/api/v1/admin/users")
@Tag(name = "Admin Users", description = "Admin-provisioned staff and agency accounts")
public class AdminUserController {
    private final AuthService authService;

    @GetMapping
    @Operation(summary = "List all accounts")
    public ResponseEntity<ApiResponse<List<UserResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.success(authService.listUsers()));
    }

    @PostMapping
    @Operation(summary = "Create a staff or agency account")
    public ResponseEntity<ApiResponse<UserResponse>> create(@Valid @RequestBody AdminCreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Account created", authService.createByAdmin(request)));
    }

    public AdminUserController(final AuthService authService) {
        this.authService = authService;
    }
}
