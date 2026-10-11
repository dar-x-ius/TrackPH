package ph.trackph.milestone.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

/**
 * Lets a service attach what-changed detail to the request it's handling, without becoming a
 * second place that writes AuditLog rows — AuditInterceptor is still the only writer; it just
 * reads these request attributes (if present) when it builds the row for this request.
 */
@Component
public class AuditContext {
    static final String ENTITY_TYPE_ATTR = "auditEntityType";
    static final String ENTITY_ID_ATTR = "auditEntityId";
    static final String DETAILS_ATTR = "auditDetails";

    private final HttpServletRequest request;
    private final ObjectMapper objectMapper;

    public void recordChange(String entityType, Long entityId, Object before, Object after) {
        attach(entityType, entityId, "{\"before\":" + toJson(before) + ",\"after\":" + toJson(after) + "}");
    }

    public void recordDeletion(String entityType, Long entityId, Object snapshot) {
        attach(entityType, entityId, "{\"deleted\":" + toJson(snapshot) + "}");
    }

    private void attach(String entityType, Long entityId, String details) {
        request.setAttribute(ENTITY_TYPE_ATTR, entityType);
        request.setAttribute(ENTITY_ID_ATTR, entityId);
        request.setAttribute(DETAILS_ATTR, details);
    }

    private String toJson(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (Exception e) {
            return "null";
        }
    }

    public AuditContext(final HttpServletRequest request, final ObjectMapper objectMapper) {
        this.request = request;
        this.objectMapper = objectMapper;
    }
}
