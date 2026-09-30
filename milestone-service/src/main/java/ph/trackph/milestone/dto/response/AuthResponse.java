package ph.trackph.milestone.dto.response;

public class AuthResponse {
    private String token;
    private String username;
    private String role;
    private Long userId;

    public String getToken() {
        return this.token;
    }

    public String getUsername() {
        return this.username;
    }

    public String getRole() {
        return this.role;
    }

    public Long getUserId() {
        return this.userId;
    }

    public AuthResponse(final String token, final String username, final String role, final Long userId) {
        this.token = token;
        this.username = username;
        this.role = role;
        this.userId = userId;
    }
}
