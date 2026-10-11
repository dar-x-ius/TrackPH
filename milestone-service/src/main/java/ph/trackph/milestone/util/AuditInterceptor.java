package ph.trackph.milestone.util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import ph.trackph.milestone.model.AuditLog;
import ph.trackph.milestone.repository.AuditLogRepository;
import ph.trackph.milestone.repository.UserRepository;
import java.util.Set;

@Component
public class AuditInterceptor implements HandlerInterceptor {
    private static final Set<String> MUTATING_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler, Exception ex) {
        if (!MUTATING_METHODS.contains(request.getMethod())) return;
        if (response.getStatus() >= 400) return;
        if (!request.getRequestURI().startsWith("/api/")) return;
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return;
        AuditLog log = new AuditLog();
        log.setAction(request.getMethod() + " " + request.getRequestURI());
        log.setIpAddress(getClientIp(request));
        log.setEntityType((String) request.getAttribute(AuditContext.ENTITY_TYPE_ATTR));
        if (request.getAttribute(AuditContext.ENTITY_ID_ATTR) instanceof Long entityId) log.setEntityId(entityId);
        log.setDetails((String) request.getAttribute(AuditContext.DETAILS_ATTR));
        // A lazy reference, not a lookup: this runs at the tail end of the request under
        // open-session-in-view, and a real findById() here was intermittently coming back
        // empty for freshly-created users even though the row existed, silently mislabeling
        // the actor as "anonymous". getReferenceById only needs the id, so it can't go stale.
        if (auth.getPrincipal() instanceof Long userId) {
            log.setUser(userRepository.getReferenceById(userId));
        }
        auditLogRepository.save(log);
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return forwarded != null ? forwarded.split(",")[0].trim() : request.getRemoteAddr();
    }

    public AuditInterceptor(final AuditLogRepository auditLogRepository, final UserRepository userRepository) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }
}
