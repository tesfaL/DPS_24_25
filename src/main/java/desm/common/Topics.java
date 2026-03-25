package desm.common;

/**
 * MQTT topic names used across the DESM system.
 *
 * <p>Centralising them here prevents typos and makes refactoring painless.
 */
public final class Topics {

    private Topics() {}

    /** Renewable energy provider → thermal plants (energy requests). */
    public static final String ENERGY_REQUEST = "desm/energy/request";

    /**
     * Thermal plants → administration server (pollution averages).
     * Each plant publishes on {@code desm/pollution/<plantId>}.
     */
    public static String pollution(int plantId) {
        return "desm/pollution/" + plantId;
    }

    /** Wildcard used by the server to subscribe to all plant pollution topics. */
    public static final String POLLUTION_WILDCARD = "desm/pollution/+";
}
