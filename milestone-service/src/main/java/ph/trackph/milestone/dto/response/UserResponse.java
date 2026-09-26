package ph.trackph.milestone.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import ph.trackph.milestone.model.User;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String username;
    private String email;
    private String role;
    private String fullName;
    private String agencyName;
    private boolean active;
    private LocalDateTime createdAt;

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(), user.getUsername(), user.getEmail(),
                user.getRole().name(), user.getFullName(), user.getAgencyName(),
                user.isActive(), user.getCreatedAt()
        );
    }
}
