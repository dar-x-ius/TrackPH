package ph.trackph.template.dto.response;

import ph.trackph.template.model.Sample;
import java.time.LocalDateTime;

public record SampleResponse(Long id, String name, String description, LocalDateTime createdAt) {

    public static SampleResponse from(Sample sample) {
        return new SampleResponse(sample.getId(), sample.getName(), sample.getDescription(), sample.getCreatedAt());
    }
}
