package ph.trackph.milestone.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import ph.trackph.milestone.model.Project;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
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
        return new ProjectResponse(
                p.getId(), p.getName(), p.getDescription(),
                p.getAgency().getId(), p.getAgency().getAgencyName(),
                p.getStatus().name(),
                p.getStartDate(), p.getEndDate(),
                p.getLocation(), p.getBudget(),
                p.getMilestones().size(),
                p.getCreatedAt()
        );
    }

    /** Reduced view for PUBLIC role — excludes budget details. */
    public static ProjectResponse publicView(Project p) {
        return new ProjectResponse(
                p.getId(), p.getName(), p.getDescription(),
                p.getAgency().getId(), p.getAgency().getAgencyName(),
                p.getStatus().name(),
                p.getStartDate(), p.getEndDate(),
                p.getLocation(), null,
                p.getMilestones().size(),
                p.getCreatedAt()
        );
    }
}
