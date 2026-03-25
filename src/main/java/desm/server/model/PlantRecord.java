package desm.server.model;

/**
 * Registry entry for a thermal power plant.
 */
public class PlantRecord {

    private final int    id;
    private final String address;
    private final int    port;

    public PlantRecord(int id, String address, int port) {
        this.id      = id;
        this.address = address;
        this.port    = port;
    }

    public int    getId()      { return id; }
    public String getAddress() { return address; }
    public int    getPort()    { return port; }
}
