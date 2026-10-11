package ph.trackph.milestone.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ph.trackph.milestone.dto.response.AttachmentResponse;
import ph.trackph.milestone.exception.ResourceNotFoundException;
import ph.trackph.milestone.exception.UnauthorizedException;
import ph.trackph.milestone.model.Attachment;
import ph.trackph.milestone.model.Milestone;
import ph.trackph.milestone.model.User;
import ph.trackph.milestone.repository.AttachmentRepository;
import ph.trackph.milestone.repository.MilestoneRepository;
import ph.trackph.milestone.repository.UserRepository;
import ph.trackph.milestone.util.AuditContext;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class AttachmentService {
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "pdf", "doc", "docx");
    private static final Map<String, String> ALLOWED_CONTENT_TYPES = Map.of(
            "jpg", "image/jpeg", "jpeg", "image/jpeg", "png", "image/png", "webp", "image/webp",
            "pdf", "application/pdf", "doc", "application/msword",
            "docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

    private final AttachmentRepository attachmentRepository;
    private final MilestoneRepository milestoneRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final AuditContext auditContext;
    private final JdbcTemplate jdbcTemplate;

    public record FileDownload(byte[] content, String contentType, String filename) {
    }

    public List<AttachmentResponse> findByMilestone(Long milestoneId) {
        return attachmentRepository.findByMilestoneIdOrderByCreatedAtDesc(milestoneId).stream().map(AttachmentResponse::from).toList();
    }

    @Transactional
    public AttachmentResponse upload(Long milestoneId, MultipartFile file, Long uploaderId, boolean uploaderIsAdmin) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Attached file is empty");
        }
        String extension = extensionOf(file.getOriginalFilename());
        String expectedContentType = ALLOWED_CONTENT_TYPES.get(extension);
        if (expectedContentType == null || !expectedContentType.equals(file.getContentType())) {
            throw new IllegalArgumentException("Unsupported file type. Allowed: " + ALLOWED_EXTENSIONS);
        }

        Milestone milestone = milestoneRepository.findById(milestoneId).orElseThrow(() -> new ResourceNotFoundException("Milestone", milestoneId));
        if (!uploaderIsAdmin && !milestone.getProject().getAgency().getId().equals(uploaderId)) {
            throw new UnauthorizedException("Only the owning agency or an admin can attach proof to this milestone");
        }
        User uploader = userRepository.findById(uploaderId).orElseThrow(() -> new ResourceNotFoundException("User", uploaderId));

        Attachment attachment = new Attachment();
        attachment.setMilestone(milestone);
        attachment.setUploadedBy(uploader);
        attachment.setOriginalFilename(file.getOriginalFilename());
        attachment.setStoredFilename(fileStorageService.store(file, extension));
        attachment.setContentType(file.getContentType());
        attachment.setFileSize(file.getSize());
        AttachmentResponse created = AttachmentResponse.from(attachmentRepository.save(attachment));
        auditContext.recordChange("Attachment", created.id(), null, created);
        return created;
    }

    public FileDownload download(Long milestoneId, Long attachmentId) throws IOException {
        Attachment attachment = getOrThrow(milestoneId, attachmentId);
        byte[] content = fileStorageService.read(attachment.getStoredFilename());
        return new FileDownload(content, attachment.getContentType(), attachment.getOriginalFilename());
    }

    /**
     * Soft delete — the row (and the file on disk) stays; it just drops out of the public listing
     * and download endpoints via the entity's @SQLRestriction. Deliberately does NOT call
     * fileStorageService.delete() anymore: this is proof-of-progress evidence on a transparency
     * platform, so "deleted" here means "hidden from the normal view," not "gone."
     */
    @Transactional
    public void delete(Long milestoneId, Long attachmentId, Long requesterId, boolean requesterIsAdmin) {
        Attachment attachment = getOrThrow(milestoneId, attachmentId);
        if (!requesterIsAdmin && !attachment.getUploadedBy().getId().equals(requesterId)) {
            throw new UnauthorizedException("Only the uploader or an admin can delete this attachment");
        }
        AttachmentResponse snapshot = AttachmentResponse.from(attachment);
        attachment.setDeletedAt(LocalDateTime.now());
        attachmentRepository.save(attachment);
        auditContext.recordDeletion("Attachment", attachmentId, snapshot);
    }

    public List<Map<String, Object>> findArchivedByMilestone(Long milestoneId) {
        return jdbcTemplate.queryForList(
                "SELECT a.id, a.original_filename AS originalFilename, u.username AS uploadedByUsername, a.deleted_at AS deletedAt " +
                        "FROM milestone_attachments a JOIN users u ON u.id = a.uploaded_by " +
                        "WHERE a.milestone_id = ? AND a.deleted_at IS NOT NULL ORDER BY a.deleted_at DESC",
                milestoneId);
    }

    @Transactional
    public void restore(Long milestoneId, Long attachmentId) {
        if (attachmentRepository.restoreById(attachmentId, milestoneId) == 0) {
            throw new ResourceNotFoundException("Attachment", attachmentId);
        }
        auditContext.recordChange("Attachment", attachmentId, null, AttachmentResponse.from(getOrThrow(milestoneId, attachmentId)));
    }

    private Attachment getOrThrow(Long milestoneId, Long attachmentId) {
        Attachment attachment = attachmentRepository.findById(attachmentId).orElseThrow(() -> new ResourceNotFoundException("Attachment", attachmentId));
        if (!attachment.getMilestone().getId().equals(milestoneId)) {
            throw new ResourceNotFoundException("Attachment", attachmentId);
        }
        return attachment;
    }

    private String extensionOf(String originalFilename) {
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new IllegalArgumentException("File must have an extension");
        }
        return originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();
    }

    public AttachmentService(final AttachmentRepository attachmentRepository, final MilestoneRepository milestoneRepository, final UserRepository userRepository, final FileStorageService fileStorageService, final AuditContext auditContext, final JdbcTemplate jdbcTemplate) {
        this.attachmentRepository = attachmentRepository;
        this.milestoneRepository = milestoneRepository;
        this.userRepository = userRepository;
        this.fileStorageService = fileStorageService;
        this.auditContext = auditContext;
        this.jdbcTemplate = jdbcTemplate;
    }
}
