package server;

import com.google.gson.Gson;
import model.PollutionData;
import org.eclipse.paho.client.mqttv3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * Subscribes to the MQTT topic on which thermal power plants publish their
 * CO2 sensor averages, and stores them in the PollutionDataStore.
 */
@Service
public class MqttPollutionSubscriber {

    private static final String BROKER_URL = "tcp://localhost:1883";
    private static final String TOPIC      = "desm/pollution/data";
    private static final String CLIENT_ID  = "AdminServer-PollutionSubscriber";

    private final Gson gson = new Gson();

    @Autowired
    private PollutionDataStore pollutionDataStore;

    private MqttClient mqttClient;

    @PostConstruct
    public void start() {
        try {
            mqttClient = new MqttClient(BROKER_URL, CLIENT_ID);
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setAutomaticReconnect(true);

            mqttClient.connect(options);
            mqttClient.subscribe(TOPIC, (topic, message) -> {
                try {
                    String payload = new String(message.getPayload());
                    PollutionData data = gson.fromJson(payload, PollutionData.class);
                    pollutionDataStore.addRecord(data);
                    System.out.println("[Server] Received pollution data from plant "
                            + data.getPlantId() + ": " + data.getAverages().size() + " averages.");
                } catch (Exception e) {
                    System.err.println("[Server] Failed to parse pollution message: " + e.getMessage());
                }
            });

            System.out.println("[Server] Subscribed to MQTT topic: " + TOPIC);
        } catch (MqttException e) {
            System.err.println("[Server] WARNING: Could not connect to MQTT broker: " + e.getMessage());
            System.err.println("[Server] Pollution data will not be received until broker is available.");
        }
    }

    @PreDestroy
    public void stop() {
        if (mqttClient != null && mqttClient.isConnected()) {
            try {
                mqttClient.disconnect();
            } catch (MqttException e) {
                System.err.println("[Server] Error disconnecting MQTT: " + e.getMessage());
            }
        }
    }
}
