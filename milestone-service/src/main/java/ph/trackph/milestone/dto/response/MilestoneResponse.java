package ph.trackph.milestone.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import ph.trackph.milestone.model.Milestone;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
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
        return new MilestoneResponse(
                m.getId(), m.getProject().getId(),
                m.getTitle(), m.getDescription(),
                m.getDueDate(), m.getCompletedDate(),
                m.getStatus().name(), m.getSequenceOrder(),
                m.getDelayDays(), m.getCreatedAt()
        );
    }
}
