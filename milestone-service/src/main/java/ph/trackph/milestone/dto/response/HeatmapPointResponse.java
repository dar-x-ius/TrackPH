package ph.trackph.milestone.dto.response;

import java.math.BigDecimal;

/** budget is deliberately public here: see the budget lens on the Projects map. */
public record HeatmapPointResponse(Long projectId, double latitude, double longitude, double weight, BigDecimal budget) {
}
