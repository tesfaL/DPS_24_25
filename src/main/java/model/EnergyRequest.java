package model;

/**
 * Energy production request published by the Renewable Energy Provider via MQTT.
 */
public class EnergyRequest {

    private int kWh;
    private long timestamp;

    public EnergyRequest() {}

    public EnergyRequest(int kWh, long timestamp) {
        this.kWh = kWh;
        this.timestamp = timestamp;
    }

    public int getKWh() { return kWh; }
    public void setKWh(int kWh) { this.kWh = kWh; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    @Override
    public String toString() {
        return "EnergyRequest{kWh=" + kWh + ", timestamp=" + timestamp + "}";
    }
}
