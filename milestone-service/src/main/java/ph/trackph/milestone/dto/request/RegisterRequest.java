package ph.trackph.milestone.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// No role field here on purpose — public self-registration only ever creates PUBLIC accounts.
// AGENCY and ADMIN accounts are provisioned by seed data / an admin, never by whoever fills in this form.
public record RegisterRequest(
        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 100, message = "Username must be 3–100 characters") String username,
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email address") String email,
        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters") String password,
        String fullName,
        String agencyName) {
}
