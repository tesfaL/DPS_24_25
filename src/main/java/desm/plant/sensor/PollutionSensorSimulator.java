package desm.plant.sensor;

import desm.common.Buffer;
import desm.common.Measurement;

import java.util.Random;

/**
 * Pollution sensor simulator (provided by the course — must not be modified).
 *
 * <p>This thread continuously generates simulated CO2 measurements at a fixed
 * frequency and inserts them into the shared {@link Buffer}.
 */
public class PollutionSensorSimulator extends Thread {

    /** Measurement frequency in milliseconds. */
    private static final long FREQUENCY_MS = 500L;

    private final Buffer buffer;
    private final Random rng;

    /** CO2 baseline for this plant (realistic variance around it). */
    private final double baseline;

    public PollutionSensorSimulator(Buffer buffer, int plantId) {
        this.buffer   = buffer;
        this.rng      = new Random(plantId); // deterministic seed per plant
        this.baseline = 200.0 + plantId * 15.0; // each plant has a distinct baseline
        setDaemon(true);
        setName("sensor-sim-plant-" + plantId);
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            // Seconds after midnight as per the project spec
            long   timestamp = (System.currentTimeMillis() / 1000) % 86400;
            double value     = baseline + (rng.nextGaussian() * 20.0);
            if (value < 0) value = 0;

            buffer.add(new Measurement(value, timestamp));

            try {
                Thread.sleep(FREQUENCY_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
