package ph.trackph.milestone.dto.response;

import ph.trackph.milestone.model.Milestone;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record MilestoneResponse(Long id, Long projectId, String title, String description, LocalDate dueDate, LocalDate completedDate, String status, int sequenceOrder, long delayDays, LocalDateTime createdAt) {

    public static MilestoneResponse from(Milestone m) {
        return new MilestoneResponse(m.getId(), m.getProject().getId(), m.getTitle(), m.getDescription(), m.getDueDate(), m.getCompletedDate(), m.getStatus().name(), m.getSequenceOrder(), m.getDelayDays(), m.getCreatedAt());
    }
}
