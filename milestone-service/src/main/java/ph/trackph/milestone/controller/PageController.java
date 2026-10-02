package ph.trackph.milestone.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/** Serves Thymeleaf pages. All data is loaded client-side via the REST API. */
@Controller
public class PageController {

    @GetMapping("/")
    public String home() { return "index"; }

    @GetMapping("/login")
    public String login() { return "auth/login"; }

    @GetMapping("/register")
    public String register() { return "auth/register"; }

    @GetMapping("/dashboard")
    public String dashboard() { return "dashboard/dashboard"; }

    @GetMapping("/projects")
    public String projectList() { return "projects/list"; }

    @GetMapping("/projects/new")
    public String projectNew() { return "projects/new"; }

    @GetMapping("/projects/{id}")
    public String projectDetail(@PathVariable Long id) { return "projects/detail"; }

    @GetMapping("/alerts")
    public String alerts() { return "alerts/list"; }

    @GetMapping("/admin/audit-logs")
    public String auditLogs() { return "admin/audit-logs"; }
}
