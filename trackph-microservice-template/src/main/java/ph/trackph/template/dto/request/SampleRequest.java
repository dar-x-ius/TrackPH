package ph.trackph.template.dto.request;

import jakarta.validation.constraints.NotBlank;

public record SampleRequest(@NotBlank(message = "Name is required") String name, String description) {
}
