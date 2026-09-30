package ph.trackph.milestone.dto.request;

import jakarta.validation.constraints.NotBlank;
import ph.trackph.milestone.model.Project;
import java.math.BigDecimal;
import java.time.LocalDate;

public class ProjectRequest {
    @NotBlank(message = "Project name is required")
    private String name;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private String location;
    private BigDecimal budget;
    private Project.Status status;

    public String getName() {
        return this.name;
    }

    public String getDescription() {
        return this.description;
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

    public Project.Status getStatus() {
        return this.status;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public void setStartDate(final LocalDate startDate) {
        this.startDate = startDate;
    }

    public void setEndDate(final LocalDate endDate) {
        this.endDate = endDate;
    }

    public void setLocation(final String location) {
        this.location = location;
    }

    public void setBudget(final BigDecimal budget) {
        this.budget = budget;
    }

    public void setStatus(final Project.Status status) {
        this.status = status;
    }
}
