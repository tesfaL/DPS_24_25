package plant;

import com.google.gson.Gson;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import model.EnergyRequest;
import model.PlantInfo;
import model.PollutionData;
import org.eclipse.paho.client.mqttv3.*;
import plant.grpc.ElectionProto.*;
import plant.grpc.ElectionServiceGrpc;
import simulator.MeasurementBuffer;
import simulator.MeasurementSimulator;
import simulator.Measurement;

import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Thermal Power Plant process.
 *
 * Responsibilities:
 *  1. Register with the Administration Server via REST.
 *  2. Start a gRPC server for peer communication (Chang-Roberts election).
 *  3. Present itself to all existing peers.
 *  4. Subscribe to the MQTT energy-request topic.
 *  5. Run a pollution sensor simulator and report averages every 10 s via MQTT.
 *  6. Participate in Chang-Roberts ring elections to decide which plant fulfils
 *     each energy request.
 *
 * Synchronization: all shared election / ring state is protected by the
 * intrinsic lock of this object (synchronized methods/blocks only – no
 * java.util.concurrent classes are used).
 */
public class ThermalPowerPlant {

    // ── MQTT ──────────────────────────────────────────────────────────────────
    private static final String BROKER_URL       = "tcp://localhost:1883";
    private static final String ENERGY_TOPIC     = "desm/energy/request";
    private static final String POLLUTION_TOPIC  = "desm/pollution/data";

    // ── Sliding window parameters ──────────────────────────────────────────────
    private static final int WINDOW_SIZE = 8;
    private static final int WINDOW_STEP = 4; // 50 % overlap

    // ── Identity ──────────────────────────────────────────────────────────────
    private final int id;
    private final int port;
    private final String adminHost;
    private final int adminPort;

    // ── Ring / peer state (guarded by 'this') ─────────────────────────────────
    private final List<PlantInfo> ring = new ArrayList<>(); // sorted by ID

    // ── Election state (guarded by 'this') ────────────────────────────────────
    private boolean busy            = false;
    private boolean electionActive  = false;
    private boolean isCandidate     = false;
    private double  myBid           = Double.MAX_VALUE;
    private long    currentRequestTs = -1;
    private int     currentKWh       = 0;

    // ── Utilities ─────────────────────────────────────────────────────────────
    private final Gson gson = new Gson();
    private final Random random = new Random();

    private Server grpcServer;
    private MqttClient mqttClient;
    private MeasurementBuffer measurementBuffer;
    private MeasurementSimulator simulator;

    // ── Constructor ───────────────────────────────────────────────────────────

    public ThermalPowerPlant(int id, int port, String adminHost, int adminPort) {
        this.id = id;
        this.port = port;
        this.adminHost = adminHost;
        this.adminPort = adminPort;
    }

    public int getId() { return id; }

    // ══════════════════════════════════════════════════════════════════════════
    //  Startup sequence
    // ══════════════════════════════════════════════════════════════════════════

    public void start() throws Exception {
        System.out.println("[Plant " + id + "] Starting on port " + port + ".");

        // 1. Start gRPC server
        grpcServer = ServerBuilder.forPort(port)
                .addService(new ElectionServiceImpl(this))
                .build()
                .start();
        System.out.println("[Plant " + id + "] gRPC server listening on port " + port + ".");

        // 2. Register with administration server
        List<PlantInfo> existingPlants = registerWithAdminServer();
        System.out.println("[Plant " + id + "] Registered. Existing peers: " + existingPlants.size());

        // 3. Build local ring (including self)
        synchronized (this) {
            for (PlantInfo p : existingPlants) {
                ring.add(p);
            }
            ring.add(new PlantInfo(id, "localhost", port));
            sortRing();
        }

        // 4. Start pollution sensor simulator
        measurementBuffer = new MeasurementBuffer();
        simulator = new MeasurementSimulator(measurementBuffer);
        simulator.start();

        // 5. Present ourselves to existing peers (parallel gRPC calls)
        presentToAllPeers(existingPlants);

        // 6. Connect MQTT and subscribe to energy requests
        connectMqtt();

        // 7. Schedule periodic pollution report (every 10 s)
        startPollutionReporter();

        System.out.println("[Plant " + id + "] Fully initialised and ready.");

        // Keep the main thread alive (gRPC server runs on background threads)
        grpcServer.awaitTermination();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Administration Server – REST registration
    // ══════════════════════════════════════════════════════════════════════════

    @SuppressWarnings("unchecked")
    private List<PlantInfo> registerWithAdminServer() throws Exception {
        String urlStr = "http://" + adminHost + ":" + adminPort + "/plants";
        String body = gson.toJson(new PlantInfo(id, "localhost", port));

        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        conn.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));

        int status = conn.getResponseCode();
        if (status == 409) {
            throw new IllegalStateException("Plant with ID " + id + " already registered.");
        }
        if (status != 200) {
            throw new RuntimeException("Registration failed with HTTP " + status);
        }

        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) sb.append(line);
        br.close();

        PlantInfo[] arr = gson.fromJson(sb.toString(), PlantInfo[].class);
        return arr == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(arr));
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  gRPC peer notifications
    // ══════════════════════════════════════════════════════════════════════════

    /** Present this plant to all given peers in parallel. */
    private void presentToAllPeers(List<PlantInfo> peers) {
        if (peers.isEmpty()) return;

        PlantMessage msg = PlantMessage.newBuilder()
                .setId(id).setAddress("localhost").setPort(port).build();

        List<Thread> threads = new ArrayList<>();
        for (PlantInfo peer : peers) {
            Thread t = new Thread(() -> {
                try {
                    sendNotifyNewPlant(peer, msg);
                } catch (Exception e) {
                    System.err.println("[Plant " + id + "] Could not notify peer " + peer.getId()
                            + ": " + e.getMessage());
                }
            });
            threads.add(t);
            t.start();
        }
        for (Thread t : threads) {
            try { t.join(); } catch (InterruptedException ignored) {}
        }
    }

    private void sendNotifyNewPlant(PlantInfo peer, PlantMessage msg) {
        ManagedChannel ch = ManagedChannelBuilder.forAddress(peer.getAddress(), peer.getPort())
                .usePlaintext().build();
        try {
            ElectionServiceGrpc.ElectionServiceBlockingStub stub =
                    ElectionServiceGrpc.newBlockingStub(ch)
                            .withDeadlineAfter(5, TimeUnit.SECONDS);
            stub.notifyNewPlant(msg);
        } finally {
            ch.shutdown();
        }
    }

    /** Broadcast winner message to all known peers in parallel. */
    private void broadcastWinner(WinnerMessage msg) {
        List<PlantInfo> peers;
        synchronized (this) {
            peers = new ArrayList<>(ring);
        }
        List<Thread> threads = new ArrayList<>();
        for (PlantInfo peer : peers) {
            if (peer.getId() == id) continue;
            Thread t = new Thread(() -> {
                try {
                    ManagedChannel ch = ManagedChannelBuilder
                            .forAddress(peer.getAddress(), peer.getPort())
                            .usePlaintext().build();
                    try {
                        ElectionServiceGrpc.ElectionServiceBlockingStub stub =
                                ElectionServiceGrpc.newBlockingStub(ch)
                                        .withDeadlineAfter(5, TimeUnit.SECONDS);
                        stub.announceWinner(msg);
                    } finally {
                        ch.shutdown();
                    }
                } catch (Exception e) {
                    System.err.println("[Plant " + id + "] Could not send winner to "
                            + peer.getId() + ": " + e.getMessage());
                }
            });
            threads.add(t);
            t.start();
        }
        for (Thread t : threads) {
            try { t.join(); } catch (InterruptedException ignored) {}
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Ring management (guarded by 'this')
    // ══════════════════════════════════════════════════════════════════════════

    private void sortRing() {
        ring.sort(Comparator.comparingInt(PlantInfo::getId));
    }

    /** Return the ring successor of this plant (next plant in sorted ring). */
    private synchronized PlantInfo getSuccessor() {
        if (ring.size() <= 1) return null;
        for (int i = 0; i < ring.size(); i++) {
            if (ring.get(i).getId() == id) {
                return ring.get((i + 1) % ring.size());
            }
        }
        return null;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  MQTT – energy requests & pollution publishing
    // ══════════════════════════════════════════════════════════════════════════

    private void connectMqtt() throws MqttException {
        String clientId = "Plant-" + id + "-" + System.currentTimeMillis();
        mqttClient = new MqttClient(BROKER_URL, clientId);

        MqttConnectOptions opts = new MqttConnectOptions();
        opts.setCleanSession(true);
        opts.setAutomaticReconnect(true);
        mqttClient.connect(opts);

        mqttClient.subscribe(ENERGY_TOPIC, (topic, message) -> {
            try {
                String payload = new String(message.getPayload());
                EnergyRequest req = gson.fromJson(payload, EnergyRequest.class);
                onEnergyRequestReceived(req);
            } catch (Exception e) {
                System.err.println("[Plant " + id + "] Failed to parse energy request: " + e.getMessage());
            }
        });

        System.out.println("[Plant " + id + "] Subscribed to MQTT topic: " + ENERGY_TOPIC);
    }

    private void startPollutionReporter() {
        Thread reporter = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(10_000);
                    reportPollution();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        reporter.setDaemon(true);
        reporter.start();
    }

    private void reportPollution() {
        List<Measurement> measurements = measurementBuffer.readAllAndClean();
        List<Double> averages = computeSlidingWindowAverages(measurements);

        if (averages.isEmpty()) return; // nothing to report

        PollutionData data = new PollutionData(id, System.currentTimeMillis(), averages);
        String payload = gson.toJson(data);
        try {
            MqttMessage msg = new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));
            msg.setQos(1);
            mqttClient.publish(POLLUTION_TOPIC, msg);
            System.out.println("[Plant " + id + "] Published " + averages.size()
                    + " CO2 average(s) to server.");
        } catch (MqttException e) {
            System.err.println("[Plant " + id + "] Failed to publish pollution data: " + e.getMessage());
        }
    }

    /** Apply sliding window (size=8, step=4) and return per-window CO2 averages. */
    private List<Double> computeSlidingWindowAverages(List<Measurement> measurements) {
        List<Double> averages = new ArrayList<>();
        for (int i = 0; i + WINDOW_SIZE <= measurements.size(); i += WINDOW_STEP) {
            double sum = 0;
            for (int j = i; j < i + WINDOW_SIZE; j++) {
                sum += measurements.get(j).getValue();
            }
            averages.add(sum / WINDOW_SIZE);
        }
        return averages;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Energy request handling – Chang-Roberts election
    // ══════════════════════════════════════════════════════════════════════════

    /** Called (on MQTT callback thread) when an energy request arrives. */
    private synchronized void onEnergyRequestReceived(EnergyRequest req) {
        if (busy) {
            System.out.println("[Plant " + id + "] Busy – ignoring request ts=" + req.getTimestamp());
            return;
        }
        if (electionActive) {
            System.out.println("[Plant " + id + "] Election already active – ignoring request ts=" + req.getTimestamp());
            return;
        }
        if (ring.isEmpty()) return;

        System.out.println("[Plant " + id + "] Energy request received: " + req.getKWh() + " kWh (ts=" + req.getTimestamp() + ")");

        // Generate bid
        myBid = 0.1 + random.nextDouble() * 0.8; // [0.1, 0.9]
        electionActive = true;
        isCandidate = true;
        currentRequestTs = req.getTimestamp();
        currentKWh = req.getKWh();

        System.out.println("[Plant " + id + "] Starting election with bid=" + String.format("%.4f", myBid));

        PlantInfo successor = getSuccessor();
        if (successor == null) {
            // Only plant in the ring – we win immediately
            System.out.println("[Plant " + id + "] Sole plant – winning election by default.");
            handleElectionWin();
            return;
        }

        ElectionMessage msg = ElectionMessage.newBuilder()
                .setPrice(myBid)
                .setPlantId(id)
                .setRequestTs(currentRequestTs)
                .setKWh(currentKWh)
                .build();

        forwardElectionMessage(successor, msg);
    }

    /**
     * Called by ElectionServiceImpl when a ring election message arrives.
     * Implements the Chang-Roberts forwarding rules.
     */
    public synchronized void onElectionMessageReceived(ElectionMessage msg) {
        double  p  = msg.getPrice();
        int     id2 = msg.getPlantId();

        // If this message belongs to a stale election, ignore it
        if (electionActive && msg.getRequestTs() != currentRequestTs) {
            System.out.println("[Plant " + id + "] Stale election message (ts=" + msg.getRequestTs() + ") – ignoring.");
            return;
        }

        // If we are not yet in an election, this message starts one for us
        if (!electionActive) {
            electionActive   = true;
            isCandidate      = true;
            currentRequestTs = msg.getRequestTs();
            currentKWh       = msg.getKWh();
            myBid            = busy ? Double.MAX_VALUE : (0.1 + random.nextDouble() * 0.8);
            System.out.println("[Plant " + id + "] Joining election (ts=" + currentRequestTs
                    + ") with bid=" + String.format("%.4f", myBid));
        }

        PlantInfo successor = getSuccessor();
        if (successor == null) return; // ring has only us

        if (isCandidate) {
            // Compare incoming bid with our own
            if (isBetterBid(p, id2, myBid, id)) {
                // Incoming is better – forward it, become passive
                isCandidate = false;
                forwardElectionMessage(successor, msg);
            } else if (p == myBid && id2 == id) {
                // Our own message completed the circuit – we win!
                handleElectionWin();
            }
            // else: our bid is better – discard incoming (our message already sent)
        } else {
            // We are passive or busy – just forward
            forwardElectionMessage(successor, msg);
        }
    }

    /**
     * Returns true if bid (p1, id1) is strictly better than (p2, id2).
     * Better = lower price; tie-break = higher plant ID.
     */
    private boolean isBetterBid(double p1, int id1, double p2, int id2) {
        if (p1 < p2) return true;
        if (p1 == p2) return id1 > id2;
        return false;
    }

    private void handleElectionWin() {
        busy          = true;
        electionActive = false;
        isCandidate   = false;
        int kWh       = currentKWh;
        long reqTs    = currentRequestTs;

        System.out.println("[Plant " + id + "] WON the election! Fulfilling " + kWh + " kWh.");

        WinnerMessage winnerMsg = WinnerMessage.newBuilder()
                .setWinnerId(id)
                .setRequestTs(reqTs)
                .setKWh(kWh)
                .build();

        // Broadcast winner (in a separate thread to avoid holding the lock)
        new Thread(() -> broadcastWinner(winnerMsg)).start();

        // Produce energy (sleep kWh ms) in a separate thread
        new Thread(() -> {
            try {
                System.out.println("[Plant " + id + "] Producing energy for " + kWh + " ms...");
                Thread.sleep(kWh);
                System.out.println("[Plant " + id + "] Energy production complete.");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                synchronized (this) {
                    busy = false;
                }
            }
        }).start();
    }

    /** Called by ElectionServiceImpl when the winner is announced. */
    public synchronized void onWinnerAnnounced(WinnerMessage msg) {
        System.out.println("[Plant " + id + "] Winner announced: Plant " + msg.getWinnerId()
                + " (ts=" + msg.getRequestTs() + ")");
        // Reset election state for non-winners
        if (msg.getWinnerId() != id) {
            electionActive = false;
            isCandidate    = false;
        }
    }

    /** Called by ElectionServiceImpl when a new plant presents itself. */
    public synchronized void onNewPlantJoined(PlantMessage msg) {
        // Add to ring if not already present
        for (PlantInfo p : ring) {
            if (p.getId() == msg.getId()) return;
        }
        ring.add(new PlantInfo(msg.getId(), msg.getAddress(), msg.getPort()));
        sortRing();
        System.out.println("[Plant " + id + "] New plant joined: Plant " + msg.getId()
                + ". Ring size: " + ring.size());
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Helper – forward election message via gRPC (non-blocking, best effort)
    // ══════════════════════════════════════════════════════════════════════════

    private void forwardElectionMessage(PlantInfo successor, ElectionMessage msg) {
        new Thread(() -> {
            ManagedChannel ch = ManagedChannelBuilder
                    .forAddress(successor.getAddress(), successor.getPort())
                    .usePlaintext().build();
            try {
                ElectionServiceGrpc.ElectionServiceBlockingStub stub =
                        ElectionServiceGrpc.newBlockingStub(ch)
                                .withDeadlineAfter(5, TimeUnit.SECONDS);
                stub.sendElectionMessage(msg);
            } catch (Exception e) {
                System.err.println("[Plant " + id + "] Could not forward election msg to "
                        + successor.getId() + ": " + e.getMessage());
            } finally {
                ch.shutdown();
            }
        }).start();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  main
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Launch a Thermal Power Plant process.
     * Usage: plant <id> <port> <adminHost> <adminPort>
     */
    public static void main(String[] args) throws Exception {
        if (args.length < 4) {
            System.err.println("Usage: plant <id> <port> <adminHost> <adminPort>");
            System.exit(1);
        }
        int id        = Integer.parseInt(args[0]);
        int port      = Integer.parseInt(args[1]);
        String host   = args[2];
        int adminPort = Integer.parseInt(args[3]);

        new ThermalPowerPlant(id, port, host, adminPort).start();
    }
}
