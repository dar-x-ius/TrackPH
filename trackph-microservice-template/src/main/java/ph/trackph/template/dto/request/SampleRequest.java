package ph.trackph.template.dto.request;

import jakarta.validation.constraints.NotBlank;

public class SampleRequest {
    @NotBlank(message = "Name is required")
    private String name;
    private String description;

    public String getName() {
        return this.name;
    }

    public String getDescription() {
        return this.description;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public void setDescription(final String description) {
        this.description = description;
    }
}
