package ph.trackph.template.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ph.trackph.template.dto.request.SampleRequest;
import ph.trackph.template.dto.response.SampleResponse;
import ph.trackph.template.exception.ResourceNotFoundException;
import ph.trackph.template.model.Sample;
import ph.trackph.template.repository.SampleRepository;
import java.util.List;

@Service
public class SampleService {
    private final SampleRepository repository;

    public List<SampleResponse> findAll() {
        return repository.findAll().stream().map(SampleResponse::from).toList();
    }

    public SampleResponse findById(Long id) {
        return SampleResponse.from(getOrThrow(id));
    }

    @Transactional
    public SampleResponse create(SampleRequest request) {
        Sample entity = new Sample();
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        return SampleResponse.from(repository.save(entity));
    }

    @Transactional
    public SampleResponse update(Long id, SampleRequest request) {
        Sample entity = getOrThrow(id);
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        return SampleResponse.from(repository.save(entity));
    }

    @Transactional
    public void delete(Long id) {
        getOrThrow(id);
        repository.deleteById(id);
    }

    private Sample getOrThrow(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Sample", id));
    }

    public SampleService(final SampleRepository repository) {
        this.repository = repository;
    }
}
