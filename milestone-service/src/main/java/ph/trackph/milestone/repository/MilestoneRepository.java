package ph.trackph.milestone.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ph.trackph.milestone.model.Milestone;

import java.time.LocalDate;
import java.util.List;

public interface MilestoneRepository extends JpaRepository<Milestone, Long> {
    List<Milestone> findByProjectIdOrderBySequenceOrderAsc(Long projectId);

    @Query("SELECT m FROM Milestone m WHERE m.dueDate < :today AND m.status NOT IN (ph.trackph.milestone.model.Milestone.Status.COMPLETED, ph.trackph.milestone.model.Milestone.Status.OVERDUE)")
    List<Milestone> findNewlyOverdue(LocalDate today);

    List<Milestone> findByProjectIdAndStatus(Long projectId, Milestone.Status status);
}
