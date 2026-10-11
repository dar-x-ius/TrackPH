package ph.trackph.milestone.dto.response;

import ph.trackph.milestone.model.Attachment;
import java.time.LocalDateTime;

public record AttachmentResponse(Long id, Long milestoneId, String originalFilename, String contentType, long fileSize, String uploadedByUsername, LocalDateTime createdAt, String downloadUrl) {

    public static AttachmentResponse from(Attachment a) {
        return new AttachmentResponse(a.getId(), a.getMilestone().getId(), a.getOriginalFilename(), a.getContentType(), a.getFileSize(), a.getUploadedBy().getUsername(), a.getCreatedAt(),
                "/api/v1/milestones/" + a.getMilestone().getId() + "/attachments/" + a.getId() + "/download");
    }
}
