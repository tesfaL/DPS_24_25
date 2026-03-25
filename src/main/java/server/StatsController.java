package server;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for administration-client statistics queries.
 *
 * GET /stats?t1=<ms>&t2=<ms>  – average CO2 between two timestamps.
 */
@RestController
@RequestMapping("/stats")
public class StatsController {

    @Autowired
    private PollutionDataStore pollutionDataStore;

    /**
     * Compute the average CO2 (g) for all reports whose timestamp falls in [t1, t2].
     *
     * @param t1 start timestamp (ms since epoch, inclusive)
     * @param t2 end   timestamp (ms since epoch, inclusive)
     */
    @GetMapping
    public ResponseEntity<?> getStats(
            @RequestParam long t1,
            @RequestParam long t2) {

        if (t1 > t2) {
            return ResponseEntity.badRequest().body("t1 must be <= t2.");
        }

        Double avg = pollutionDataStore.computeAverage(t1, t2);

        Map<String, Object> response = new HashMap<>();
        response.put("t1", t1);
        response.put("t2", t2);
        if (avg == null) {
            response.put("averageCO2", null);
            response.put("message", "No data found in the specified time range.");
        } else {
            response.put("averageCO2", avg);
        }
        return ResponseEntity.ok(response);
    }
}
