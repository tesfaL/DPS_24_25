package plant.grpc;

import static io.grpc.MethodDescriptor.generateFullMethodName;
import static io.grpc.stub.ClientCalls.asyncBidiStreamingCall;
import static io.grpc.stub.ClientCalls.asyncClientStreamingCall;
import static io.grpc.stub.ClientCalls.asyncServerStreamingCall;
import static io.grpc.stub.ClientCalls.asyncUnaryCall;
import static io.grpc.stub.ClientCalls.blockingServerStreamingCall;
import static io.grpc.stub.ClientCalls.blockingUnaryCall;
import static io.grpc.stub.ClientCalls.futureUnaryCall;
import static io.grpc.stub.ServerCalls.asyncBidiStreamingCall;
import static io.grpc.stub.ServerCalls.asyncClientStreamingCall;
import static io.grpc.stub.ServerCalls.asyncServerStreamingCall;
import static io.grpc.stub.ServerCalls.asyncUnaryCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedStreamingCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall;

/**
 * <pre>
 * gRPC service that each Thermal Power Plant exposes.
 * Used for Chang-Roberts ring election and peer registration.
 * </pre>
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.25.0)",
    comments = "Source: election.proto")
public final class ElectionServiceGrpc {

  private ElectionServiceGrpc() {}

  public static final String SERVICE_NAME = "ElectionService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<plant.grpc.ElectionProto.ElectionMessage,
      plant.grpc.ElectionProto.Ack> getSendElectionMessageMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "SendElectionMessage",
      requestType = plant.grpc.ElectionProto.ElectionMessage.class,
      responseType = plant.grpc.ElectionProto.Ack.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<plant.grpc.ElectionProto.ElectionMessage,
      plant.grpc.ElectionProto.Ack> getSendElectionMessageMethod() {
    io.grpc.MethodDescriptor<plant.grpc.ElectionProto.ElectionMessage, plant.grpc.ElectionProto.Ack> getSendElectionMessageMethod;
    if ((getSendElectionMessageMethod = ElectionServiceGrpc.getSendElectionMessageMethod) == null) {
      synchronized (ElectionServiceGrpc.class) {
        if ((getSendElectionMessageMethod = ElectionServiceGrpc.getSendElectionMessageMethod) == null) {
          ElectionServiceGrpc.getSendElectionMessageMethod = getSendElectionMessageMethod =
              io.grpc.MethodDescriptor.<plant.grpc.ElectionProto.ElectionMessage, plant.grpc.ElectionProto.Ack>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "SendElectionMessage"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  plant.grpc.ElectionProto.ElectionMessage.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  plant.grpc.ElectionProto.Ack.getDefaultInstance()))
              .setSchemaDescriptor(new ElectionServiceMethodDescriptorSupplier("SendElectionMessage"))
              .build();
        }
      }
    }
    return getSendElectionMessageMethod;
  }

  private static volatile io.grpc.MethodDescriptor<plant.grpc.ElectionProto.WinnerMessage,
      plant.grpc.ElectionProto.Ack> getAnnounceWinnerMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "AnnounceWinner",
      requestType = plant.grpc.ElectionProto.WinnerMessage.class,
      responseType = plant.grpc.ElectionProto.Ack.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<plant.grpc.ElectionProto.WinnerMessage,
      plant.grpc.ElectionProto.Ack> getAnnounceWinnerMethod() {
    io.grpc.MethodDescriptor<plant.grpc.ElectionProto.WinnerMessage, plant.grpc.ElectionProto.Ack> getAnnounceWinnerMethod;
    if ((getAnnounceWinnerMethod = ElectionServiceGrpc.getAnnounceWinnerMethod) == null) {
      synchronized (ElectionServiceGrpc.class) {
        if ((getAnnounceWinnerMethod = ElectionServiceGrpc.getAnnounceWinnerMethod) == null) {
          ElectionServiceGrpc.getAnnounceWinnerMethod = getAnnounceWinnerMethod =
              io.grpc.MethodDescriptor.<plant.grpc.ElectionProto.WinnerMessage, plant.grpc.ElectionProto.Ack>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "AnnounceWinner"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  plant.grpc.ElectionProto.WinnerMessage.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  plant.grpc.ElectionProto.Ack.getDefaultInstance()))
              .setSchemaDescriptor(new ElectionServiceMethodDescriptorSupplier("AnnounceWinner"))
              .build();
        }
      }
    }
    return getAnnounceWinnerMethod;
  }

  private static volatile io.grpc.MethodDescriptor<plant.grpc.ElectionProto.PlantMessage,
      plant.grpc.ElectionProto.Ack> getNotifyNewPlantMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "NotifyNewPlant",
      requestType = plant.grpc.ElectionProto.PlantMessage.class,
      responseType = plant.grpc.ElectionProto.Ack.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<plant.grpc.ElectionProto.PlantMessage,
      plant.grpc.ElectionProto.Ack> getNotifyNewPlantMethod() {
    io.grpc.MethodDescriptor<plant.grpc.ElectionProto.PlantMessage, plant.grpc.ElectionProto.Ack> getNotifyNewPlantMethod;
    if ((getNotifyNewPlantMethod = ElectionServiceGrpc.getNotifyNewPlantMethod) == null) {
      synchronized (ElectionServiceGrpc.class) {
        if ((getNotifyNewPlantMethod = ElectionServiceGrpc.getNotifyNewPlantMethod) == null) {
          ElectionServiceGrpc.getNotifyNewPlantMethod = getNotifyNewPlantMethod =
              io.grpc.MethodDescriptor.<plant.grpc.ElectionProto.PlantMessage, plant.grpc.ElectionProto.Ack>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "NotifyNewPlant"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  plant.grpc.ElectionProto.PlantMessage.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  plant.grpc.ElectionProto.Ack.getDefaultInstance()))
              .setSchemaDescriptor(new ElectionServiceMethodDescriptorSupplier("NotifyNewPlant"))
              .build();
        }
      }
    }
    return getNotifyNewPlantMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static ElectionServiceStub newStub(io.grpc.Channel channel) {
    return new ElectionServiceStub(channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static ElectionServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    return new ElectionServiceBlockingStub(channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static ElectionServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    return new ElectionServiceFutureStub(channel);
  }

  /**
   * <pre>
   * gRPC service that each Thermal Power Plant exposes.
   * Used for Chang-Roberts ring election and peer registration.
   * </pre>
   */
  public static abstract class ElectionServiceImplBase implements io.grpc.BindableService {

    /**
     * <pre>
     * Forward an election message around the ring.
     * </pre>
     */
    public void sendElectionMessage(plant.grpc.ElectionProto.ElectionMessage request,
        io.grpc.stub.StreamObserver<plant.grpc.ElectionProto.Ack> responseObserver) {
      asyncUnimplementedUnaryCall(getSendElectionMessageMethod(), responseObserver);
    }

    /**
     * <pre>
     * Broadcast the winner of an election to all plants.
     * </pre>
     */
    public void announceWinner(plant.grpc.ElectionProto.WinnerMessage request,
        io.grpc.stub.StreamObserver<plant.grpc.ElectionProto.Ack> responseObserver) {
      asyncUnimplementedUnaryCall(getAnnounceWinnerMethod(), responseObserver);
    }

    /**
     * <pre>
     * A new plant presents itself to all existing plants.
     * </pre>
     */
    public void notifyNewPlant(plant.grpc.ElectionProto.PlantMessage request,
        io.grpc.stub.StreamObserver<plant.grpc.ElectionProto.Ack> responseObserver) {
      asyncUnimplementedUnaryCall(getNotifyNewPlantMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getSendElectionMessageMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                plant.grpc.ElectionProto.ElectionMessage,
                plant.grpc.ElectionProto.Ack>(
                  this, METHODID_SEND_ELECTION_MESSAGE)))
          .addMethod(
            getAnnounceWinnerMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                plant.grpc.ElectionProto.WinnerMessage,
                plant.grpc.ElectionProto.Ack>(
                  this, METHODID_ANNOUNCE_WINNER)))
          .addMethod(
            getNotifyNewPlantMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                plant.grpc.ElectionProto.PlantMessage,
                plant.grpc.ElectionProto.Ack>(
                  this, METHODID_NOTIFY_NEW_PLANT)))
          .build();
    }
  }

  /**
   * <pre>
   * gRPC service that each Thermal Power Plant exposes.
   * Used for Chang-Roberts ring election and peer registration.
   * </pre>
   */
  public static final class ElectionServiceStub extends io.grpc.stub.AbstractStub<ElectionServiceStub> {
    private ElectionServiceStub(io.grpc.Channel channel) {
      super(channel);
    }

    private ElectionServiceStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ElectionServiceStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new ElectionServiceStub(channel, callOptions);
    }

    /**
     * <pre>
     * Forward an election message around the ring.
     * </pre>
     */
    public void sendElectionMessage(plant.grpc.ElectionProto.ElectionMessage request,
        io.grpc.stub.StreamObserver<plant.grpc.ElectionProto.Ack> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getSendElectionMessageMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Broadcast the winner of an election to all plants.
     * </pre>
     */
    public void announceWinner(plant.grpc.ElectionProto.WinnerMessage request,
        io.grpc.stub.StreamObserver<plant.grpc.ElectionProto.Ack> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getAnnounceWinnerMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * A new plant presents itself to all existing plants.
     * </pre>
     */
    public void notifyNewPlant(plant.grpc.ElectionProto.PlantMessage request,
        io.grpc.stub.StreamObserver<plant.grpc.ElectionProto.Ack> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getNotifyNewPlantMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * <pre>
   * gRPC service that each Thermal Power Plant exposes.
   * Used for Chang-Roberts ring election and peer registration.
   * </pre>
   */
  public static final class ElectionServiceBlockingStub extends io.grpc.stub.AbstractStub<ElectionServiceBlockingStub> {
    private ElectionServiceBlockingStub(io.grpc.Channel channel) {
      super(channel);
    }

    private ElectionServiceBlockingStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ElectionServiceBlockingStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new ElectionServiceBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * Forward an election message around the ring.
     * </pre>
     */
    public plant.grpc.ElectionProto.Ack sendElectionMessage(plant.grpc.ElectionProto.ElectionMessage request) {
      return blockingUnaryCall(
          getChannel(), getSendElectionMessageMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Broadcast the winner of an election to all plants.
     * </pre>
     */
    public plant.grpc.ElectionProto.Ack announceWinner(plant.grpc.ElectionProto.WinnerMessage request) {
      return blockingUnaryCall(
          getChannel(), getAnnounceWinnerMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * A new plant presents itself to all existing plants.
     * </pre>
     */
    public plant.grpc.ElectionProto.Ack notifyNewPlant(plant.grpc.ElectionProto.PlantMessage request) {
      return blockingUnaryCall(
          getChannel(), getNotifyNewPlantMethod(), getCallOptions(), request);
    }
  }

  /**
   * <pre>
   * gRPC service that each Thermal Power Plant exposes.
   * Used for Chang-Roberts ring election and peer registration.
   * </pre>
   */
  public static final class ElectionServiceFutureStub extends io.grpc.stub.AbstractStub<ElectionServiceFutureStub> {
    private ElectionServiceFutureStub(io.grpc.Channel channel) {
      super(channel);
    }

    private ElectionServiceFutureStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ElectionServiceFutureStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new ElectionServiceFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     * Forward an election message around the ring.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<plant.grpc.ElectionProto.Ack> sendElectionMessage(
        plant.grpc.ElectionProto.ElectionMessage request) {
      return futureUnaryCall(
          getChannel().newCall(getSendElectionMessageMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Broadcast the winner of an election to all plants.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<plant.grpc.ElectionProto.Ack> announceWinner(
        plant.grpc.ElectionProto.WinnerMessage request) {
      return futureUnaryCall(
          getChannel().newCall(getAnnounceWinnerMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * A new plant presents itself to all existing plants.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<plant.grpc.ElectionProto.Ack> notifyNewPlant(
        plant.grpc.ElectionProto.PlantMessage request) {
      return futureUnaryCall(
          getChannel().newCall(getNotifyNewPlantMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_SEND_ELECTION_MESSAGE = 0;
  private static final int METHODID_ANNOUNCE_WINNER = 1;
  private static final int METHODID_NOTIFY_NEW_PLANT = 2;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final ElectionServiceImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(ElectionServiceImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_SEND_ELECTION_MESSAGE:
          serviceImpl.sendElectionMessage((plant.grpc.ElectionProto.ElectionMessage) request,
              (io.grpc.stub.StreamObserver<plant.grpc.ElectionProto.Ack>) responseObserver);
          break;
        case METHODID_ANNOUNCE_WINNER:
          serviceImpl.announceWinner((plant.grpc.ElectionProto.WinnerMessage) request,
              (io.grpc.stub.StreamObserver<plant.grpc.ElectionProto.Ack>) responseObserver);
          break;
        case METHODID_NOTIFY_NEW_PLANT:
          serviceImpl.notifyNewPlant((plant.grpc.ElectionProto.PlantMessage) request,
              (io.grpc.stub.StreamObserver<plant.grpc.ElectionProto.Ack>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }
  }

  private static abstract class ElectionServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    ElectionServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return plant.grpc.ElectionProto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("ElectionService");
    }
  }

  private static final class ElectionServiceFileDescriptorSupplier
      extends ElectionServiceBaseDescriptorSupplier {
    ElectionServiceFileDescriptorSupplier() {}
  }

  private static final class ElectionServiceMethodDescriptorSupplier
      extends ElectionServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    ElectionServiceMethodDescriptorSupplier(String methodName) {
      this.methodName = methodName;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (ElectionServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new ElectionServiceFileDescriptorSupplier())
              .addMethod(getSendElectionMessageMethod())
              .addMethod(getAnnounceWinnerMethod())
              .addMethod(getNotifyNewPlantMethod())
              .build();
        }
      }
    }
    return result;
  }
}
