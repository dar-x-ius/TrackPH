package ph.trackph.milestone.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
    @Column(nullable = false, length = 100)
    private String action;
    @Column(name = "entity_type", length = 100)
    private String entityType;
    @Column(name = "entity_id")
    private Long entityId;
    @Column(columnDefinition = "TEXT")
    private String details;
    @Column(name = "ip_address", length = 45)
    private String ipAddress;
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public Long getId() {
        return this.id;
    }

    public User getUser() {
        return this.user;
    }

    public String getAction() {
        return this.action;
    }

    public String getEntityType() {
        return this.entityType;
    }

    public Long getEntityId() {
        return this.entityId;
    }

    public String getDetails() {
        return this.details;
    }

    public String getIpAddress() {
        return this.ipAddress;
    }

    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public void setUser(final User user) {
        this.user = user;
    }

    public void setAction(final String action) {
        this.action = action;
    }

    public void setEntityType(final String entityType) {
        this.entityType = entityType;
    }

    public void setEntityId(final Long entityId) {
        this.entityId = entityId;
    }

    public void setDetails(final String details) {
        this.details = details;
    }

    public void setIpAddress(final String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public void setCreatedAt(final LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public AuditLog() {
    }
}
