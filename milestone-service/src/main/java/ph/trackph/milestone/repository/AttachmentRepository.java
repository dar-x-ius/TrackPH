package ph.trackph.milestone.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ph.trackph.milestone.model.Attachment;
import java.util.List;

public interface AttachmentRepository extends JpaRepository<Attachment, Long> {
    List<Attachment> findByMilestoneIdOrderByCreatedAtDesc(Long milestoneId);
}
