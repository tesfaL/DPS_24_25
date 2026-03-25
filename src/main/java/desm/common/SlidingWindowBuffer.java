package desm.common;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * Thread-safe sliding-window buffer for pollution measurements.
 *
 * <p>Window size: 8 measurements, overlap factor: 50 % (step = 4).
 * When the buffer reaches 8 readings, the average is computed and stored,
 * then the oldest 4 entries are discarded (50 % overlap).
 */
public class SlidingWindowBuffer implements Buffer {

    private static final int WINDOW_SIZE = 8;
    private static final int STEP = WINDOW_SIZE / 2; // 50 % overlap

    private final Queue<Measurement> window = new LinkedList<>();
    private final List<Double> averages = new ArrayList<>();

    @Override
    public synchronized void add(Measurement m) {
        window.add(m);
        if (window.size() == WINDOW_SIZE) {
            double avg = window.stream().mapToDouble(Measurement::getValue).average().orElse(0);
            averages.add(avg);
            // Slide: discard the oldest STEP measurements
            for (int i = 0; i < STEP; i++) {
                window.poll();
            }
        }
    }

    @Override
    public synchronized List<Measurement> readAllAndClean() {
        // Return computed averages wrapped as Measurement objects with current timestamp,
        // then clear the averages list so the buffer is ready for the next window.
        List<Measurement> result = new ArrayList<>();
        long now = System.currentTimeMillis();
        for (Double avg : averages) {
            result.add(new Measurement(avg, now));
        }
        averages.clear();
        return result;
    }
}
