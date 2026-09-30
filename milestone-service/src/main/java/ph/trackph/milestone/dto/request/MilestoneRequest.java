package ph.trackph.milestone.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import ph.trackph.milestone.model.Milestone;
import java.time.LocalDate;

public record MilestoneRequest(
        @NotBlank(message = "Title is required") String title,
        String description,
        @NotNull(message = "Due date is required") LocalDate dueDate,
        int sequenceOrder,
        Milestone.Status status,
        LocalDate completedDate) {
}
