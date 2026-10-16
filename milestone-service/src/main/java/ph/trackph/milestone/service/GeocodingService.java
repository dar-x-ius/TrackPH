package ph.trackph.milestone.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Turns a free-text location (e.g. "Quezon City") into map coordinates via Nominatim,
 * OpenStreetMap's free geocoder. No API key or billing account needed — in exchange its usage
 * policy caps public requests at 1/second and requires a descriptive User-Agent, both handled here.
 */
@Service
public class GeocodingService {
    private static final Logger log = LoggerFactory.getLogger(GeocodingService.class);
    private static final String SEARCH_URL = "https://nominatim.openstreetmap.org/search";
    private static final String USER_AGENT = "TrackPH-MilestoneService/1.0 (capstone project; government project monitoring)";

    private final RestClient restClient = RestClient.create();

    public record Coordinates(double latitude, double longitude) {}

    @SuppressWarnings("unchecked")
    public Optional<Coordinates> geocode(String location) {
        if (location == null || location.isBlank()) {
            return Optional.empty();
        }
        try {
            List<Map<String, Object>> results = restClient.get()
                    .uri(SEARCH_URL + "?format=json&limit=1&q={query}", location)
                    .header(HttpHeaders.USER_AGENT, USER_AGENT)
                    .retrieve()
                    .body(List.class);
            if (results == null || results.isEmpty()) {
                return Optional.empty();
            }
            Map<String, Object> first = results.get(0);
            double lat = Double.parseDouble((String) first.get("lat"));
            double lon = Double.parseDouble((String) first.get("lon"));
            return Optional.of(new Coordinates(lat, lon));
        } catch (Exception e) {
            log.warn("Geocoding failed for location '{}': {}", location, e.getMessage());
            return Optional.empty();
        }
    }
}
