package ph.trackph.milestone.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DashboardSettingsRequest {
    /** Free-form JSON string of widget configuration. */
    private String widgetConfig;
}
