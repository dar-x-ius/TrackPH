package ph.trackph.template.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Serves Thymeleaf pages. Add your own service's pages here. */
@Controller
public class PageController {

    @GetMapping("/login")
    public String login() { return "auth/login"; }

    @GetMapping("/register")
    public String register() { return "auth/register"; }

    /** Living reference for every shared theme.css primitive — template-only, not synced to services. */
    @GetMapping("/style-guide")
    public String styleGuide() { return "style-guide"; }
}
