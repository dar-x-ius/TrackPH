package ph.trackph.milestone.config;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Supplies the brand name and footer tagline to every Thymeleaf page. Edit the two constants for your service. */
@ControllerAdvice
public class BrandConfig {
    private static final String APP_NAME = "TrackPH Milestones";
    private static final String APP_TAGLINE = "Milestone & Timeline Monitoring";

    @ModelAttribute("appName")
    public String appName() {
        return APP_NAME;
    }

    @ModelAttribute("appTagline")
    public String appTagline() {
        return APP_TAGLINE;
    }
}
