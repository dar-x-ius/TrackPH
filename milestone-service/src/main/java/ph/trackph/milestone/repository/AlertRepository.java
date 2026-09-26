package ph.trackph.milestone.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import ph.trackph.milestone.model.Alert;

public interface AlertRepository extends JpaRepository<Alert, Long> {
    Page<Alert> findByProjectIdOrderByTriggeredAtDesc(Long projectId, Pageable pageable);
    Page<Alert> findAllByOrderByTriggeredAtDesc(Pageable pageable);
    long countByReadFalse();
    boolean existsByProjectIdAndMilestoneIdAndType(Long projectId, Long milestoneId, Alert.AlertType type);
}
