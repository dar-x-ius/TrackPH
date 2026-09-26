package ph.trackph.template.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import ph.trackph.template.model.Sample;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class SampleResponse {

    private Long id;
    private String name;
    private String description;
    private LocalDateTime createdAt;

    public static SampleResponse from(Sample sample) {
        return new SampleResponse(
                sample.getId(),
                sample.getName(),
                sample.getDescription(),
                sample.getCreatedAt()
        );
    }
}
