package ph.trackph.milestone.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ph.trackph.milestone.dto.request.ProjectRequest;
import ph.trackph.milestone.dto.response.ProjectResponse;
import ph.trackph.milestone.exception.ResourceNotFoundException;
import ph.trackph.milestone.model.Project;
import ph.trackph.milestone.model.User;
import ph.trackph.milestone.repository.ProjectRepository;
import ph.trackph.milestone.repository.UserRepository;

@Service
public class ProjectService {
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

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
        return ProjectResponse.from(projectRepository.save(project));
    }

    @Transactional
    public ProjectResponse update(Long id, ProjectRequest request) {
        Project project = getOrThrow(id);
        applyRequest(project, request);
        return ProjectResponse.from(projectRepository.save(project));
    }

    @Transactional
    public void delete(Long id) {
        getOrThrow(id);
        projectRepository.deleteById(id);
    }

    private void applyRequest(Project project, ProjectRequest req) {
        project.setName(req.name());
        project.setDescription(req.description());
        project.setStartDate(req.startDate());
        project.setEndDate(req.endDate());
        project.setLocation(req.location());
        project.setBudget(req.budget());
        if (req.status() != null) project.setStatus(req.status());
    }

    private Project getOrThrow(Long id) {
        return projectRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Project", id));
    }

    public ProjectService(final ProjectRepository projectRepository, final UserRepository userRepository) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
    }
}
