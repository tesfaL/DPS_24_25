package desm.server.mqtt;

import com.google.gson.*;
import desm.common.Config;
import desm.common.Topics;
import desm.server.PlantRegistry;
import desm.server.model.PollutionReading;
import org.eclipse.paho.client.mqttv3.*;

import java.util.List;

/**
 * MQTT subscriber that runs inside the administration server.
 *
 * <p>It subscribes to the wildcard topic {@link Topics#POLLUTION_WILDCARD}
 * so it receives pollution-average payloads from every plant, then stores
 * them in the {@link PlantRegistry} for later querying.
 */
public class ServerMqttSubscriber {

    private final PlantRegistry registry;
    private final Gson          gson = new Gson();
    private MqttClient          mqtt;

    public ServerMqttSubscriber(PlantRegistry registry) {
        this.registry = registry;
    }

    /** Connect and start subscribing. Runs in the calling thread. */
    public void connect() throws MqttException {
        String clientId = "admin-server-" + System.currentTimeMillis();
        mqtt = new MqttClient(Config.MQTT_BROKER, clientId);

        MqttConnectOptions opts = new MqttConnectOptions();
        opts.setCleanSession(true);
        opts.setAutomaticReconnect(true);
        mqtt.connect(opts);

        mqtt.subscribe(Topics.POLLUTION_WILDCARD, Config.MQTT_QOS, this::onMessage);
        System.out.println("[Server MQTT] Subscribed to: " + Topics.POLLUTION_WILDCARD);
    }

    // ─────────────────────────────────────────────────────────────────────

    private void onMessage(String topic, MqttMessage message) {
        try {
            JsonObject obj = JsonParser.parseString(new String(message.getPayload()))
                    .getAsJsonObject();

            int  plantId   = obj.get("plantId").getAsInt();
            long timestamp = obj.get("timestamp").getAsLong();

            JsonArray avgs = obj.getAsJsonArray("averages");
            for (JsonElement el : avgs) {
                // Each element is a Measurement JSON object
                JsonObject m   = el.getAsJsonObject();
                double     co2 = m.get("value").getAsDouble();
                registry.addReading(new PollutionReading(plantId, timestamp, co2));
            }
            System.out.printf("[Server MQTT] Received %d readings from plant #%d%n",
                    avgs.size(), plantId);
        } catch (Exception e) {
            System.err.println("[Server MQTT] Parse error: " + e.getMessage());
        }
    }
}
