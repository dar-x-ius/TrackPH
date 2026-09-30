package ph.trackph.milestone.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "alerts")
public class Alert {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "milestone_id")
    private Milestone milestone;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertType type;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Severity severity = Severity.LOW;
    @Column(name = "is_read", nullable = false)
    private boolean read = false;
    @CreationTimestamp
    @Column(name = "triggered_at")
    private LocalDateTime triggeredAt;


    public enum AlertType {
        DEADLINE_MISSED, DELAY_DETECTED, STATUS_CHANGE, REPORT_SUBMITTED
    }


    public enum Severity {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    public Long getId() {
        return this.id;
    }

    public Project getProject() {
        return this.project;
    }

    public Milestone getMilestone() {
        return this.milestone;
    }

    public AlertType getType() {
        return this.type;
    }

    public String getMessage() {
        return this.message;
    }

    public Severity getSeverity() {
        return this.severity;
    }

    public boolean isRead() {
        return this.read;
    }

    public LocalDateTime getTriggeredAt() {
        return this.triggeredAt;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public void setProject(final Project project) {
        this.project = project;
    }

    public void setMilestone(final Milestone milestone) {
        this.milestone = milestone;
    }

    public void setType(final AlertType type) {
        this.type = type;
    }

    public void setMessage(final String message) {
        this.message = message;
    }

    public void setSeverity(final Severity severity) {
        this.severity = severity;
    }

    public void setRead(final boolean read) {
        this.read = read;
    }
}
