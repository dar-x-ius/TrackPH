package ph.trackph.milestone.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ph.trackph.milestone.dto.request.ProjectRequest;
import ph.trackph.milestone.dto.response.ProjectResponse;
import ph.trackph.milestone.exception.ResourceNotFoundException;
import ph.trackph.milestone.exception.UnauthorizedException;
import ph.trackph.milestone.model.Attachment;
import ph.trackph.milestone.model.Milestone;
import ph.trackph.milestone.model.Project;
import ph.trackph.milestone.model.User;
import ph.trackph.milestone.repository.AttachmentRepository;
import ph.trackph.milestone.repository.MilestoneRepository;
import ph.trackph.milestone.repository.ProjectRepository;
import ph.trackph.milestone.repository.UserRepository;
import ph.trackph.milestone.util.AuditContext;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class ProjectService {
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final AttachmentRepository attachmentRepository;
    private final MilestoneRepository milestoneRepository;
    private final AuditContext auditContext;
    private final JdbcTemplate jdbcTemplate;

    public Page<ProjectResponse> findAll(Pageable pageable) {
        return projectRepository.findAll(pageable).map(ProjectResponse::from);
    }

    public Page<ProjectResponse> findAllPublic(Pageable pageable) {
        return projectRepository.findAll(pageable).map(ProjectResponse::publicView);
    }

    public ProjectResponse findById(Long id) {
        return ProjectResponse.from(getOrThrow(id));
    }

    @Transactional
    public ProjectResponse create(Long agencyUserId, ProjectRequest request) {
        User agency = userRepository.findById(agencyUserId).orElseThrow(() -> new ResourceNotFoundException("User", agencyUserId));
        Project project = new Project();
        applyRequest(project, request);
        project.setAgency(agency);
        ProjectResponse created = ProjectResponse.from(projectRepository.save(project));
        auditContext.recordChange("Project", created.id(), null, created);
        return created;
    }

    @Transactional
    public ProjectResponse update(Long id, ProjectRequest request, Long requesterId, boolean requesterIsAdmin) {
        Project project = getOrThrow(id);
        requireOwnerOrAdmin(project, requesterId, requesterIsAdmin);
        ProjectResponse before = ProjectResponse.from(project);
        applyRequest(project, request);
        ProjectResponse after = ProjectResponse.from(projectRepository.save(project));
        auditContext.recordChange("Project", id, before, after);
        return after;
    }

    /**
     * Soft delete, cascaded by hand to milestones/attachments (the project→milestone JPA cascade
     * would otherwise still issue a real DELETE) — so the record and any attached evidence survive
     * even if someone tries to remove it, and the public audit trail can still show what it looked
     * like right before deletion.
     */
    @Transactional
    public void delete(Long id, Long requesterId, boolean requesterIsAdmin) {
        Project project = getOrThrow(id);
        requireOwnerOrAdmin(project, requesterId, requesterIsAdmin);
        ProjectResponse snapshot = ProjectResponse.from(project);

        LocalDateTime now = LocalDateTime.now();
        int attachmentCount = 0;
        for (Milestone milestone : project.getMilestones()) {
            List<Attachment> attachments = attachmentRepository.findByMilestoneIdOrderByCreatedAtDesc(milestone.getId());
            attachments.forEach(a -> a.setDeletedAt(now));
            attachmentCount += attachments.size();
            milestone.setDeletedAt(now);
        }
        project.setDeletedAt(now);
        projectRepository.save(project);

        auditContext.recordDeletion("Project", id, Map.of(
                "project", snapshot,
                "milestonesArchived", project.getMilestones().size(),
                "attachmentsArchived", attachmentCount));
    }

    /**
     * Raw SQL, not a repository method — @SQLRestriction would hide these rows from any
     * JPA-level query, including a hand-written JPQL/native one mapped back to the entity.
     */
    public List<Map<String, Object>> findArchived() {
        return jdbcTemplate.queryForList(
                "SELECT id, name, status, deleted_at AS deletedAt FROM projects WHERE deleted_at IS NOT NULL ORDER BY deleted_at DESC");
    }

    @Transactional
    public void restore(Long id) {
        if (projectRepository.restoreById(id) == 0) {
            throw new ResourceNotFoundException("Project", id);
        }
        milestoneRepository.restoreArchivedByProjectId(id);
        attachmentRepository.restoreArchivedByProjectId(id);
        auditContext.recordChange("Project", id, null, findById(id));
    }

    private void requireOwnerOrAdmin(Project project, Long requesterId, boolean requesterIsAdmin) {
        if (!requesterIsAdmin && !project.getAgency().getId().equals(requesterId)) {
            throw new UnauthorizedException("Only the owning agency or an admin can modify this project");
        }
    }

    private void applyRequest(Project project, ProjectRequest req) {
        project.setName(req.name());
        if (req.description() != null) project.setDescription(req.description());
        if (req.startDate() != null) project.setStartDate(req.startDate());
        if (req.endDate() != null) project.setEndDate(req.endDate());
        if (req.location() != null) project.setLocation(req.location());
        if (req.budget() != null) project.setBudget(req.budget());
        if (req.status() != null) project.setStatus(req.status());
    }

    private Project getOrThrow(Long id) {
        return projectRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Project", id));
    }

    public ProjectService(final ProjectRepository projectRepository, final UserRepository userRepository, final AttachmentRepository attachmentRepository, final MilestoneRepository milestoneRepository, final AuditContext auditContext, final JdbcTemplate jdbcTemplate) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.attachmentRepository = attachmentRepository;
        this.milestoneRepository = milestoneRepository;
        this.auditContext = auditContext;
        this.jdbcTemplate = jdbcTemplate;
    }
}
