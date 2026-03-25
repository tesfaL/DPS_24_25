package desm.server;

import desm.server.model.PlantRecord;
import desm.server.model.PollutionReading;

import java.util.*;

/**
 * In-memory registry that stores active plants and their pollution readings.
 *
 * <p>All public methods are {@code synchronized} so that concurrent access
 * from the REST controller and the MQTT subscriber is safe without any
 * external locking. This fulfils the project requirement of hand-rolled
 * synchronisation using only {@code synchronized}/{@code wait}/{@code notify}.
 */
public class PlantRegistry {

    /** Plants currently registered in the network, keyed by ID. */
    private final Map<Integer, PlantRecord>   plants   = new LinkedHashMap<>();

    /** Pollution readings received from all plants, ordered by arrival time. */
    private final List<PollutionReading>      readings = new ArrayList<>();

    // ─────────────────────────────────────────────────────────────────────
    //  Plant management
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Register a new plant.
     *
     * @return the list of plants already in the network (excluding the new one),
     *         or {@code null} if the ID is already taken.
     */
    public synchronized List<PlantRecord> register(PlantRecord record) {
        if (plants.containsKey(record.getId())) {
            return null; // duplicate ID
        }
        // Snapshot before insertion so the new plant gets existing peers only
        List<PlantRecord> existing = new ArrayList<>(plants.values());
        plants.put(record.getId(), record);
        return existing;
    }

    /** Return an unmodifiable snapshot of all currently active plants. */
    public synchronized List<PlantRecord> getAll() {
        return Collections.unmodifiableList(new ArrayList<>(plants.values()));
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Pollution readings
    // ─────────────────────────────────────────────────────────────────────

    /** Store a new pollution reading received via MQTT. */
    public synchronized void addReading(PollutionReading r) {
        readings.add(r);
    }

    /**
     * Compute the average CO2 emission across all plants in the window
     * [t1, t2] (inclusive, timestamps in milliseconds).
     *
     * @return the average, or {@code -1} if no data exists for that window.
     */
    public synchronized double averageCo2(long t1, long t2) {
        double sum   = 0;
        int    count = 0;
        for (PollutionReading r : readings) {
            if (r.getTimestamp() >= t1 && r.getTimestamp() <= t2) {
                sum += r.getAverageCo2();
                count++;
            }
        }
        return count == 0 ? -1 : sum / count;
    }
}
