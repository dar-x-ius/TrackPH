package ph.trackph.milestone.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ph.trackph.milestone.dto.request.DashboardSettingsRequest;
import ph.trackph.milestone.dto.response.DashboardResponse;
import ph.trackph.milestone.exception.ResourceNotFoundException;
import ph.trackph.milestone.model.DashboardConfig;
import ph.trackph.milestone.model.Project;
import ph.trackph.milestone.repository.AlertRepository;
import ph.trackph.milestone.repository.DashboardConfigRepository;
import ph.trackph.milestone.repository.ProjectRepository;
import ph.trackph.milestone.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final DashboardConfigRepository dashboardConfigRepository;
    private final ProjectRepository projectRepository;
    private final AlertRepository alertRepository;
    private final UserRepository userRepository;

    public DashboardResponse getDashboard(Long userId) {
        DashboardConfig config = dashboardConfigRepository.findByUserId(userId)
                .orElseGet(() -> defaultConfig(userId));

        long total = projectRepository.count();
        long inProgress = projectRepository.findByStatus(Project.Status.IN_PROGRESS, org.springframework.data.domain.Pageable.unpaged()).getTotalElements();
        long delayed = projectRepository.findByStatus(Project.Status.DELAYED, org.springframework.data.domain.Pageable.unpaged()).getTotalElements();
        long unread = alertRepository.countByReadFalse();

        return new DashboardResponse(userId, config.getWidgetConfig(), total, inProgress, delayed, unread);
    }

    @Transactional
    public DashboardResponse updateSettings(Long userId, DashboardSettingsRequest request) {
        DashboardConfig config = dashboardConfigRepository.findByUserId(userId)
                .orElseGet(() -> defaultConfig(userId));
        config.setWidgetConfig(request.getWidgetConfig());
        dashboardConfigRepository.save(config);
        return getDashboard(userId);
    }

    private DashboardConfig defaultConfig(Long userId) {
        DashboardConfig config = new DashboardConfig();
        userRepository.findById(userId).ifPresent(config::setUser);
        config.setWidgetConfig("{\"widgets\":[\"projects\",\"alerts\",\"milestones\"]}");
        return dashboardConfigRepository.save(config);
    }
}
