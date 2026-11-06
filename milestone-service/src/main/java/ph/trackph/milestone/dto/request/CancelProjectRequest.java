package ph.trackph.milestone.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CancelProjectRequest(@NotBlank(message = "A reason is required") String reason) {
}
