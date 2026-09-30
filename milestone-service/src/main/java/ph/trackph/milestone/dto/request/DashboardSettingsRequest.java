package ph.trackph.milestone.dto.request;

public class DashboardSettingsRequest {
    /**
     * Free-form JSON string of widget configuration.
     */
    private String widgetConfig;

    /**
     * Free-form JSON string of widget configuration.
     */
    public String getWidgetConfig() {
        return this.widgetConfig;
    }

    /**
     * Free-form JSON string of widget configuration.
     */
    public void setWidgetConfig(final String widgetConfig) {
        this.widgetConfig = widgetConfig;
    }
}
