package ph.trackph.milestone.dto.response;

public record AuthResponse(String token, String username, String role, Long userId) {
}
