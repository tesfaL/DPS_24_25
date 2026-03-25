package simulator;

import java.util.Random;

/**
 * Simulates a CO2 pollution sensor on a thermal power plant.
 * Periodically generates measurements and adds them to the shared buffer.
 * The timestamp is the number of seconds elapsed since midnight.
 */
public class MeasurementSimulator extends Thread {

    private static final int PERIOD_MS = 500; // produce a measurement every 500 ms

    private final Buffer buffer;
    private final Random random;
    private volatile boolean running;

    public MeasurementSimulator(Buffer buffer) {
        this.buffer = buffer;
        this.random = new Random();
        this.running = true;
        setDaemon(true); // do not block JVM exit
    }

    @Override
    public void run() {
        while (running) {
            try {
                // CO2 value in grams: realistic range for thermal plants
                double co2Value = 10.0 + random.nextDouble() * 90.0;

                // Timestamp: seconds after midnight
                long secondsAfterMidnight = (System.currentTimeMillis() / 1000L) % 86400L;

                buffer.add(new Measurement(co2Value, secondsAfterMidnight));

                Thread.sleep(PERIOD_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                running = false;
            }
        }
    }

    public void stopSimulator() {
        running = false;
        interrupt();
    }
}
