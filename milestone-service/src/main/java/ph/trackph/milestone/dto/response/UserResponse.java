package ph.trackph.milestone.dto.response;

import ph.trackph.milestone.model.User;
import java.time.LocalDateTime;

public record UserResponse(Long id, String username, String email, String fullName, String agencyName, User.Role role, boolean active, LocalDateTime createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getEmail(), user.getFullName(), user.getAgencyName(), user.getRole(), user.isActive(), user.getCreatedAt());
    }
}
