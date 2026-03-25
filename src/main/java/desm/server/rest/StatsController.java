package desm.server.rest;

import desm.server.PlantRegistry;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST endpoints for pollution statistics, consumed by the administration client.
 *
 * <pre>
 *  GET /stats/co2?t1=&lt;ms&gt;&amp;t2=&lt;ms&gt;  — average CO2 in [t1, t2]
 * </pre>
 */
@RestController
@RequestMapping("/stats")
public class StatsController {

    private final PlantRegistry registry;

    public StatsController(PlantRegistry registry) {
        this.registry = registry;
    }

    /**
     * Return the average CO2 emission across all plants between {@code t1}
     * and {@code t2} (both in milliseconds since the Unix epoch).
     *
     * <p>Response JSON:
     * <pre>{ "t1": 1234, "t2": 5678, "averageCo2": 215.3 }</pre>
     * or {@code 404} when no data exists for that window.
     */
    @GetMapping("/co2")
    public ResponseEntity<?> averageCo2(@RequestParam long t1,
                                        @RequestParam long t2) {
        if (t1 > t2) {
            return ResponseEntity.badRequest().body("t1 must be ≤ t2");
        }
        double avg = registry.averageCo2(t1, t2);
        if (avg < 0) {
            return ResponseEntity.status(404).body("No data for the given time window");
        }
        return ResponseEntity.ok(Map.of("t1", t1, "t2", t2, "averageCo2", avg));
    }
}
