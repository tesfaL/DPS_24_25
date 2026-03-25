package desm.plant.grpc;

import desm.plant.election.ElectionManager;
import io.grpc.stub.StreamObserver;

/**
 * gRPC service implementation — handles messages sent by peer plants.
 *
 * <p>This class is the entry point for all inter-plant communication.
 * It delegates business logic to {@link ElectionManager}, keeping the
 * transport layer cleanly separated from the algorithm.
 */
public class PlantElectionServiceImpl extends PlantElectionServiceGrpc.PlantElectionServiceImplBase {

    private final ElectionManager election;

    // We keep a volatile reference so the MQTT thread can update it before a
    // new election token arrives via gRPC.
    private volatile int currentKwh;

    public PlantElectionServiceImpl(ElectionManager election) {
        this.election = election;
    }

    /** Called by the MQTT thread so gRPC knows how many kWh the ongoing request is for. */
    public void setCurrentKwh(int kwh) { this.currentKwh = kwh; }

    // ─────────────────────────────────────────────────────────────────────
    //  gRPC method implementations
    // ─────────────────────────────────────────────────────────────────────

    @Override
    public void forwardElectionToken(ElectionToken request,
                                     StreamObserver<Ack> responseObserver) {
        Ack reply = election.receiveToken(request, currentKwh);
        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }

    @Override
    public void announceCoordinator(CoordinatorAnnouncement request,
                                    StreamObserver<Ack> responseObserver) {
        Ack reply = election.receiveCoordinator(request);
        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }

    @Override
    public void introducePlant(PlantDescriptor request,
                               StreamObserver<Ack> responseObserver) {
        // The plant will be picked up naturally via the admin-server registry;
        // we acknowledge receipt so the new plant knows the ring is reachable.
        System.out.printf("[gRPC] Plant #%d introduced itself (port %d)%n",
                request.getId(), request.getPort());
        responseObserver.onNext(Ack.newBuilder().setSuccess(true).build());
        responseObserver.onCompleted();
    }
}
