package rocksdb_service;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 * <pre>
 * The RocksDB service definition
 * </pre>
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.58.0)",
    comments = "Source: rocksdb_service.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class RocksDbServiceGrpc {

  private RocksDbServiceGrpc() {}

  public static final java.lang.String SERVICE_NAME = "rocksdb_service.RocksDbService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<rocksdb_service.RocksdbService.PutRequest,
      rocksdb_service.RocksdbService.PutResponse> getPutMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "Put",
      requestType = rocksdb_service.RocksdbService.PutRequest.class,
      responseType = rocksdb_service.RocksdbService.PutResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<rocksdb_service.RocksdbService.PutRequest,
      rocksdb_service.RocksdbService.PutResponse> getPutMethod() {
    io.grpc.MethodDescriptor<rocksdb_service.RocksdbService.PutRequest, rocksdb_service.RocksdbService.PutResponse> getPutMethod;
    if ((getPutMethod = RocksDbServiceGrpc.getPutMethod) == null) {
      synchronized (RocksDbServiceGrpc.class) {
        if ((getPutMethod = RocksDbServiceGrpc.getPutMethod) == null) {
          RocksDbServiceGrpc.getPutMethod = getPutMethod =
              io.grpc.MethodDescriptor.<rocksdb_service.RocksdbService.PutRequest, rocksdb_service.RocksdbService.PutResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "Put"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  rocksdb_service.RocksdbService.PutRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  rocksdb_service.RocksdbService.PutResponse.getDefaultInstance()))
              .setSchemaDescriptor(new RocksDbServiceMethodDescriptorSupplier("Put"))
              .build();
        }
      }
    }
    return getPutMethod;
  }

  private static volatile io.grpc.MethodDescriptor<rocksdb_service.RocksdbService.GetRequest,
      rocksdb_service.RocksdbService.GetResponse> getGetMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "Get",
      requestType = rocksdb_service.RocksdbService.GetRequest.class,
      responseType = rocksdb_service.RocksdbService.GetResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<rocksdb_service.RocksdbService.GetRequest,
      rocksdb_service.RocksdbService.GetResponse> getGetMethod() {
    io.grpc.MethodDescriptor<rocksdb_service.RocksdbService.GetRequest, rocksdb_service.RocksdbService.GetResponse> getGetMethod;
    if ((getGetMethod = RocksDbServiceGrpc.getGetMethod) == null) {
      synchronized (RocksDbServiceGrpc.class) {
        if ((getGetMethod = RocksDbServiceGrpc.getGetMethod) == null) {
          RocksDbServiceGrpc.getGetMethod = getGetMethod =
              io.grpc.MethodDescriptor.<rocksdb_service.RocksdbService.GetRequest, rocksdb_service.RocksdbService.GetResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "Get"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  rocksdb_service.RocksdbService.GetRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  rocksdb_service.RocksdbService.GetResponse.getDefaultInstance()))
              .setSchemaDescriptor(new RocksDbServiceMethodDescriptorSupplier("Get"))
              .build();
        }
      }
    }
    return getGetMethod;
  }

  private static volatile io.grpc.MethodDescriptor<rocksdb_service.RocksdbService.MultiGetRequest,
      rocksdb_service.RocksdbService.MultiGetResponse> getMultiGetMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "MultiGet",
      requestType = rocksdb_service.RocksdbService.MultiGetRequest.class,
      responseType = rocksdb_service.RocksdbService.MultiGetResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<rocksdb_service.RocksdbService.MultiGetRequest,
      rocksdb_service.RocksdbService.MultiGetResponse> getMultiGetMethod() {
    io.grpc.MethodDescriptor<rocksdb_service.RocksdbService.MultiGetRequest, rocksdb_service.RocksdbService.MultiGetResponse> getMultiGetMethod;
    if ((getMultiGetMethod = RocksDbServiceGrpc.getMultiGetMethod) == null) {
      synchronized (RocksDbServiceGrpc.class) {
        if ((getMultiGetMethod = RocksDbServiceGrpc.getMultiGetMethod) == null) {
          RocksDbServiceGrpc.getMultiGetMethod = getMultiGetMethod =
              io.grpc.MethodDescriptor.<rocksdb_service.RocksdbService.MultiGetRequest, rocksdb_service.RocksdbService.MultiGetResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "MultiGet"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  rocksdb_service.RocksdbService.MultiGetRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  rocksdb_service.RocksdbService.MultiGetResponse.getDefaultInstance()))
              .setSchemaDescriptor(new RocksDbServiceMethodDescriptorSupplier("MultiGet"))
              .build();
        }
      }
    }
    return getMultiGetMethod;
  }

  private static volatile io.grpc.MethodDescriptor<rocksdb_service.RocksdbService.GetByPrefixRequest,
      rocksdb_service.RocksdbService.GetByPrefixResponse> getGetByPrefixMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetByPrefix",
      requestType = rocksdb_service.RocksdbService.GetByPrefixRequest.class,
      responseType = rocksdb_service.RocksdbService.GetByPrefixResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<rocksdb_service.RocksdbService.GetByPrefixRequest,
      rocksdb_service.RocksdbService.GetByPrefixResponse> getGetByPrefixMethod() {
    io.grpc.MethodDescriptor<rocksdb_service.RocksdbService.GetByPrefixRequest, rocksdb_service.RocksdbService.GetByPrefixResponse> getGetByPrefixMethod;
    if ((getGetByPrefixMethod = RocksDbServiceGrpc.getGetByPrefixMethod) == null) {
      synchronized (RocksDbServiceGrpc.class) {
        if ((getGetByPrefixMethod = RocksDbServiceGrpc.getGetByPrefixMethod) == null) {
          RocksDbServiceGrpc.getGetByPrefixMethod = getGetByPrefixMethod =
              io.grpc.MethodDescriptor.<rocksdb_service.RocksdbService.GetByPrefixRequest, rocksdb_service.RocksdbService.GetByPrefixResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetByPrefix"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  rocksdb_service.RocksdbService.GetByPrefixRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  rocksdb_service.RocksdbService.GetByPrefixResponse.getDefaultInstance()))
              .setSchemaDescriptor(new RocksDbServiceMethodDescriptorSupplier("GetByPrefix"))
              .build();
        }
      }
    }
    return getGetByPrefixMethod;
  }

  private static volatile io.grpc.MethodDescriptor<rocksdb_service.RocksdbService.DeleteRequest,
      rocksdb_service.RocksdbService.DeleteResponse> getDeleteMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "Delete",
      requestType = rocksdb_service.RocksdbService.DeleteRequest.class,
      responseType = rocksdb_service.RocksdbService.DeleteResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<rocksdb_service.RocksdbService.DeleteRequest,
      rocksdb_service.RocksdbService.DeleteResponse> getDeleteMethod() {
    io.grpc.MethodDescriptor<rocksdb_service.RocksdbService.DeleteRequest, rocksdb_service.RocksdbService.DeleteResponse> getDeleteMethod;
    if ((getDeleteMethod = RocksDbServiceGrpc.getDeleteMethod) == null) {
      synchronized (RocksDbServiceGrpc.class) {
        if ((getDeleteMethod = RocksDbServiceGrpc.getDeleteMethod) == null) {
          RocksDbServiceGrpc.getDeleteMethod = getDeleteMethod =
              io.grpc.MethodDescriptor.<rocksdb_service.RocksdbService.DeleteRequest, rocksdb_service.RocksdbService.DeleteResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "Delete"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  rocksdb_service.RocksdbService.DeleteRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  rocksdb_service.RocksdbService.DeleteResponse.getDefaultInstance()))
              .setSchemaDescriptor(new RocksDbServiceMethodDescriptorSupplier("Delete"))
              .build();
        }
      }
    }
    return getDeleteMethod;
  }

  private static volatile io.grpc.MethodDescriptor<rocksdb_service.RocksdbService.ReplicationEntriesRequest,
      rocksdb_service.RocksdbService.ReplicationEntriesResponse> getGetReplicationEntriesMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetReplicationEntries",
      requestType = rocksdb_service.RocksdbService.ReplicationEntriesRequest.class,
      responseType = rocksdb_service.RocksdbService.ReplicationEntriesResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<rocksdb_service.RocksdbService.ReplicationEntriesRequest,
      rocksdb_service.RocksdbService.ReplicationEntriesResponse> getGetReplicationEntriesMethod() {
    io.grpc.MethodDescriptor<rocksdb_service.RocksdbService.ReplicationEntriesRequest, rocksdb_service.RocksdbService.ReplicationEntriesResponse> getGetReplicationEntriesMethod;
    if ((getGetReplicationEntriesMethod = RocksDbServiceGrpc.getGetReplicationEntriesMethod) == null) {
      synchronized (RocksDbServiceGrpc.class) {
        if ((getGetReplicationEntriesMethod = RocksDbServiceGrpc.getGetReplicationEntriesMethod) == null) {
          RocksDbServiceGrpc.getGetReplicationEntriesMethod = getGetReplicationEntriesMethod =
              io.grpc.MethodDescriptor.<rocksdb_service.RocksdbService.ReplicationEntriesRequest, rocksdb_service.RocksdbService.ReplicationEntriesResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetReplicationEntries"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  rocksdb_service.RocksdbService.ReplicationEntriesRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  rocksdb_service.RocksdbService.ReplicationEntriesResponse.getDefaultInstance()))
              .setSchemaDescriptor(new RocksDbServiceMethodDescriptorSupplier("GetReplicationEntries"))
              .build();
        }
      }
    }
    return getGetReplicationEntriesMethod;
  }

  private static volatile io.grpc.MethodDescriptor<rocksdb_service.RocksdbService.ReplicationEntry,
      rocksdb_service.RocksdbService.SyncReplicationResponse> getSyncReplicationMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "SyncReplication",
      requestType = rocksdb_service.RocksdbService.ReplicationEntry.class,
      responseType = rocksdb_service.RocksdbService.SyncReplicationResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.CLIENT_STREAMING)
  public static io.grpc.MethodDescriptor<rocksdb_service.RocksdbService.ReplicationEntry,
      rocksdb_service.RocksdbService.SyncReplicationResponse> getSyncReplicationMethod() {
    io.grpc.MethodDescriptor<rocksdb_service.RocksdbService.ReplicationEntry, rocksdb_service.RocksdbService.SyncReplicationResponse> getSyncReplicationMethod;
    if ((getSyncReplicationMethod = RocksDbServiceGrpc.getSyncReplicationMethod) == null) {
      synchronized (RocksDbServiceGrpc.class) {
        if ((getSyncReplicationMethod = RocksDbServiceGrpc.getSyncReplicationMethod) == null) {
          RocksDbServiceGrpc.getSyncReplicationMethod = getSyncReplicationMethod =
              io.grpc.MethodDescriptor.<rocksdb_service.RocksdbService.ReplicationEntry, rocksdb_service.RocksdbService.SyncReplicationResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.CLIENT_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "SyncReplication"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  rocksdb_service.RocksdbService.ReplicationEntry.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  rocksdb_service.RocksdbService.SyncReplicationResponse.getDefaultInstance()))
              .setSchemaDescriptor(new RocksDbServiceMethodDescriptorSupplier("SyncReplication"))
              .build();
        }
      }
    }
    return getSyncReplicationMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static RocksDbServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<RocksDbServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<RocksDbServiceStub>() {
        @java.lang.Override
        public RocksDbServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new RocksDbServiceStub(channel, callOptions);
        }
      };
    return RocksDbServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static RocksDbServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<RocksDbServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<RocksDbServiceBlockingStub>() {
        @java.lang.Override
        public RocksDbServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new RocksDbServiceBlockingStub(channel, callOptions);
        }
      };
    return RocksDbServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static RocksDbServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<RocksDbServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<RocksDbServiceFutureStub>() {
        @java.lang.Override
        public RocksDbServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new RocksDbServiceFutureStub(channel, callOptions);
        }
      };
    return RocksDbServiceFutureStub.newStub(factory, channel);
  }

  /**
   * <pre>
   * The RocksDB service definition
   * </pre>
   */
  public interface AsyncService {

    /**
     * <pre>
     * Put a key-value pair
     * </pre>
     */
    default void put(rocksdb_service.RocksdbService.PutRequest request,
        io.grpc.stub.StreamObserver<rocksdb_service.RocksdbService.PutResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getPutMethod(), responseObserver);
    }

    /**
     * <pre>
     * Get a value by key
     * </pre>
     */
    default void get(rocksdb_service.RocksdbService.GetRequest request,
        io.grpc.stub.StreamObserver<rocksdb_service.RocksdbService.GetResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetMethod(), responseObserver);
    }

    /**
     * <pre>
     * Get multiple values by keys
     * </pre>
     */
    default void multiGet(rocksdb_service.RocksdbService.MultiGetRequest request,
        io.grpc.stub.StreamObserver<rocksdb_service.RocksdbService.MultiGetResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getMultiGetMethod(), responseObserver);
    }

    /**
     * <pre>
     * Get values by prefix
     * </pre>
     */
    default void getByPrefix(rocksdb_service.RocksdbService.GetByPrefixRequest request,
        io.grpc.stub.StreamObserver<rocksdb_service.RocksdbService.GetByPrefixResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetByPrefixMethod(), responseObserver);
    }

    /**
     * <pre>
     * Delete a key
     * </pre>
     */
    default void delete(rocksdb_service.RocksdbService.DeleteRequest request,
        io.grpc.stub.StreamObserver<rocksdb_service.RocksdbService.DeleteResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getDeleteMethod(), responseObserver);
    }

    /**
     * <pre>
     * Replication methods
     * </pre>
     */
    default void getReplicationEntries(rocksdb_service.RocksdbService.ReplicationEntriesRequest request,
        io.grpc.stub.StreamObserver<rocksdb_service.RocksdbService.ReplicationEntriesResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetReplicationEntriesMethod(), responseObserver);
    }

    /**
     */
    default io.grpc.stub.StreamObserver<rocksdb_service.RocksdbService.ReplicationEntry> syncReplication(
        io.grpc.stub.StreamObserver<rocksdb_service.RocksdbService.SyncReplicationResponse> responseObserver) {
      return io.grpc.stub.ServerCalls.asyncUnimplementedStreamingCall(getSyncReplicationMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service RocksDbService.
   * <pre>
   * The RocksDB service definition
   * </pre>
   */
  public static abstract class RocksDbServiceImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return RocksDbServiceGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service RocksDbService.
   * <pre>
   * The RocksDB service definition
   * </pre>
   */
  public static final class RocksDbServiceStub
      extends io.grpc.stub.AbstractAsyncStub<RocksDbServiceStub> {
    private RocksDbServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected RocksDbServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new RocksDbServiceStub(channel, callOptions);
    }

    /**
     * <pre>
     * Put a key-value pair
     * </pre>
     */
    public void put(rocksdb_service.RocksdbService.PutRequest request,
        io.grpc.stub.StreamObserver<rocksdb_service.RocksdbService.PutResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getPutMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Get a value by key
     * </pre>
     */
    public void get(rocksdb_service.RocksdbService.GetRequest request,
        io.grpc.stub.StreamObserver<rocksdb_service.RocksdbService.GetResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Get multiple values by keys
     * </pre>
     */
    public void multiGet(rocksdb_service.RocksdbService.MultiGetRequest request,
        io.grpc.stub.StreamObserver<rocksdb_service.RocksdbService.MultiGetResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getMultiGetMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Get values by prefix
     * </pre>
     */
    public void getByPrefix(rocksdb_service.RocksdbService.GetByPrefixRequest request,
        io.grpc.stub.StreamObserver<rocksdb_service.RocksdbService.GetByPrefixResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetByPrefixMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Delete a key
     * </pre>
     */
    public void delete(rocksdb_service.RocksdbService.DeleteRequest request,
        io.grpc.stub.StreamObserver<rocksdb_service.RocksdbService.DeleteResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getDeleteMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Replication methods
     * </pre>
     */
    public void getReplicationEntries(rocksdb_service.RocksdbService.ReplicationEntriesRequest request,
        io.grpc.stub.StreamObserver<rocksdb_service.RocksdbService.ReplicationEntriesResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetReplicationEntriesMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public io.grpc.stub.StreamObserver<rocksdb_service.RocksdbService.ReplicationEntry> syncReplication(
        io.grpc.stub.StreamObserver<rocksdb_service.RocksdbService.SyncReplicationResponse> responseObserver) {
      return io.grpc.stub.ClientCalls.asyncClientStreamingCall(
          getChannel().newCall(getSyncReplicationMethod(), getCallOptions()), responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service RocksDbService.
   * <pre>
   * The RocksDB service definition
   * </pre>
   */
  public static final class RocksDbServiceBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<RocksDbServiceBlockingStub> {
    private RocksDbServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected RocksDbServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new RocksDbServiceBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * Put a key-value pair
     * </pre>
     */
    public rocksdb_service.RocksdbService.PutResponse put(rocksdb_service.RocksdbService.PutRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getPutMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Get a value by key
     * </pre>
     */
    public rocksdb_service.RocksdbService.GetResponse get(rocksdb_service.RocksdbService.GetRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Get multiple values by keys
     * </pre>
     */
    public rocksdb_service.RocksdbService.MultiGetResponse multiGet(rocksdb_service.RocksdbService.MultiGetRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getMultiGetMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Get values by prefix
     * </pre>
     */
    public rocksdb_service.RocksdbService.GetByPrefixResponse getByPrefix(rocksdb_service.RocksdbService.GetByPrefixRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetByPrefixMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Delete a key
     * </pre>
     */
    public rocksdb_service.RocksdbService.DeleteResponse delete(rocksdb_service.RocksdbService.DeleteRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getDeleteMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Replication methods
     * </pre>
     */
    public rocksdb_service.RocksdbService.ReplicationEntriesResponse getReplicationEntries(rocksdb_service.RocksdbService.ReplicationEntriesRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetReplicationEntriesMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service RocksDbService.
   * <pre>
   * The RocksDB service definition
   * </pre>
   */
  public static final class RocksDbServiceFutureStub
      extends io.grpc.stub.AbstractFutureStub<RocksDbServiceFutureStub> {
    private RocksDbServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected RocksDbServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new RocksDbServiceFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     * Put a key-value pair
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<rocksdb_service.RocksdbService.PutResponse> put(
        rocksdb_service.RocksdbService.PutRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getPutMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Get a value by key
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<rocksdb_service.RocksdbService.GetResponse> get(
        rocksdb_service.RocksdbService.GetRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Get multiple values by keys
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<rocksdb_service.RocksdbService.MultiGetResponse> multiGet(
        rocksdb_service.RocksdbService.MultiGetRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getMultiGetMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Get values by prefix
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<rocksdb_service.RocksdbService.GetByPrefixResponse> getByPrefix(
        rocksdb_service.RocksdbService.GetByPrefixRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetByPrefixMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Delete a key
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<rocksdb_service.RocksdbService.DeleteResponse> delete(
        rocksdb_service.RocksdbService.DeleteRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getDeleteMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Replication methods
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<rocksdb_service.RocksdbService.ReplicationEntriesResponse> getReplicationEntries(
        rocksdb_service.RocksdbService.ReplicationEntriesRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetReplicationEntriesMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_PUT = 0;
  private static final int METHODID_GET = 1;
  private static final int METHODID_MULTI_GET = 2;
  private static final int METHODID_GET_BY_PREFIX = 3;
  private static final int METHODID_DELETE = 4;
  private static final int METHODID_GET_REPLICATION_ENTRIES = 5;
  private static final int METHODID_SYNC_REPLICATION = 6;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final AsyncService serviceImpl;
    private final int methodId;

    MethodHandlers(AsyncService serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_PUT:
          serviceImpl.put((rocksdb_service.RocksdbService.PutRequest) request,
              (io.grpc.stub.StreamObserver<rocksdb_service.RocksdbService.PutResponse>) responseObserver);
          break;
        case METHODID_GET:
          serviceImpl.get((rocksdb_service.RocksdbService.GetRequest) request,
              (io.grpc.stub.StreamObserver<rocksdb_service.RocksdbService.GetResponse>) responseObserver);
          break;
        case METHODID_MULTI_GET:
          serviceImpl.multiGet((rocksdb_service.RocksdbService.MultiGetRequest) request,
              (io.grpc.stub.StreamObserver<rocksdb_service.RocksdbService.MultiGetResponse>) responseObserver);
          break;
        case METHODID_GET_BY_PREFIX:
          serviceImpl.getByPrefix((rocksdb_service.RocksdbService.GetByPrefixRequest) request,
              (io.grpc.stub.StreamObserver<rocksdb_service.RocksdbService.GetByPrefixResponse>) responseObserver);
          break;
        case METHODID_DELETE:
          serviceImpl.delete((rocksdb_service.RocksdbService.DeleteRequest) request,
              (io.grpc.stub.StreamObserver<rocksdb_service.RocksdbService.DeleteResponse>) responseObserver);
          break;
        case METHODID_GET_REPLICATION_ENTRIES:
          serviceImpl.getReplicationEntries((rocksdb_service.RocksdbService.ReplicationEntriesRequest) request,
              (io.grpc.stub.StreamObserver<rocksdb_service.RocksdbService.ReplicationEntriesResponse>) responseObserver);
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
        case METHODID_SYNC_REPLICATION:
          return (io.grpc.stub.StreamObserver<Req>) serviceImpl.syncReplication(
              (io.grpc.stub.StreamObserver<rocksdb_service.RocksdbService.SyncReplicationResponse>) responseObserver);
        default:
          throw new AssertionError();
      }
    }
  }

  public static final io.grpc.ServerServiceDefinition bindService(AsyncService service) {
    return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
        .addMethod(
          getPutMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              rocksdb_service.RocksdbService.PutRequest,
              rocksdb_service.RocksdbService.PutResponse>(
                service, METHODID_PUT)))
        .addMethod(
          getGetMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              rocksdb_service.RocksdbService.GetRequest,
              rocksdb_service.RocksdbService.GetResponse>(
                service, METHODID_GET)))
        .addMethod(
          getMultiGetMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              rocksdb_service.RocksdbService.MultiGetRequest,
              rocksdb_service.RocksdbService.MultiGetResponse>(
                service, METHODID_MULTI_GET)))
        .addMethod(
          getGetByPrefixMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              rocksdb_service.RocksdbService.GetByPrefixRequest,
              rocksdb_service.RocksdbService.GetByPrefixResponse>(
                service, METHODID_GET_BY_PREFIX)))
        .addMethod(
          getDeleteMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              rocksdb_service.RocksdbService.DeleteRequest,
              rocksdb_service.RocksdbService.DeleteResponse>(
                service, METHODID_DELETE)))
        .addMethod(
          getGetReplicationEntriesMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              rocksdb_service.RocksdbService.ReplicationEntriesRequest,
              rocksdb_service.RocksdbService.ReplicationEntriesResponse>(
                service, METHODID_GET_REPLICATION_ENTRIES)))
        .addMethod(
          getSyncReplicationMethod(),
          io.grpc.stub.ServerCalls.asyncClientStreamingCall(
            new MethodHandlers<
              rocksdb_service.RocksdbService.ReplicationEntry,
              rocksdb_service.RocksdbService.SyncReplicationResponse>(
                service, METHODID_SYNC_REPLICATION)))
        .build();
  }

  private static abstract class RocksDbServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    RocksDbServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return rocksdb_service.RocksdbService.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("RocksDbService");
    }
  }

  private static final class RocksDbServiceFileDescriptorSupplier
      extends RocksDbServiceBaseDescriptorSupplier {
    RocksDbServiceFileDescriptorSupplier() {}
  }

  private static final class RocksDbServiceMethodDescriptorSupplier
      extends RocksDbServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    RocksDbServiceMethodDescriptorSupplier(java.lang.String methodName) {
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
      synchronized (RocksDbServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new RocksDbServiceFileDescriptorSupplier())
              .addMethod(getPutMethod())
              .addMethod(getGetMethod())
              .addMethod(getMultiGetMethod())
              .addMethod(getGetByPrefixMethod())
              .addMethod(getDeleteMethod())
              .addMethod(getGetReplicationEntriesMethod())
              .addMethod(getSyncReplicationMethod())
              .build();
        }
      }
    }
    return result;
  }
}
