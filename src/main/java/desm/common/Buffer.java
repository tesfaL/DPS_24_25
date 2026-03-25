package desm.common;

import java.util.List;

/**
 * Buffer interface for the sliding-window pollution sensor data.
 * Implementations must be thread-safe (reads and writes can occur concurrently).
 */
public interface Buffer {

    /** Add a single measurement to the buffer. */
    void add(Measurement m);

    /**
     * Return all measurements currently in the buffer and clear it,
     * making room for new entries (sliding-window overlap semantics are
     * handled by the caller via {@code SlidingWindowBuffer}).
     */
    List<Measurement> readAllAndClean();
}
