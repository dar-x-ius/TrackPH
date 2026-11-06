package ph.trackph.milestone.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/** Status and completedDate are derived by StatusService; an old client that still sends them is ignored. */
public record MilestoneRequest(
        @NotBlank(message = "Title is required") String title,
        String description,
        @NotNull(message = "Due date is required") LocalDate dueDate,
        int sequenceOrder) {
}
