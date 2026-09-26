package ph.trackph.milestone.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ph.trackph.milestone.model.DashboardConfig;

import java.util.Optional;

public interface DashboardConfigRepository extends JpaRepository<DashboardConfig, Long> {
    Optional<DashboardConfig> findByUserId(Long userId);
}
