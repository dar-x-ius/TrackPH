package ph.trackph.milestone.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import ph.trackph.milestone.dto.response.AlertResponse;
import ph.trackph.milestone.exception.ResourceNotFoundException;
import ph.trackph.milestone.model.Alert;
import ph.trackph.milestone.model.Milestone;
import ph.trackph.milestone.model.Project;
import ph.trackph.milestone.repository.AlertRepository;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class AlertService {
    private final AlertRepository alertRepository;
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(0L);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));
        return emitter;
    }

    private void broadcast(AlertResponse alert) {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("alert").data(alert));
            } catch (IOException e) {
                emitters.remove(emitter);
            }
        }
    }

    public Page<AlertResponse> findAll(Pageable pageable) {
        return alertRepository.findAllByOrderByTriggeredAtDesc(pageable).map(AlertResponse::from);
    }

    public long countUnread() {
        return alertRepository.countByReadFalse();
    }

    @Transactional
    public AlertResponse markRead(Long id) {
        Alert alert = alertRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Alert", id));
        alert.setRead(true);
        return AlertResponse.from(alertRepository.save(alert));
    }

    @Transactional
    public int markAllRead() {
        return alertRepository.markAllRead();
    }

    @Transactional
    public void createAlert(Project project, Milestone milestone, Alert.AlertType type, String message, Alert.Severity severity) {
        // deduplicate: skip if same project+milestone+type already exists unread
        if (milestone != null && alertRepository.existsByProjectIdAndMilestoneIdAndType(project.getId(), milestone.getId(), type)) {
            return;
        }
        Alert alert = new Alert();
        alert.setProject(project);
        alert.setMilestone(milestone);
        alert.setType(type);
        alert.setMessage(message);
        alert.setSeverity(severity);
        Alert saved = alertRepository.save(alert);
        broadcast(AlertResponse.from(saved));
    }

    public static Alert.Severity severityFromDelayDays(long days) {
        if (days >= 15) return Alert.Severity.CRITICAL;
        if (days >= 8) return Alert.Severity.HIGH;
        if (days >= 4) return Alert.Severity.MEDIUM;
        return Alert.Severity.LOW;
    }

    public AlertService(final AlertRepository alertRepository) {
        this.alertRepository = alertRepository;
    }
}
