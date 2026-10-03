package ph.trackph.milestone.dto.response;

import java.util.List;

public record AuditActivityReportResponse(List<ActionCount> actionCounts, List<UserActivity> topUsers) {

    public record ActionCount(Long userId, String username, String action, long count) {
    }

    public record UserActivity(Long userId, String username, long totalActions) {
    }
}
