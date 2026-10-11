package ph.trackph.milestone.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ph.trackph.milestone.model.Attachment;
import java.util.List;

public interface AttachmentRepository extends JpaRepository<Attachment, Long> {
    List<Attachment> findByMilestoneIdOrderByCreatedAtDesc(Long milestoneId);

    @Modifying
    @Query(value = "UPDATE milestone_attachments SET deleted_at = NULL WHERE milestone_id IN (SELECT id FROM milestones WHERE project_id = :projectId) AND deleted_at IS NOT NULL", nativeQuery = true)
    void restoreArchivedByProjectId(@Param("projectId") Long projectId);

    @Modifying
    @Query(value = "UPDATE milestone_attachments SET deleted_at = NULL WHERE id = :id AND milestone_id = :milestoneId AND deleted_at IS NOT NULL", nativeQuery = true)
    int restoreById(@Param("id") Long id, @Param("milestoneId") Long milestoneId);
}
