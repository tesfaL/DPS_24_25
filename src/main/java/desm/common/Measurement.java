package desm.common;

/**
 * Represents a single CO2 pollution measurement produced by a plant's sensor.
 */
public class Measurement {

    /** CO2 emission value in grams. */
    private final double value;

    /** Timestamp in milliseconds (seconds after midnight as per the simulator). */
    private final long timestamp;

    public Measurement(double value, long timestamp) {
        this.value = value;
        this.timestamp = timestamp;
    }

    public double getValue() {
        return value;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "Measurement{value=" + value + "g, timestamp=" + timestamp + "ms}";
    }
}
