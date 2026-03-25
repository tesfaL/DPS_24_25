package desm.provider;

import com.google.gson.Gson;
import desm.common.Config;
import desm.common.Topics;
import org.eclipse.paho.client.mqttv3.*;

import java.util.Random;

/**
 * Renewable Energy Provider
 * ─────────────────────────
 * Every {@link Config#REQUEST_INTERVAL_MS} seconds this process generates an
 * energy request (random kWh amount between 5 000 and 15 000) and publishes
 * it on the MQTT topic {@link Topics#ENERGY_REQUEST}.
 *
 * <p>Creative touch: the provider is named after a real renewable-energy site
 * (printed in the startup banner) and logs each publication with a green ⚡
 * symbol so operators can spot it at a glance in mixed console output.
 *
 * <p>Run with: {@code java -cp <jar> desm.provider.RenewableEnergyProvider}
 */
public class RenewableEnergyProvider {

    // ── ANSI colour helpers ────────────────────────────────────────────────
    private static final String GREEN  = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String CYAN   = "\u001B[36m";
    private static final String RESET  = "\u001B[0m";

    private static final String[] SITE_NAMES = {
        "Lago di Como Hydro",
        "Val Venosta Solar Farm",
        "Etna Wind Corridor",
        "Sardinia Offshore Wind",
        "Pontine Marshes Solar Park"
    };

    private final MqttClient mqtt;
    private final Gson       gson = new Gson();
    private final Random     rng  = new Random();

    public RenewableEnergyProvider() throws MqttException {
        String clientId = "provider-" + System.currentTimeMillis();
        this.mqtt = new MqttClient(Config.MQTT_BROKER, clientId);
        MqttConnectOptions opts = new MqttConnectOptions();
        opts.setCleanSession(true);
        opts.setAutomaticReconnect(true);
        this.mqtt.connect(opts);
    }

    /** Main loop: publishes one energy request every 10 seconds. */
    public void run() throws InterruptedException {
        printBanner();
        while (true) {
            int    kwh       = Config.REQUEST_MIN_KWH
                             + rng.nextInt(Config.REQUEST_MAX_KWH - Config.REQUEST_MIN_KWH + 1);
            long   timestamp = System.currentTimeMillis();
            String payload   = gson.toJson(new EnergyRequest(kwh, timestamp));

            try {
                MqttMessage msg = new MqttMessage(payload.getBytes());
                msg.setQos(Config.MQTT_QOS);
                msg.setRetained(false);
                mqtt.publish(Topics.ENERGY_REQUEST, msg);

                System.out.printf("%s⚡ [%s] Energy request published → %d kWh%s%n",
                        GREEN, ts(timestamp), kwh, RESET);
            } catch (MqttException e) {
                System.err.println("MQTT publish error: " + e.getMessage());
            }

            Thread.sleep(Config.REQUEST_INTERVAL_MS);
        }
    }

    // ── Internal helpers ───────────────────────────────────────────────────

    private String ts(long ms) {
        return String.format("%tT", ms);
    }

    private void printBanner() {
        String site = SITE_NAMES[rng.nextInt(SITE_NAMES.length)];
        System.out.println(CYAN +
            "╔══════════════════════════════════════════════════╗\n" +
            "║      DESM — Renewable Energy Provider            ║\n" +
            "║  Site: " + padRight(site, 41) + "║\n" +
            "║  Publishing every 10 s on: " + padRight(Topics.ENERGY_REQUEST, 22) + "║\n" +
            "╚══════════════════════════════════════════════════╝" + RESET);
        System.out.println(YELLOW + "Connecting to MQTT broker at " + Config.MQTT_BROKER + RESET);
    }

    private static String padRight(String s, int len) {
        if (s.length() >= len) return s.substring(0, len);
        return s + " ".repeat(len - s.length());
    }

    // ── Entry point ────────────────────────────────────────────────────────

    public static void main(String[] args) throws Exception {
        new RenewableEnergyProvider().run();
    }

    // ── DTO ───────────────────────────────────────────────────────────────

    /** JSON-serialisable energy request sent over MQTT. */
    public static class EnergyRequest {
        public final int  kwhAmount;
        public final long timestamp;

        public EnergyRequest(int kwhAmount, long timestamp) {
            this.kwhAmount = kwhAmount;
            this.timestamp = timestamp;
        }
    }
}
