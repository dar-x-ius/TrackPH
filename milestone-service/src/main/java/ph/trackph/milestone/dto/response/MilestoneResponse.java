package ph.trackph.milestone.dto.response;

import ph.trackph.milestone.model.Milestone;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class MilestoneResponse {
    private Long id;
    private Long projectId;
    private String title;
    private String description;
    private LocalDate dueDate;
    private LocalDate completedDate;
    private String status;
    private int sequenceOrder;
    private long delayDays;
    private LocalDateTime createdAt;

    public static MilestoneResponse from(Milestone m) {
        return new MilestoneResponse(m.getId(), m.getProject().getId(), m.getTitle(), m.getDescription(), m.getDueDate(), m.getCompletedDate(), m.getStatus().name(), m.getSequenceOrder(), m.getDelayDays(), m.getCreatedAt());
    }

    public Long getId() {
        return this.id;
    }

    public Long getProjectId() {
        return this.projectId;
    }

    public String getTitle() {
        return this.title;
    }

    public String getDescription() {
        return this.description;
    }

    public LocalDate getDueDate() {
        return this.dueDate;
    }

    public LocalDate getCompletedDate() {
        return this.completedDate;
    }

    public String getStatus() {
        return this.status;
    }

    public int getSequenceOrder() {
        return this.sequenceOrder;
    }

    public long getDelayDays() {
        return this.delayDays;
    }

    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    public MilestoneResponse(final Long id, final Long projectId, final String title, final String description, final LocalDate dueDate, final LocalDate completedDate, final String status, final int sequenceOrder, final long delayDays, final LocalDateTime createdAt) {
        this.id = id;
        this.projectId = projectId;
        this.title = title;
        this.description = description;
        this.dueDate = dueDate;
        this.completedDate = completedDate;
        this.status = status;
        this.sequenceOrder = sequenceOrder;
        this.delayDays = delayDays;
        this.createdAt = createdAt;
    }
}
