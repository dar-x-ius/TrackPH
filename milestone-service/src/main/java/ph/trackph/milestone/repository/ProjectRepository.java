package ph.trackph.milestone.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import ph.trackph.milestone.model.Project;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    Page<Project> findByAgencyId(Long agencyId, Pageable pageable);
    Page<Project> findByStatus(Project.Status status, Pageable pageable);
}
