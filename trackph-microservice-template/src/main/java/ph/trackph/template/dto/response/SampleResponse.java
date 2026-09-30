package ph.trackph.template.dto.response;

import ph.trackph.template.model.Sample;
import java.time.LocalDateTime;

public class SampleResponse {
    private Long id;
    private String name;
    private String description;
    private LocalDateTime createdAt;

    public static SampleResponse from(Sample sample) {
        return new SampleResponse(sample.getId(), sample.getName(), sample.getDescription(), sample.getCreatedAt());
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

    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    public SampleResponse(final Long id, final String name, final String description, final LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.createdAt = createdAt;
    }
}
