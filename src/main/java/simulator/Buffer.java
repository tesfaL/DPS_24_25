package simulator;

import java.util.List;

/**
 * Buffer interface for the pollution sensor simulator.
 * Implementations must be thread-safe.
 */
public interface Buffer {
    void add(Measurement m);
    List<Measurement> readAllAndClean();
}
