package ph.trackph.milestone.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ph.trackph.milestone.model.Project;
import ph.trackph.milestone.repository.ProjectRepository;
import ph.trackph.milestone.service.GeocodingService;
import java.util.List;
import java.util.Optional;

/**
 * Picks up projects whose location text hasn't been turned into map coordinates yet
 * and geocodes them in the background, so saving a project never has to wait on an
 * external API call. Calls are spaced ~1/second to stay within Nominatim's public
 * usage policy (its public server has no API key, so it's rate-limited instead).
 */
@Component
public class GeocodingScheduler {
    private static final Logger log = LoggerFactory.getLogger(GeocodingScheduler.class);
    private static final int BATCH_SIZE = 20;
    private static final long THROTTLE_MS = 1100;

    private final ProjectRepository projectRepository;
    private final GeocodingService geocodingService;

    /**
     * Not @Transactional: the throttling sleep between calls would otherwise hold a DB
     * transaction (and connection) open for the whole batch. Geocoding happens first with no
     * transaction open, then the results are saved in one short, separate write.
     */
    @Scheduled(fixedRate = 60_000)
    public void geocodePendingProjects() {
        List<Project> pending = projectRepository.findByGeocodeStatus(Project.GeocodeStatus.PENDING, PageRequest.of(0, BATCH_SIZE));
        if (pending.isEmpty()) {
            return;
        }
        for (int i = 0; i < pending.size(); i++) {
            Project project = pending.get(i);
            Optional<GeocodingService.Coordinates> coords = geocodingService.geocode(project.getLocation());
            if (coords.isPresent()) {
                project.setLatitude(coords.get().latitude());
                project.setLongitude(coords.get().longitude());
                project.setGeocodeStatus(Project.GeocodeStatus.DONE);
            } else {
                project.setGeocodeStatus(Project.GeocodeStatus.FAILED);
            }
            if (i < pending.size() - 1) {
                sleepThrottle();
            }
        }
        projectRepository.saveAll(pending);
        log.info("Geocoded {} project(s)", pending.size());
    }

    private void sleepThrottle() {
        try {
            Thread.sleep(THROTTLE_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public GeocodingScheduler(final ProjectRepository projectRepository, final GeocodingService geocodingService) {
        this.projectRepository = projectRepository;
        this.geocodingService = geocodingService;
    }
}
