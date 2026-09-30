package ph.trackph.milestone.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import ph.trackph.milestone.model.Milestone;
import java.time.LocalDate;

public class MilestoneRequest {
    @NotBlank(message = "Title is required")
    private String title;
    private String description;
    @NotNull(message = "Due date is required")
    private LocalDate dueDate;
    private int sequenceOrder;
    private Milestone.Status status;
    private LocalDate completedDate;

    public String getTitle() {
        return this.title;
    }

    public String getDescription() {
        return this.description;
    }

    public LocalDate getDueDate() {
        return this.dueDate;
    }

    public int getSequenceOrder() {
        return this.sequenceOrder;
    }

    public Milestone.Status getStatus() {
        return this.status;
    }

    public LocalDate getCompletedDate() {
        return this.completedDate;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public void setDueDate(final LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public void setSequenceOrder(final int sequenceOrder) {
        this.sequenceOrder = sequenceOrder;
    }

    public void setStatus(final Milestone.Status status) {
        this.status = status;
    }

    public void setCompletedDate(final LocalDate completedDate) {
        this.completedDate = completedDate;
    }
}
