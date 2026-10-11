package ph.trackph.milestone.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ph.trackph.milestone.model.AuditLog;
import java.time.LocalDateTime;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    Page<AuditLog> findByUserId(Long userId, Pageable pageable);
    Page<AuditLog> findByEntityTypeAndEntityId(String entityType, Long entityId, Pageable pageable);

    // Account activity (sign-in/registration) and marking an alert read are personal, not
    // project/milestone activity — keep them out of the public trail. Full detail stays in findAll().
    @Query("SELECT a FROM AuditLog a WHERE a.action NOT LIKE '%/api/v1/auth/%' AND a.action NOT LIKE '%/alerts/%read%'")
    Page<AuditLog> findAllPublic(Pageable pageable);

    @Query("SELECT a.user.id, a.user.username, a.action, COUNT(a) FROM AuditLog a " +
            "WHERE a.createdAt BETWEEN :from AND :to GROUP BY a.user.id, a.user.username, a.action")
    List<Object[]> countByUserAndActionBetween(LocalDateTime from, LocalDateTime to);
}
