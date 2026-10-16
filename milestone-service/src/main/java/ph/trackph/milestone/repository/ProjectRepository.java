package ph.trackph.milestone.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ph.trackph.milestone.model.Project;
import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    Page<Project> findByAgencyId(Long agencyId, Pageable pageable);
    long countByStatus(Project.Status status);
    Page<Project> findByStatus(Project.Status status, Pageable pageable);
    List<Project> findByGeocodeStatus(Project.GeocodeStatus geocodeStatus, Pageable pageable);
    List<Project> findByGeocodeStatusAndLatitudeIsNotNullAndLongitudeIsNotNull(Project.GeocodeStatus geocodeStatus);

    @Modifying
    @Query(value = "UPDATE projects SET deleted_at = NULL WHERE id = :id AND deleted_at IS NOT NULL", nativeQuery = true)
    int restoreById(@Param("id") Long id);
}
