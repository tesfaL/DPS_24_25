package desm.common;

/**
 * Shared constants for the entire DESM application.
 */
public final class Config {

    private Config() {}

    // ── MQTT ──────────────────────────────────────────────────────────────────
    public static final String MQTT_BROKER      = "tcp://localhost:1883";
    public static final int    MQTT_QOS         = 2;

    // ── Administration server ─────────────────────────────────────────────────
    public static final String SERVER_HOST      = "localhost";
    public static final int    SERVER_PORT      = 8080;

    // ── Renewable energy provider ─────────────────────────────────────────────
    /** kWh interval between energy publications (ms). */
    public static final long   REQUEST_INTERVAL_MS = 10_000L;
    public static final int    REQUEST_MIN_KWH     = 5_000;
    public static final int    REQUEST_MAX_KWH     = 15_000;

    // ── Thermal power plant ───────────────────────────────────────────────────
    public static final double BID_MIN = 0.1;
    public static final double BID_MAX = 0.9;

    /** Pollution data is pushed to the server every 10 seconds. */
    public static final long   POLLUTION_SEND_INTERVAL_MS = 10_000L;

    // ── Sliding-window sensor buffer ──────────────────────────────────────────
    public static final int WINDOW_SIZE   = 8;
    public static final int WINDOW_OVERLAP = 4;  // 50 % of WINDOW_SIZE
}
