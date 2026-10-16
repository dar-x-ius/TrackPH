package ph.trackph.milestone.dto.response;

import ph.trackph.milestone.model.Project;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ProjectResponse(Long id, String name, String description, Long agencyId, String agencyName, String status, LocalDate startDate, LocalDate endDate, String location, Double latitude, Double longitude, String geocodeStatus, BigDecimal budget, int milestoneCount, LocalDateTime createdAt) {

    public static ProjectResponse from(Project p) {
        return new ProjectResponse(p.getId(), p.getName(), p.getDescription(), p.getAgency().getId(), p.getAgency().getAgencyName(), p.getStatus().name(), p.getStartDate(), p.getEndDate(), p.getLocation(), p.getLatitude(), p.getLongitude(), p.getGeocodeStatus().name(), p.getBudget(), p.getMilestones().size(), p.getCreatedAt());
    }

    /**
     * Reduced view for PUBLIC role — excludes budget details. Coordinates stay in, since they're
     * no more sensitive than the location text already shown to everyone.
     */
    public static ProjectResponse publicView(Project p) {
        return new ProjectResponse(p.getId(), p.getName(), p.getDescription(), p.getAgency().getId(), p.getAgency().getAgencyName(), p.getStatus().name(), p.getStartDate(), p.getEndDate(), p.getLocation(), p.getLatitude(), p.getLongitude(), p.getGeocodeStatus().name(), null, p.getMilestones().size(), p.getCreatedAt());
    }
}
