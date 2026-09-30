package ph.trackph.milestone.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import ph.trackph.milestone.model.User;

public class RegisterRequest {
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 100, message = "Username must be 3–100 characters")
    private String username;
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email address")
    private String email;
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;
    private String fullName;
    private String agencyName;
    private User.Role role = User.Role.PUBLIC;

    public String getUsername() {
        return this.username;
    }

    public String getEmail() {
        return this.email;
    }

    public String getPassword() {
        return this.password;
    }

    public String getFullName() {
        return this.fullName;
    }

    public String getAgencyName() {
        return this.agencyName;
    }

    public User.Role getRole() {
        return this.role;
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    public void setEmail(final String email) {
        this.email = email;
    }

    public void setPassword(final String password) {
        this.password = password;
    }

    public void setFullName(final String fullName) {
        this.fullName = fullName;
    }

    public void setAgencyName(final String agencyName) {
        this.agencyName = agencyName;
    }

    public void setRole(final User.Role role) {
        this.role = role;
    }
}
