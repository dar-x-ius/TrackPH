package ph.trackph.milestone.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ph.trackph.milestone.model.User;

// Unlike RegisterRequest, this one takes a role — only an ADMIN can reach this endpoint,
// so there's no self-escalation risk in letting the caller pick it.
public record AdminCreateUserRequest(
        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 100, message = "Username must be 3–100 characters") String username,
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email address") String email,
        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters") String password,
        @NotNull(message = "Role is required") User.Role role,
        String fullName,
        String agencyName) {
}
