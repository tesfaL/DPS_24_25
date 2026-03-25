package desm.common;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * Thread-safe sliding-window buffer for pollution measurements.
 *
 * <p>Window size: {@link Config#WINDOW_SIZE} measurements,
 * overlap factor: 50 % (step = {@link Config#WINDOW_OVERLAP}).
 * When the buffer reaches {@link Config#WINDOW_SIZE} readings,
 * the average is computed and stored, then the oldest
 * {@link Config#WINDOW_OVERLAP} entries are discarded.
 */
public class SlidingWindowBuffer implements Buffer {

    private final Queue<Measurement> window = new LinkedList<>();
    private final List<Double>       averages = new ArrayList<>();

    @Override
    public synchronized void add(Measurement m) {
        window.add(m);
        if (window.size() == Config.WINDOW_SIZE) {
            double avg = window.stream().mapToDouble(Measurement::getValue).average().orElse(0);
            averages.add(avg);
            // Slide: discard the oldest WINDOW_OVERLAP measurements (50 % overlap)
            for (int i = 0; i < Config.WINDOW_OVERLAP; i++) {
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
