package ph.trackph.template.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import ph.trackph.template.model.User;

public record RegisterRequest(
        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 100, message = "Username must be 3–100 characters") String username,
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email address") String email,
        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters") String password,
        String fullName,
        String agencyName,
        User.Role role) {
}
