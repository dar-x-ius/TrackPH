package ph.trackph.milestone.dto.response;

import ph.trackph.milestone.model.Project;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class ProjectResponse {
    private Long id;
    private String name;
    private String description;
    private Long agencyId;
    private String agencyName;
    private String status;
    private LocalDate startDate;
    private LocalDate endDate;
    private String location;
    private BigDecimal budget;
    private int milestoneCount;
    private LocalDateTime createdAt;

    public static ProjectResponse from(Project p) {
        return new ProjectResponse(p.getId(), p.getName(), p.getDescription(), p.getAgency().getId(), p.getAgency().getAgencyName(), p.getStatus().name(), p.getStartDate(), p.getEndDate(), p.getLocation(), p.getBudget(), p.getMilestones().size(), p.getCreatedAt());
    }

    /**
     * Reduced view for PUBLIC role — excludes budget details.
     */
    public static ProjectResponse publicView(Project p) {
        return new ProjectResponse(p.getId(), p.getName(), p.getDescription(), p.getAgency().getId(), p.getAgency().getAgencyName(), p.getStatus().name(), p.getStartDate(), p.getEndDate(), p.getLocation(), null, p.getMilestones().size(), p.getCreatedAt());
    }

    public Long getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public String getDescription() {
        return this.description;
    }

    public Long getAgencyId() {
        return this.agencyId;
    }

    public String getAgencyName() {
        return this.agencyName;
    }

    public String getStatus() {
        return this.status;
    }

    public LocalDate getStartDate() {
        return this.startDate;
    }

    public LocalDate getEndDate() {
        return this.endDate;
    }

    public String getLocation() {
        return this.location;
    }

    public BigDecimal getBudget() {
        return this.budget;
    }

    public int getMilestoneCount() {
        return this.milestoneCount;
    }

    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    public ProjectResponse(final Long id, final String name, final String description, final Long agencyId, final String agencyName, final String status, final LocalDate startDate, final LocalDate endDate, final String location, final BigDecimal budget, final int milestoneCount, final LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.agencyId = agencyId;
        this.agencyName = agencyName;
        this.status = status;
        this.startDate = startDate;
        this.endDate = endDate;
        this.location = location;
        this.budget = budget;
        this.milestoneCount = milestoneCount;
        this.createdAt = createdAt;
    }
}
