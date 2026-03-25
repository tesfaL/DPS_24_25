package plant;

import io.grpc.stub.StreamObserver;
import plant.grpc.ElectionProto.*;
import plant.grpc.ElectionServiceGrpc;

/**
 * gRPC service implementation hosted inside each Thermal Power Plant process.
 * Handles:
 *   - Chang-Roberts ring election messages
 *   - Winner announcements
 *   - New-plant notifications
 *
 * All state modifications delegate back to the owning ThermalPowerPlant so that
 * a single lock object protects the shared election state.
 */
public class ElectionServiceImpl extends ElectionServiceGrpc.ElectionServiceImplBase {

    private final ThermalPowerPlant plant;

    public ElectionServiceImpl(ThermalPowerPlant plant) {
        this.plant = plant;
    }

    @Override
    public void sendElectionMessage(ElectionMessage request,
                                    StreamObserver<Ack> responseObserver) {
        try {
            plant.onElectionMessageReceived(request);
            responseObserver.onNext(Ack.newBuilder().setSuccess(true).build());
        } catch (Exception e) {
            System.err.println("[Plant " + plant.getId() + "] Error in election message: " + e.getMessage());
            responseObserver.onNext(Ack.newBuilder().setSuccess(false).build());
        }
        responseObserver.onCompleted();
    }

    @Override
    public void announceWinner(WinnerMessage request,
                               StreamObserver<Ack> responseObserver) {
        try {
            plant.onWinnerAnnounced(request);
            responseObserver.onNext(Ack.newBuilder().setSuccess(true).build());
        } catch (Exception e) {
            System.err.println("[Plant " + plant.getId() + "] Error in winner announcement: " + e.getMessage());
            responseObserver.onNext(Ack.newBuilder().setSuccess(false).build());
        }
        responseObserver.onCompleted();
    }

    @Override
    public void notifyNewPlant(PlantMessage request,
                               StreamObserver<Ack> responseObserver) {
        try {
            plant.onNewPlantJoined(request);
            responseObserver.onNext(Ack.newBuilder().setSuccess(true).build());
        } catch (Exception e) {
            System.err.println("[Plant " + plant.getId() + "] Error in new-plant notification: " + e.getMessage());
            responseObserver.onNext(Ack.newBuilder().setSuccess(false).build());
        }
        responseObserver.onCompleted();
    }
}
