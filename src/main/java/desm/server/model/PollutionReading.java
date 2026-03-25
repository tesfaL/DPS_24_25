package desm.server.model;

/**
 * A single CO2 reading forwarded from a plant via MQTT.
 */
public class PollutionReading {

    private final int    plantId;
    private final long   timestamp;
    private final double averageCo2;

    public PollutionReading(int plantId, long timestamp, double averageCo2) {
        this.plantId    = plantId;
        this.timestamp  = timestamp;
        this.averageCo2 = averageCo2;
    }

    public int    getPlantId()    { return plantId; }
    public long   getTimestamp()  { return timestamp; }
    public double getAverageCo2() { return averageCo2; }
}
