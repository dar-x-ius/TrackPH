package ph.trackph.milestone.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import ph.trackph.milestone.model.Project;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class ProjectRequest {

    @NotBlank(message = "Project name is required")
    private String name;

    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private String location;
    private BigDecimal budget;
    private Project.Status status;
}
