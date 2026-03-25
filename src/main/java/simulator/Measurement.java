package simulator;

/**
 * Represents a single CO2 pollution measurement produced by the sensor simulator.
 */
public class Measurement {

    private final double value;     // CO2 value in grams
    private final long timestamp;   // seconds after midnight

    public Measurement(double value, long timestamp) {
        this.value = value;
        this.timestamp = timestamp;
    }

    public double getValue() {
        return value;
    }

    /** Seconds elapsed since midnight at the time of measurement. */
    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "Measurement{value=" + value + ", timestamp=" + timestamp + "}";
    }
}
