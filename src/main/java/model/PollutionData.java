package model;

import java.util.List;

/**
 * Pollution data sent from a thermal power plant to the administration server via MQTT.
 * Contains the sliding-window CO2 averages accumulated since the last report.
 */
public class PollutionData {

    private int plantId;
    private long timestamp;      // ms since epoch when the report was generated
    private List<Double> averages; // CO2 averages (g) computed with sliding window

    public PollutionData() {}

    public PollutionData(int plantId, long timestamp, List<Double> averages) {
        this.plantId = plantId;
        this.timestamp = timestamp;
        this.averages = averages;
    }

    public int getPlantId() { return plantId; }
    public void setPlantId(int plantId) { this.plantId = plantId; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public List<Double> getAverages() { return averages; }
    public void setAverages(List<Double> averages) { this.averages = averages; }
}
