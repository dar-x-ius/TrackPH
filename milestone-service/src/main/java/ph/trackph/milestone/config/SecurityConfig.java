package ph.trackph.milestone.config;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable).sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS)).authorizeHttpRequests(auth -> auth.requestMatchers("/", "/login", "/register", "/error", "/projects", "/projects/{id}", "/dashboard", "/alerts").permitAll().requestMatchers("/api/v1/auth/**").permitAll().requestMatchers(HttpMethod.GET, "/api/v1/projects/archived", "/api/v1/milestones/*/attachments/archived", "/api/v1/dashboard", "/api/v1/alerts", "/api/v1/alerts/unread-count").permitAll().requestMatchers(HttpMethod.POST, "/api/v1/projects/*/restore", "/api/v1/milestones/*/attachments/*/restore").hasAnyRole("ADMIN").requestMatchers(HttpMethod.GET, "/api/v1/projects", "/api/v1/projects/{id}", "/api/v1/projects/{projectId}/milestones").permitAll().requestMatchers("/css/**", "/js/**", "/images/**").permitAll().requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll().requestMatchers("/h2-console/**").permitAll().requestMatchers("/api/v1/dev/**").permitAll().requestMatchers("/admin/audit-logs").hasAnyRole("ADMIN", "AGENCY").requestMatchers("/admin/**", "/api/v1/admin/**").hasAnyRole("ADMIN").requestMatchers(HttpMethod.GET, "/api/v1/audit-logs").permitAll().requestMatchers("/api/v1/audit-logs/user/**", "/api/v1/audit-logs/report").hasAnyRole("ADMIN", "AGENCY").requestMatchers(HttpMethod.POST, "/api/v1/projects").hasAnyRole("AGENCY", "ADMIN").requestMatchers(HttpMethod.PUT, "/api/v1/projects/{id}").hasAnyRole("AGENCY", "ADMIN").requestMatchers(HttpMethod.DELETE, "/api/v1/projects/{id}").hasAnyRole("AGENCY", "ADMIN").requestMatchers(HttpMethod.POST, "/api/v1/projects/{projectId}/milestones").hasAnyRole("AGENCY", "ADMIN").requestMatchers(HttpMethod.PUT, "/api/v1/milestones/{id}").hasAnyRole("AGENCY", "ADMIN").requestMatchers(HttpMethod.POST, "/api/v1/milestones/*/report").hasAnyRole("AGENCY", "ADMIN").requestMatchers(HttpMethod.GET, "/api/v1/milestones/*/attachments", "/api/v1/milestones/*/attachments/*/download").permitAll().requestMatchers(HttpMethod.POST, "/api/v1/milestones/*/attachments").hasAnyRole("AGENCY", "ADMIN").requestMatchers(HttpMethod.DELETE, "/api/v1/milestones/*/attachments/*").hasAnyRole("AGENCY", "ADMIN").anyRequest().authenticated()).addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class).exceptionHandling(e -> e.authenticationEntryPoint((req, res, ex) -> {
            if (req.getRequestURI().startsWith("/api/")) {
                res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                res.setContentType("application/json");
                res.getWriter().write("{\"status\":\"error\",\"message\":\"Authentication required\"}");
            } else {
                res.sendRedirect("/login");
            }
        })).logout(logout -> logout.logoutUrl("/auth/logout").addLogoutHandler((req, res, auth) -> {
            Cookie c = new Cookie("trackph-jwt", "");
            c.setMaxAge(0);
            c.setPath("/");
            res.addCookie(c);
        }).logoutSuccessUrl("/login?logout").permitAll()).headers(h -> h.frameOptions(f -> f.sameOrigin()));
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    public SecurityConfig(final JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }
}
