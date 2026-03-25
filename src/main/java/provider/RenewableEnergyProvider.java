package provider;

import com.google.gson.Gson;
import model.EnergyRequest;
import org.eclipse.paho.client.mqttv3.*;

import java.nio.charset.StandardCharsets;
import java.util.Random;

/**
 * Renewable Energy Provider process.
 *
 * Every 10 seconds publishes a randomly generated energy request (5 000–15 000 kWh)
 * to the MQTT topic {@code desm/energy/request}.
 *
 * Usage: provider
 */
public class RenewableEnergyProvider {

    private static final String BROKER_URL   = "tcp://localhost:1883";
    private static final String ENERGY_TOPIC = "desm/energy/request";
    private static final int    INTERVAL_MS  = 10_000;
    private static final int    MIN_KWH      = 5_000;
    private static final int    MAX_KWH      = 15_000;

    private final Gson gson = new Gson();
    private final Random random = new Random();

    public void start() throws MqttException, InterruptedException {
        MqttClient client = new MqttClient(BROKER_URL, "RenewableEnergyProvider");

        MqttConnectOptions opts = new MqttConnectOptions();
        opts.setCleanSession(true);
        opts.setAutomaticReconnect(true);
        client.connect(opts);
        System.out.println("[Provider] Connected to MQTT broker at " + BROKER_URL);

        while (true) {
            int kWh = MIN_KWH + random.nextInt(MAX_KWH - MIN_KWH + 1);
            long ts = System.currentTimeMillis();
            EnergyRequest request = new EnergyRequest(kWh, ts);

            String payload = gson.toJson(request);
            MqttMessage msg = new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));
            msg.setQos(1);
            msg.setRetained(false);

            client.publish(ENERGY_TOPIC, msg);
            System.out.println("[Provider] Published energy request: " + kWh + " kWh (ts=" + ts + ")");

            Thread.sleep(INTERVAL_MS);
        }
    }

    public static void main(String[] args) throws Exception {
        new RenewableEnergyProvider().start();
    }
}
