package desm.plant;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import desm.common.*;
import desm.plant.election.ElectionManager;
import desm.plant.grpc.PlantElectionServiceImpl;
import desm.plant.sensor.PollutionSensorSimulator;
import desm.provider.RenewableEnergyProvider.EnergyRequest;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.eclipse.paho.client.mqttv3.*;

import java.io.*;
import java.lang.reflect.Type;
import java.net.*;
import java.util.*;

/**
 * Thermal Power Plant process.
 *
 * <h2>Startup sequence</h2>
 * <ol>
 *   <li>Register with the administration server (REST POST).</li>
 *   <li>Receive the current list of peers from the server.</li>
 *   <li>Start the pollution-sensor simulator thread.</li>
 *   <li>Start the gRPC server (listens for peer messages).</li>
 *   <li>Introduce itself to each existing peer via gRPC.</li>
 *   <li>Subscribe to the MQTT energy-request topic.</li>
 *   <li>Start the pollution-report thread (publishes averages every 10 s).</li>
 * </ol>
 *
 * <p>Usage: {@code java -cp <jar> desm.plant.ThermalPowerPlant <id> <port> [serverPort]}
 *
 * <p>Creative touch: each plant picks a code-name from a list of Italian
 * power-plant cities and logs with a colour unique to its ID. This makes
 * mixed console output from 4–5 plants much easier to read at a glance.
 */
public class ThermalPowerPlant {

    // ── ANSI colours (one per plant slot; cycles for >8 plants) ───────────
    private static final String[] PLANT_COLOURS = {
        "\u001B[31m", // Red
        "\u001B[32m", // Green
        "\u001B[33m", // Yellow
        "\u001B[34m", // Blue
        "\u001B[35m", // Magenta
        "\u001B[36m", // Cyan
        "\u001B[91m", // Bright Red
        "\u001B[92m", // Bright Green
    };
    private static final String RESET = "\u001B[0m";

    private static final String[] CODE_NAMES = {
        "Alba", "Bergamo", "Como", "Domodossola",
        "Empoli", "Faenza", "Genova", "Hillside"
    };

    // ── Fields ─────────────────────────────────────────────────────────────
    private final int    id;
    private final int    port;
    private final String colour;
    private final String codeName;

    private final Buffer                 sensorBuffer = new SlidingWindowBuffer();
    private final ElectionManager        election;
    private final PlantElectionServiceImpl grpcService;
    private final Gson                   gson        = new Gson();

    private List<PlantInfo> peers = new ArrayList<>(); // sorted by ID

    private MqttClient mqtt;
    private Server     grpcServer;

    // ─────────────────────────────────────────────────────────────────────
    //  Constructor
    // ─────────────────────────────────────────────────────────────────────

    public ThermalPowerPlant(int id, int port) {
        this.id       = id;
        this.port     = port;
        this.colour   = PLANT_COLOURS[id % PLANT_COLOURS.length];
        this.codeName = CODE_NAMES[id % CODE_NAMES.length];

        this.election   = new ElectionManager(id, this::onWin);
        this.grpcService = new PlantElectionServiceImpl(election);
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Lifecycle
    // ─────────────────────────────────────────────────────────────────────

    public void start() throws Exception {
        printBanner();

        // 1. Register with admin server
        List<PlantInfo> existing = registerWithServer();
        log("Registered. Peers in network: " + existing.size());

        // 2. Build sorted ring (self + peers)
        synchronized (this) {
            peers = new ArrayList<>(existing);
            peers.add(new PlantInfo(id, "localhost", port));
            peers.sort(Comparator.comparingInt(PlantInfo::getId));
            election.updateRing(peers);
        }

        // 3. Start sensor simulator
        new PollutionSensorSimulator(sensorBuffer, id).start();
        log("Pollution sensor started");

        // 4. Start gRPC server
        grpcServer = ServerBuilder.forPort(port).addService(grpcService).build().start();
        log("gRPC server listening on port " + port);

        // 5. Introduce ourselves to existing peers (parallel gRPC calls)
        introduceToAll(existing);

        // 6. Connect MQTT and subscribe to energy requests
        connectMqtt();
        subscribeToEnergyRequests();

        // 7. Start pollution reporter thread
        startPollutionReporter();

        log("✅ Plant #" + id + " (" + codeName + ") is fully operational!");

        // Block until gRPC server stops
        grpcServer.awaitTermination();
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Registration
    // ─────────────────────────────────────────────────────────────────────

    private List<PlantInfo> registerWithServer() throws IOException {
        String serverUrl = "http://" + Config.SERVER_HOST + ":" + Config.SERVER_PORT
                         + "/plants/register";

        // Build JSON body
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id",      id);
        body.put("address", "localhost");
        body.put("port",    port);
        String jsonBody = gson.toJson(body);

        HttpURLConnection conn = (HttpURLConnection)
                new URL(serverUrl).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonBody.getBytes());
        }

        int code = conn.getResponseCode();
        if (code == 409) {
            throw new IllegalStateException("Plant ID " + id + " already registered!");
        }
        if (code != 200 && code != 201) {
            throw new IOException("Registration failed with HTTP " + code);
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream()))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            Type listType = new TypeToken<List<PlantInfo>>() {}.getType();
            List<PlantInfo> result = gson.fromJson(sb.toString(), listType);
            return result != null ? result : Collections.emptyList();
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    //  gRPC peer introduction (parallel)
    // ─────────────────────────────────────────────────────────────────────

    private void introduceToAll(List<PlantInfo> targets) {
        desm.plant.grpc.PlantDescriptor descriptor = desm.plant.grpc.PlantDescriptor.newBuilder()
                .setId(id)
                .setAddress("localhost")
                .setPort(port)
                .build();

        List<Thread> threads = new ArrayList<>();
        for (PlantInfo peer : targets) {
            Thread t = new Thread(() -> {
                io.grpc.ManagedChannel ch = io.grpc.ManagedChannelBuilder
                        .forAddress(peer.getAddress(), peer.getPort())
                        .usePlaintext().build();
                try {
                    desm.plant.grpc.PlantElectionServiceGrpc
                            .newBlockingStub(ch)
                            .withDeadlineAfter(3, java.util.concurrent.TimeUnit.SECONDS)
                            .introducePlant(descriptor);
                    log("Introduced to " + peer);
                } catch (Exception e) {
                    log("Introduction to " + peer + " failed: " + e.getMessage());
                } finally {
                    try { ch.shutdown().awaitTermination(2, java.util.concurrent.TimeUnit.SECONDS); }
                    catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                }
            }, "intro-" + peer.getId());
            threads.add(t);
            t.start();
        }
        // Wait for all introductions to complete
        for (Thread t : threads) {
            try { t.join(5000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    //  MQTT
    // ─────────────────────────────────────────────────────────────────────

    private void connectMqtt() throws MqttException {
        String clientId = "plant-" + id + "-" + System.currentTimeMillis();
        mqtt = new MqttClient(Config.MQTT_BROKER, clientId);
        MqttConnectOptions opts = new MqttConnectOptions();
        opts.setCleanSession(true);
        opts.setAutomaticReconnect(true);
        mqtt.connect(opts);
    }

    private void subscribeToEnergyRequests() throws MqttException {
        mqtt.subscribe(Topics.ENERGY_REQUEST, Config.MQTT_QOS, (topic, message) -> {
            String json = new String(message.getPayload());
            EnergyRequest req = gson.fromJson(json, EnergyRequest.class);
            log(String.format("⚡ Energy request received: %d kWh (req=%d)",
                    req.kwhAmount, req.timestamp));
            grpcService.setCurrentKwh(req.kwhAmount);
            election.startElection(req.timestamp, req.kwhAmount);
        });
        log("Subscribed to: " + Topics.ENERGY_REQUEST);
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Pollution reporter
    // ─────────────────────────────────────────────────────────────────────

    private void startPollutionReporter() {
        Thread reporter = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(Config.POLLUTION_SEND_INTERVAL_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }

                List<desm.common.Measurement> averages = sensorBuffer.readAllAndClean();
                if (averages.isEmpty()) continue;

                Map<String, Object> payload = new LinkedHashMap<>();
                payload.put("plantId",   id);
                payload.put("timestamp", System.currentTimeMillis());
                payload.put("averages",  averages);
                String json = gson.toJson(payload);

                try {
                    MqttMessage msg = new MqttMessage(json.getBytes());
                    msg.setQos(Config.MQTT_QOS);
                    mqtt.publish(Topics.pollution(id), msg);
                    log("📊 Sent " + averages.size() + " pollution averages to server");
                } catch (MqttException e) {
                    log("MQTT publish error (pollution): " + e.getMessage());
                }
            }
        }, "pollution-reporter-" + id);
        reporter.setDaemon(true);
        reporter.start();
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Win callback
    // ─────────────────────────────────────────────────────────────────────

    /** Called (in a gRPC thread) when this plant wins an election. */
    private void onWin(int kwhAmount, double bid, long requestId) {
        Thread t = new Thread(() -> {
            log(String.format("🏆 WON election! Producing %d kWh at $%.4f/kWh…",
                    kwhAmount, bid));
            try {
                // 1 ms per kWh as per project spec
                Thread.sleep(kwhAmount);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            election.setBusy(false);
            log("✅ Energy production complete (" + kwhAmount + " kWh)");
        }, "production-" + requestId);
        t.setDaemon(true);
        t.start();
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Logging helpers
    // ─────────────────────────────────────────────────────────────────────

    private void log(String msg) {
        System.out.printf("%s[Plant #%d %-12s]%s %s%n",
                colour, id, codeName, RESET, msg);
    }

    private void printBanner() {
        System.out.println(colour +
            "╔══════════════════════════════════════════════════╗\n" +
            "║  DESM — Thermal Power Plant                      ║\n" +
            "║  ID: " + padRight(String.valueOf(id), 44) + "║\n" +
            "║  Code-name: " + padRight(codeName, 37) + "║\n" +
            "║  gRPC port: " + padRight(String.valueOf(port), 37) + "║\n" +
            "╚══════════════════════════════════════════════════╝" + RESET);
    }

    private static String padRight(String s, int len) {
        if (s.length() >= len) return s.substring(0, len);
        return s + " ".repeat(len - s.length());
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Entry point
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Args: {@code <id> <grpcPort> [adminServerPort=8080]}
     */
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: ThermalPowerPlant <id> <grpcPort> [adminServerPort]");
            System.exit(1);
        }
        int id   = Integer.parseInt(args[0]);
        int port = Integer.parseInt(args[1]);
        if (args.length >= 3) {
            // Override default server port via system property so Config remains clean
            System.setProperty("desm.server.port", args[2]);
        }
        new ThermalPowerPlant(id, port).start();
    }
}
