package ph.trackph.template.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ph.trackph.template.model.Sample;

public interface SampleRepository extends JpaRepository<Sample, Long> {
}
