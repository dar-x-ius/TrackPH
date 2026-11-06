package ph.trackph.milestone.dto.response;

import ph.trackph.milestone.model.Milestone;
import ph.trackph.milestone.service.StatusDeriver;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** {@code needsReview} is the asterisk: the status rests on a proof nobody has checked. Derived on every read, never stored. */
public record MilestoneResponse(Long id, Long projectId, String title, String description, LocalDate dueDate, LocalDate completedDate, String status, int sequenceOrder, long delayDays, LocalDateTime createdAt, boolean needsReview) {

    public static MilestoneResponse from(Milestone m) {
        return new MilestoneResponse(m.getId(), m.getProject().getId(), m.getTitle(), m.getDescription(), m.getDueDate(), m.getCompletedDate(), m.getStatus().name(), m.getSequenceOrder(), m.getDelayDays(), m.getCreatedAt(), needsReview(m));
    }

    /** Phase 1 has no review step yet, so every counting proof is unreviewed. */
    public static boolean needsReview(Milestone m) {
        // ponytail: lazy attachment loading is N+1 across a project list; fine at this scale, batch-fetch if the list slows
        return StatusDeriver.milestoneNeedsReview(m.getStatus(), m.getAttachments().size());
    }
}
