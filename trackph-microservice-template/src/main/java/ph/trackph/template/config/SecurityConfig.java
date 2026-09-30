package ph.trackph.template.config;

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
        
        // Public pages
        // Auth API
        // Public project read
        // Static assets + Swagger
        // Admin routes
        // Everything else requires auth
        // Allow Thymeleaf form-based login for UI pages
        // Allow H2 console frames in dev
        http.csrf(AbstractHttpConfigurer::disable).sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS)).authorizeHttpRequests(auth -> auth.requestMatchers("/", "/login", "/register", "/error").permitAll().requestMatchers("/api/v1/auth/**").permitAll().requestMatchers(HttpMethod.GET, "/api/v1/projects").permitAll().requestMatchers(HttpMethod.GET, "/api/v1/projects/{id}").permitAll().requestMatchers("/css/**", "/js/**", "/images/**").permitAll().requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll().requestMatchers("/h2-console/**").permitAll().requestMatchers("/admin/**", "/api/v1/admin/**").hasAnyRole("ADMIN").anyRequest().authenticated()).addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class).formLogin(form -> form.loginPage("/login").loginProcessingUrl("/auth/login-form").defaultSuccessUrl("/dashboard", true).failureUrl("/login?error").permitAll()).logout(logout -> logout.logoutUrl("/auth/logout").logoutSuccessUrl("/login?logout").permitAll()).headers(h -> h.frameOptions(f -> f.sameOrigin()));
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
