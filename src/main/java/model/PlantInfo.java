package model;

/**
 * Represents a thermal power plant's registration info (ID, address, gRPC port).
 */
public class PlantInfo {

    private int id;
    private String address;
    private int port;

    public PlantInfo() {}

    public PlantInfo(int id, String address, int port) {
        this.id = id;
        this.address = address;
        this.port = port;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }

    @Override
    public String toString() {
        return "PlantInfo{id=" + id + ", address='" + address + "', port=" + port + "}";
    }
}
