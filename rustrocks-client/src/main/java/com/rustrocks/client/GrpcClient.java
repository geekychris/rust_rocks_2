package com.rustrocks.client;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import com.google.protobuf.ByteString;

// Import generated protobuf classes
import rocksdb_service.RocksDbServiceGrpc;
import rocksdb_service.RocksdbService.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public class GrpcClient implements AutoCloseable {
    private final ManagedChannel channel;
    private final RocksDbServiceGrpc.RocksDbServiceBlockingStub blockingStub;

    public GrpcClient(String host, int port) {
        this.channel = ManagedChannelBuilder.forTarget("localhost:" + port)
            .nameResolverFactory(new io.grpc.internal.DnsNameResolverProvider())
            .defaultLoadBalancingPolicy("pick_first")
            .usePlaintext()
            .build();
        this.blockingStub = RocksDbServiceGrpc.newBlockingStub(channel);
    }

    @Override
    public void close() throws InterruptedException {
        shutdown();
    }
    
    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    public static class ApiResponse<T> {
        public boolean success;
        public T data;
        public String error;

        public ApiResponse(boolean success, T data, String error) {
            this.success = success;
            this.data = data;
            this.error = error;
        }
    }

    public static class CompositeKey {
        public String partition;
        public String key;

        public CompositeKey(String partition, String key) {
            this.partition = partition;
            this.key = key;
        }
    }

    public static class KeyValue {
        public String key;
        public byte[] value;

        public KeyValue(String key, byte[] value) {
            this.key = key;
            this.value = value;
        }
    }

    public ApiResponse<Void> put(String partition, String key, byte[] value) {
        try {
            PutRequest request = PutRequest.newBuilder()
                    .setPartition(partition)
                    .setKey(key)
                    .setValue(ByteString.copyFrom(value))
                    .build();

            PutResponse response = blockingStub.put(request);

            if (response.getSuccess()) {
                return new ApiResponse<>(true, null, null);
            } else {
                String error = response.hasError() ? response.getError() : "Put operation failed";
                return new ApiResponse<>(false, null, error);
            }
        } catch (StatusRuntimeException e) {
            String error = String.format("gRPC Error: status=%s, code=%s, description=%s", 
                e.getStatus(), e.getStatus().getCode(), e.getStatus().getDescription());
            System.err.println("Detailed error: " + error);
            e.printStackTrace();
            return new ApiResponse<>(false, null, error);
        } catch (Exception e) {
            System.err.println("Detailed error: " + e.toString());
            e.printStackTrace();
            return new ApiResponse<>(false, null, "Error: " + e.getMessage());
        }
    }

    public ApiResponse<byte[]> get(String partition, String key) {
        try {
            GetRequest request = GetRequest.newBuilder()
                    .setPartition(partition)
                    .setKey(key)
                    .build();

            GetResponse response = blockingStub.get(request);

            if (response.hasError() && !response.getError().isEmpty()) {
                return new ApiResponse<>(false, null, response.getError());
            }

            if (response.getFound() && response.hasValue()) {
                byte[] value = response.getValue().toByteArray();
                return new ApiResponse<>(true, value, null);
            } else {
                return new ApiResponse<>(false, null, "Key not found");
            }
        } catch (StatusRuntimeException e) {
            return new ApiResponse<>(false, null, "gRPC Error: " + e.getStatus().getDescription());
        } catch (Exception e) {
            return new ApiResponse<>(false, null, "Error: " + e.getMessage());
        }
    }

    public ApiResponse<Void> delete(String partition, String key) {
        try {
            DeleteRequest request = DeleteRequest.newBuilder()
                    .setPartition(partition)
                    .setKey(key)
                    .build();

            DeleteResponse response = blockingStub.delete(request);

            if (response.getSuccess()) {
                return new ApiResponse<>(true, null, null);
            } else {
                String error = response.hasError() ? response.getError() : "Delete operation failed";
                return new ApiResponse<>(false, null, error);
            }
        } catch (StatusRuntimeException e) {
            return new ApiResponse<>(false, null, "gRPC Error: " + e.getStatus().getDescription());
        } catch (Exception e) {
            return new ApiResponse<>(false, null, "Error: " + e.getMessage());
        }
    }

    public ApiResponse<Map<String, byte[]>> multiGet(List<CompositeKey> keys) {
        try {
            MultiGetRequest.Builder requestBuilder = MultiGetRequest.newBuilder();
            
            for (CompositeKey key : keys) {
                rocksdb_service.RocksdbService.CompositeKey protoKey = 
                    rocksdb_service.RocksdbService.CompositeKey.newBuilder()
                        .setPartition(key.partition)
                        .setKey(key.key)
                        .build();
                requestBuilder.addKeys(protoKey);
            }

            MultiGetRequest request = requestBuilder.build();
            MultiGetResponse response = blockingStub.multiGet(request);

            if (response.hasError() && !response.getError().isEmpty()) {
                return new ApiResponse<>(false, null, response.getError());
            }

            Map<String, byte[]> results = new HashMap<>();
            for (KeyValueResult result : response.getResultsList()) {
                String keyStr = result.getKey().getPartition() + ":" + result.getKey().getKey();
                if (result.hasValue()) {
                    results.put(keyStr, result.getValue().toByteArray());
                }
            }

            return new ApiResponse<>(true, results, null);
        } catch (StatusRuntimeException e) {
            return new ApiResponse<>(false, null, "gRPC Error: " + e.getStatus().getDescription());
        } catch (Exception e) {
            return new ApiResponse<>(false, null, "Error: " + e.getMessage());
        }
    }

    public ApiResponse<List<KeyValue>> getByPrefix(String partition, String keyPrefix, Integer limit) {
        try {
            GetByPrefixRequest.Builder requestBuilder = GetByPrefixRequest.newBuilder()
                    .setPartition(partition)
                    .setKeyPrefix(keyPrefix);
            
            if (limit != null) {
                requestBuilder.setLimit(limit);
            }

            GetByPrefixRequest request = requestBuilder.build();
            GetByPrefixResponse response = blockingStub.getByPrefix(request);

            if (response.hasError() && !response.getError().isEmpty()) {
                return new ApiResponse<>(false, null, response.getError());
            }

            List<KeyValue> results = new java.util.ArrayList<>();
            for (rocksdb_service.RocksdbService.KeyValue kv : response.getResultsList()) {
                results.add(new KeyValue(kv.getKey(), kv.getValue().toByteArray()));
            }

            return new ApiResponse<>(true, results, null);
        } catch (StatusRuntimeException e) {
            return new ApiResponse<>(false, null, "gRPC Error: " + e.getStatus().getDescription());
        } catch (Exception e) {
            return new ApiResponse<>(false, null, "Error: " + e.getMessage());
        }
    }

    public boolean health() {
        try {
            // Since there's no health check in the proto, we'll try a simple get operation
            // with a non-existent key to test connectivity
            GetRequest request = GetRequest.newBuilder()
                    .setPartition("__health__")
                    .setKey("__check__")
                    .build();

            blockingStub.withDeadlineAfter(5, TimeUnit.SECONDS).get(request);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}