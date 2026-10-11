package ph.trackph.milestone.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "milestone_attachments")
public class Attachment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "milestone_id", nullable = false)
    private Milestone milestone;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by", nullable = false)
    private User uploadedBy;
    @Column(name = "original_filename", nullable = false)
    private String originalFilename;
    @Column(name = "stored_filename", nullable = false, unique = true)
    private String storedFilename;
    @Column(name = "content_type", nullable = false)
    private String contentType;
    @Column(name = "file_size", nullable = false)
    private long fileSize;
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public Long getId() {
        return this.id;
    }

    public Milestone getMilestone() {
        return this.milestone;
    }

    public User getUploadedBy() {
        return this.uploadedBy;
    }

    public String getOriginalFilename() {
        return this.originalFilename;
    }

    public String getStoredFilename() {
        return this.storedFilename;
    }

    public String getContentType() {
        return this.contentType;
    }

    public long getFileSize() {
        return this.fileSize;
    }

    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public void setMilestone(final Milestone milestone) {
        this.milestone = milestone;
    }

    public void setUploadedBy(final User uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    public void setOriginalFilename(final String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public void setStoredFilename(final String storedFilename) {
        this.storedFilename = storedFilename;
    }

    public void setContentType(final String contentType) {
        this.contentType = contentType;
    }

    public void setFileSize(final long fileSize) {
        this.fileSize = fileSize;
    }

    public void setCreatedAt(final LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
