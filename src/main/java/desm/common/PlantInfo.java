package desm.common;

/**
 * Immutable description of a thermal power plant as known to the registry.
 */
public class PlantInfo {

    private final int    id;
    private final String address;
    private final int    port;

    public PlantInfo(int id, String address, int port) {
        this.id      = id;
        this.address = address;
        this.port    = port;
    }

    public int    getId()      { return id; }
    public String getAddress() { return address; }
    public int    getPort()    { return port; }

    /** Convenience: gRPC target string understood by ManagedChannelBuilder. */
    public String grpcTarget() { return address + ":" + port; }

    @Override
    public String toString() {
        return "Plant#" + id + "@" + address + ":" + port;
    }
}
