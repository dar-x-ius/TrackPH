package ph.trackph.milestone.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "milestones")
public class Milestone {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;
    @Column(nullable = false)
    private String title;
    @Column(columnDefinition = "TEXT")
    private String description;
    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;
    @Column(name = "completed_date")
    private LocalDate completedDate;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.PENDING;
    @Column(name = "sequence_order")
    private int sequenceOrder = 0;
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public long getDelayDays() {
        if (status == Status.OVERDUE && dueDate != null) {
            return ChronoUnit.DAYS.between(dueDate, LocalDate.now());
        }
        return 0;
    }


    public enum Status {
        PENDING, IN_PROGRESS, COMPLETED, OVERDUE;
    }

    public Long getId() {
        return this.id;
    }

    public Project getProject() {
        return this.project;
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

    public Status getStatus() {
        return this.status;
    }

    public int getSequenceOrder() {
        return this.sequenceOrder;
    }

    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return this.updatedAt;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public void setProject(final Project project) {
        this.project = project;
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

    public void setCompletedDate(final LocalDate completedDate) {
        this.completedDate = completedDate;
    }

    public void setStatus(final Status status) {
        this.status = status;
    }

    public void setSequenceOrder(final int sequenceOrder) {
        this.sequenceOrder = sequenceOrder;
    }

    public void setCreatedAt(final LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(final LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Milestone() {
    }
}
