package ph.trackph.milestone.dto.response;

import ph.trackph.milestone.model.Project;
import ph.trackph.milestone.service.StatusDeriver;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ProjectResponse(Long id, String name, String description, Long agencyId, String agencyName, String status, LocalDate startDate, LocalDate endDate, String location, Double latitude, Double longitude, String geocodeStatus, BigDecimal budget, int milestoneCount, LocalDateTime createdAt, boolean needsReview, String cancelReason, LocalDate originalEndDate, boolean endDateExtended) {

    public static ProjectResponse from(Project p) {
        return new ProjectResponse(p.getId(), p.getName(), p.getDescription(), p.getAgency().getId(), p.getAgency().getAgencyName(), p.getStatus().name(), p.getStartDate(), p.getEndDate(), p.getLocation(), p.getLatitude(), p.getLongitude(), p.getGeocodeStatus().name(), p.getBudget(), p.getMilestones().size(), p.getCreatedAt(), needsReview(p), p.getCancelReason(), p.getOriginalEndDate(), extended(p));
    }

    /**
     * Reduced view for PUBLIC role — excludes budget details. Coordinates stay in, since they're
     * no more sensitive than the location text already shown to everyone. Status, the asterisk and
     * a cancellation reason stay in too: they're the point of a transparency platform.
     */
    public static ProjectResponse publicView(Project p) {
        return new ProjectResponse(p.getId(), p.getName(), p.getDescription(), p.getAgency().getId(), p.getAgency().getAgencyName(), p.getStatus().name(), p.getStartDate(), p.getEndDate(), p.getLocation(), p.getLatitude(), p.getLongitude(), p.getGeocodeStatus().name(), null, p.getMilestones().size(), p.getCreatedAt(), needsReview(p), p.getCancelReason(), p.getOriginalEndDate(), extended(p));
    }

    /** The original end date is kept so a later, pushed-out date shows as extended. */
    private static boolean extended(Project p) {
        return p.getOriginalEndDate() != null && p.getEndDate() != null && p.getEndDate().isAfter(p.getOriginalEndDate());
    }

    private static boolean needsReview(Project p) {
        return StatusDeriver.projectNeedsReview(p.getStatus(), p.getMilestones().stream().anyMatch(MilestoneResponse::needsReview));
    }
}
