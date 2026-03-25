package desm.plant.election;

import desm.common.Config;
import desm.common.PlantInfo;
import desm.plant.grpc.PlantElectionServiceGrpc;
import desm.plant.grpc.ElectionToken;
import desm.plant.grpc.CoordinatorAnnouncement;
import desm.plant.grpc.Ack;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Chang-Roberts ring election manager.
 *
 * <h2>Algorithm summary</h2>
 * <ol>
 *   <li>On receiving an energy request each active plant generates a random
 *       bid in [0.1, 0.9] and sends an {@link ElectionToken} to its
 *       <em>clockwise</em> neighbour.</li>
 *   <li>When a plant receives a token it compares the candidate's bid with
 *       its own pending bid:
 *       <ul>
 *         <li>If the incoming bid is <em>lower</em> (or equal with a higher
 *             ID), the token is forwarded.</li>
 *         <li>If the incoming bid is <em>higher</em> the token is dropped
 *             (the local candidate is better).</li>
 *         <li>If the token carries the plant's own ID the plant has won —
 *             it broadcasts a {@link CoordinatorAnnouncement}.</li>
 *       </ul>
 *   </li>
 * </ol>
 *
 * <p>The ring is defined by the <em>sorted</em> list of active plant IDs;
 * each plant's clockwise successor is the next plant in the sorted order
 * (wrapping around). This makes insertion of new plants straightforward.
 */
public class ElectionManager {

    private final int           myId;
    private final Random        rng = new Random();

    // ── Shared state accessed by the gRPC server thread ───────────────────
    /** Neighbours sorted by ID — defines the ring order. */
    private volatile List<PlantInfo> ring;

    /** Bid currently in play for the ongoing election (null = no election). */
    private volatile Double pendingBid;

    /** Request ID of the ongoing election. */
    private volatile long pendingRequestId;

    /** Whether this plant is currently busy fulfilling a request. */
    private volatile boolean busy;

    // ── Callback ──────────────────────────────────────────────────────────
    private final WinnerCallback callback;

    public interface WinnerCallback {
        /** Called on the thread that detects the win. kwhAmount ≥ 1. */
        void onWin(int kwhAmount, double bid, long requestId);
    }

    public ElectionManager(int myId, WinnerCallback callback) {
        this.myId     = myId;
        this.callback = callback;
    }

    /** Update the ring when the list of known peers changes. */
    public synchronized void updateRing(List<PlantInfo> sortedPeers) {
        this.ring = sortedPeers;
    }

    public synchronized boolean isBusy() { return busy; }

    public synchronized void setBusy(boolean busy) { this.busy = busy; }

    // ─────────────────────────────────────────────────────────────────────
    //  Called when a new energy request arrives (MQTT thread)
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Kick off a new election for the given request.
     * Runs in the MQTT callback thread; spawns a short-lived gRPC-sender thread.
     */
    public synchronized void startElection(long requestId, int kwhAmount) {
        if (busy) {
            System.out.println("[Election] Plant #" + myId
                    + " is busy — skipping request " + requestId);
            return;
        }
        double bid = Config.BID_MIN + rng.nextDouble() * (Config.BID_MAX - Config.BID_MIN);
        pendingBid       = bid;
        pendingRequestId = requestId;

        System.out.printf("[Election] Plant #%d starting election  bid=%.4f  req=%d%n",
                myId, bid, requestId);

        ElectionToken token = ElectionToken.newBuilder()
                .setCandidateId(myId)
                .setCandidateBid(bid)
                .setRequestId(requestId)
                .build();

        forwardTokenAsync(token, kwhAmount);
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Called by the gRPC service impl (incoming messages)
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Process an incoming election token.
     *
     * @return {@link Ack} to send back to the caller.
     */
    public synchronized Ack receiveToken(ElectionToken token, int kwhAmount) {
        // Token made a full round — this plant wins.
        if (token.getCandidateId() == myId) {
            busy = true;
            System.out.printf("[Election] ✓ Plant #%d WINS  bid=%.4f  req=%d%n",
                    myId, token.getCandidateBid(), token.getRequestId());
            broadcastCoordinator(token, kwhAmount);
            return ack(true);
        }

        // Decide whether to forward or suppress the token.
        boolean forward;
        if (pendingBid == null || busy) {
            forward = true; // we are not competing
        } else {
            int cmp = Double.compare(token.getCandidateBid(), pendingBid);
            if (cmp < 0) {
                forward = true;  // incoming bid is lower → better candidate
            } else if (cmp == 0) {
                forward = token.getCandidateId() > myId; // higher ID wins ties
            } else {
                forward = false; // our bid is lower → suppress incoming
                // Inject our own token (if not already travelling)
                if (!busy && pendingBid != null) {
                    ElectionToken mine = ElectionToken.newBuilder()
                            .setCandidateId(myId)
                            .setCandidateBid(pendingBid)
                            .setRequestId(pendingRequestId)
                            .build();
                    forwardTokenAsync(mine, kwhAmount);
                }
            }
        }

        if (forward) {
            forwardTokenAsync(token, kwhAmount);
        }
        return ack(true);
    }

    /**
     * Process an incoming coordinator announcement.
     */
    public synchronized Ack receiveCoordinator(CoordinatorAnnouncement ann) {
        pendingBid = null; // election over for us
        if (ann.getWinnerId() == myId) {
            return ack(true); // already handled in receiveToken
        }
        System.out.printf("[Election] Plant #%d acknowledges winner #%d  bid=%.4f%n",
                myId, ann.getWinnerId(), ann.getWinningBid());
        return ack(true);
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Private helpers
    // ─────────────────────────────────────────────────────────────────────

    private void forwardTokenAsync(ElectionToken token, int kwhAmount) {
        PlantInfo next = nextNeighbour();
        if (next == null) {
            System.err.println("[Election] Ring is empty — cannot forward token");
            return;
        }
        new Thread(() -> {
            ManagedChannel ch = channel(next);
            try {
                PlantElectionServiceGrpc.PlantElectionServiceBlockingStub stub =
                        PlantElectionServiceGrpc.newBlockingStub(ch)
                                .withDeadlineAfter(3, TimeUnit.SECONDS);
                stub.forwardElectionToken(token);
            } catch (Exception e) {
                System.err.println("[Election] gRPC forward failed → " + next + ": " + e.getMessage());
            } finally {
                shutdownChannel(ch);
            }
        }, "grpc-fwd-" + next.getId()).start();
    }

    private void broadcastCoordinator(ElectionToken winnerToken, int kwhAmount) {
        CoordinatorAnnouncement ann = CoordinatorAnnouncement.newBuilder()
                .setWinnerId(winnerToken.getCandidateId())
                .setWinningBid(winnerToken.getCandidateBid())
                .setRequestId(winnerToken.getRequestId())
                .setKwhAmount(kwhAmount)
                .build();

        List<PlantInfo> snapshot = ring;
        if (snapshot == null) return;

        for (PlantInfo peer : snapshot) {
            if (peer.getId() == myId) continue;
            new Thread(() -> {
                ManagedChannel ch = channel(peer);
                try {
                    PlantElectionServiceGrpc.PlantElectionServiceBlockingStub stub =
                            PlantElectionServiceGrpc.newBlockingStub(ch)
                                    .withDeadlineAfter(3, TimeUnit.SECONDS);
                    stub.announceCoordinator(ann);
                } catch (Exception e) {
                    System.err.println("[Election] broadcast failed → " + peer + ": " + e.getMessage());
                } finally {
                    shutdownChannel(ch);
                }
            }, "grpc-bc-" + peer.getId()).start();
        }

        // Trigger local production
        callback.onWin(kwhAmount, winnerToken.getCandidateBid(), winnerToken.getRequestId());
    }

    /** The plant's clockwise successor in the sorted ring. */
    private PlantInfo nextNeighbour() {
        List<PlantInfo> snapshot = ring;
        if (snapshot == null || snapshot.isEmpty()) return null;

        // Find our position; next is (pos+1) % size
        for (int i = 0; i < snapshot.size(); i++) {
            if (snapshot.get(i).getId() == myId) {
                return snapshot.get((i + 1) % snapshot.size());
            }
        }
        // We are not yet in the ring (shouldn't happen)
        return snapshot.get(0);
    }

    private static ManagedChannel channel(PlantInfo p) {
        return ManagedChannelBuilder
                .forAddress(p.getAddress(), p.getPort())
                .usePlaintext()
                .build();
    }

    private static void shutdownChannel(ManagedChannel ch) {
        try { ch.shutdown().awaitTermination(2, TimeUnit.SECONDS); }
        catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
    }

    private static Ack ack(boolean ok) {
        return Ack.newBuilder().setSuccess(ok).build();
    }
}
