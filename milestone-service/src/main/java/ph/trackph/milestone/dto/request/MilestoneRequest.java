package ph.trackph.milestone.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import ph.trackph.milestone.model.Milestone;

import java.time.LocalDate;

@Getter
@Setter
public class MilestoneRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @NotNull(message = "Due date is required")
    private LocalDate dueDate;

    private int sequenceOrder;
    private Milestone.Status status;
    private LocalDate completedDate;
}
