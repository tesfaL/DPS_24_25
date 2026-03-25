package simulator;

import java.util.ArrayList;
import java.util.List;

/**
 * Thread-safe implementation of Buffer.
 * Measurements are accumulated and returned atomically via readAllAndClean().
 * Synchronization is achieved exclusively with the synchronized keyword
 * and wait/notify (no java.util.concurrent classes are used).
 */
public class MeasurementBuffer implements Buffer {

    private final List<Measurement> measurements = new ArrayList<>();

    @Override
    public synchronized void add(Measurement m) {
        measurements.add(m);
    }

    @Override
    public synchronized List<Measurement> readAllAndClean() {
        List<Measurement> copy = new ArrayList<>(measurements);
        measurements.clear();
        return copy;
    }
}
