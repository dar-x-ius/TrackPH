package ph.trackph.milestone.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import ph.trackph.milestone.model.Project;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ProjectRequest(
        @NotBlank(message = "Project name is required") String name,
        String description,
        LocalDate startDate,
        LocalDate endDate,
        String location,
        BigDecimal budget,
        Project.Status status) {

    @AssertTrue(message = "End date must be on or after the start date")
    public boolean isDateRangeValid() {
        return startDate == null || endDate == null || !endDate.isBefore(startDate);
    }
}
