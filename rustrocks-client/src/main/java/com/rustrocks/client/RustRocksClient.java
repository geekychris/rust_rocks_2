package com.rustrocks.client;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

@Command(name = "rustrocks-client", mixinStandardHelpOptions = true, version = "1.0.0",
        description = "Command-line client for RustRocks key-value service")
public class RustRocksClient implements Callable<Integer> {

    @Option(names = {"-h", "--host"}, description = "Server host (default: 127.0.0.1)", defaultValue = "127.0.0.1")
    String host;

    @Option(names = {"-p", "--port"}, description = "Server port (default: 8080 for REST, 50051 for gRPC)")
    Integer port;

    @Option(names = {"-P", "--protocol"}, description = "Protocol to use: rest or grpc (default: rest)", defaultValue = "rest")
    String protocol;

    @Option(names = {"-v", "--verbose"}, description = "Verbose output")
    boolean verbose;

    @Command(name = "put", description = "Store a key-value pair")
    static class PutCommand implements Callable<Integer> {
        @Option(names = {"-h", "--host"}, description = "Server host", defaultValue = "127.0.0.1")
        String host;

        @Option(names = {"-p", "--port"}, description = "Server port")
        Integer port;

        @Option(names = {"-P", "--protocol"}, description = "Protocol: rest or grpc", defaultValue = "rest")
        String protocol;

        @Option(names = {"-v", "--verbose"}, description = "Verbose output")
        boolean verbose;

        @Parameters(index = "0", description = "Partition name")
        String partition;

        @Parameters(index = "1", description = "Key")
        String key;

        @Parameters(index = "2", description = "Value")
        String value;

        @Override
        public Integer call() throws Exception {
            try {
                int actualPort = port != null ? port : ("grpc".equals(protocol) ? 50051 : 8080);
                
                if (verbose) {
                    System.out.printf("Connecting to %s:%d using %s%n", host, actualPort, protocol.toUpperCase());
                    System.out.printf("PUT %s:%s = \"%s\"%n", partition, key, value);
                }

                byte[] valueBytes = value.getBytes(StandardCharsets.UTF_8);

                if ("grpc".equals(protocol)) {
                    GrpcClient client = new GrpcClient(host, actualPort);
                    try {
                        var response = client.put(partition, key, valueBytes);
                        
                        if (response.success) {
                            System.out.println("✓ Put successful");
                            return 0;
                        } else {
                            System.err.printf("✗ Put failed: %s%n", response.error);
                            return 1;
                        }
                    } finally {
                        client.shutdown();
                    }
                } else {
                    RestClient client = new RestClient(host, actualPort);
                    var response = client.put(partition, key, valueBytes);
                    
                    if (response.success) {
                        System.out.println("✓ Put successful");
                        return 0;
                    } else {
                        System.err.printf("✗ Put failed: %s%n", response.error);
                        return 1;
                    }
                }
            } catch (Exception e) {
                System.err.printf("Error: %s%n", e.getMessage());
                if (verbose) {
                    e.printStackTrace();
                }
                return 1;
            }
        }
    }

    @Command(name = "get", description = "Retrieve a value by key")
    static class GetCommand implements Callable<Integer> {
        @Option(names = {"-h", "--host"}, description = "Server host", defaultValue = "127.0.0.1")
        String host;

        @Option(names = {"-p", "--port"}, description = "Server port")
        Integer port;

        @Option(names = {"-P", "--protocol"}, description = "Protocol: rest or grpc", defaultValue = "rest")
        String protocol;

        @Option(names = {"-v", "--verbose"}, description = "Verbose output")
        boolean verbose;

        @Option(names = {"-b", "--binary"}, description = "Output as binary (hex) instead of string")
        boolean binary;

        @Parameters(index = "0", description = "Partition name")
        String partition;

        @Parameters(index = "1", description = "Key")
        String key;

        @Override
        public Integer call() throws Exception {
            try {
                int actualPort = port != null ? port : ("grpc".equals(protocol) ? 50051 : 8080);
                
                if (verbose) {
                    System.out.printf("Connecting to %s:%d using %s%n", host, actualPort, protocol.toUpperCase());
                    System.out.printf("GET %s:%s%n", partition, key);
                }

                if ("grpc".equals(protocol)) {
                    GrpcClient client = new GrpcClient(host, actualPort);
                    try {
                        var response = client.get(partition, key);
                        
                        if (response.success && response.data != null) {
                            if (binary) {
                                System.out.println(bytesToHex(response.data));
                            } else {
                                System.out.println(new String(response.data, StandardCharsets.UTF_8));
                            }
                            return 0;
                        } else {
                            System.err.printf("✗ Get failed: %s%n", response.error);
                            return 1;
                        }
                    } finally {
                        client.shutdown();
                    }
                } else {
                    RestClient client = new RestClient(host, actualPort);
                    var response = client.get(partition, key);
                    
                    if (response.success && response.data != null) {
                        if (binary) {
                            System.out.println(bytesToHex(response.data));
                        } else {
                            System.out.println(new String(response.data, StandardCharsets.UTF_8));
                        }
                        return 0;
                    } else {
                        System.err.printf("✗ Get failed: %s%n", response.error);
                        return 1;
                    }
                }
            } catch (Exception e) {
                System.err.printf("Error: %s%n", e.getMessage());
                if (verbose) {
                    e.printStackTrace();
                }
                return 1;
            }
        }
    }

    @Command(name = "delete", description = "Delete a key")
    static class DeleteCommand implements Callable<Integer> {
        @Option(names = {"-h", "--host"}, description = "Server host", defaultValue = "127.0.0.1")
        String host;

        @Option(names = {"-p", "--port"}, description = "Server port")
        Integer port;

        @Option(names = {"-P", "--protocol"}, description = "Protocol: rest or grpc", defaultValue = "rest")
        String protocol;

        @Option(names = {"-v", "--verbose"}, description = "Verbose output")
        boolean verbose;

        @Parameters(index = "0", description = "Partition name")
        String partition;

        @Parameters(index = "1", description = "Key")
        String key;

        @Override
        public Integer call() throws Exception {
            try {
                int actualPort = port != null ? port : ("grpc".equals(protocol) ? 50051 : 8080);
                
                if (verbose) {
                    System.out.printf("Connecting to %s:%d using %s%n", host, actualPort, protocol.toUpperCase());
                    System.out.printf("DELETE %s:%s%n", partition, key);
                }

                if ("grpc".equals(protocol)) {
                    GrpcClient client = new GrpcClient(host, actualPort);
                    try {
                        var response = client.delete(partition, key);
                        
                        if (response.success) {
                            System.out.println("✓ Delete successful");
                            return 0;
                        } else {
                            System.err.printf("✗ Delete failed: %s%n", response.error);
                            return 1;
                        }
                    } finally {
                        client.shutdown();
                    }
                } else {
                    RestClient client = new RestClient(host, actualPort);
                    var response = client.delete(partition, key);
                    
                    if (response.success) {
                        System.out.println("✓ Delete successful");
                        return 0;
                    } else {
                        System.err.printf("✗ Delete failed: %s%n", response.error);
                        return 1;
                    }
                }
            } catch (Exception e) {
                System.err.printf("Error: %s%n", e.getMessage());
                if (verbose) {
                    e.printStackTrace();
                }
                return 1;
            }
        }
    }

    @Command(name = "multi-get", description = "Retrieve multiple values by keys")
    static class MultiGetCommand implements Callable<Integer> {
        @Option(names = {"-h", "--host"}, description = "Server host", defaultValue = "127.0.0.1")
        String host;

        @Option(names = {"-p", "--port"}, description = "Server port")
        Integer port;

        @Option(names = {"-P", "--protocol"}, description = "Protocol: rest or grpc", defaultValue = "rest")
        String protocol;

        @Option(names = {"-v", "--verbose"}, description = "Verbose output")
        boolean verbose;

        @Option(names = {"-b", "--binary"}, description = "Output as binary (hex) instead of string")
        boolean binary;

        @Parameters(description = "Keys in format partition:key")
        List<String> keys;

        @Override
        public Integer call() throws Exception {
            try {
                int actualPort = port != null ? port : ("grpc".equals(protocol) ? 50051 : 8080);
                
                if (verbose) {
                    System.out.printf("Connecting to %s:%d using %s%n", host, actualPort, protocol.toUpperCase());
                    System.out.printf("MULTI-GET %s%n", keys);
                }

                List<RestClient.CompositeKey> restKeys = new ArrayList<>();
                List<GrpcClient.CompositeKey> grpcKeys = new ArrayList<>();
                
                for (String keyStr : keys) {
                    String[] parts = keyStr.split(":", 2);
                    if (parts.length != 2) {
                        System.err.printf("Invalid key format: %s (expected partition:key)%n", keyStr);
                        return 1;
                    }
                    restKeys.add(new RestClient.CompositeKey(parts[0], parts[1]));
                    grpcKeys.add(new GrpcClient.CompositeKey(parts[0], parts[1]));
                }

                if ("grpc".equals(protocol)) {
                    GrpcClient client = new GrpcClient(host, actualPort);
                    try {
                        var response = client.multiGet(grpcKeys);
                        
                        if (response.success && response.data != null) {
                            for (Map.Entry<String, byte[]> entry : response.data.entrySet()) {
                                if (binary) {
                                    System.out.printf("%s = %s%n", entry.getKey(), bytesToHex(entry.getValue()));
                                } else {
                                    System.out.printf("%s = %s%n", entry.getKey(), 
                                        new String(entry.getValue(), StandardCharsets.UTF_8));
                                }
                            }
                            return 0;
                        } else {
                            System.err.printf("✗ Multi-get failed: %s%n", response.error);
                            return 1;
                        }
                    } finally {
                        client.shutdown();
                    }
                } else {
                    RestClient client = new RestClient(host, actualPort);
                    var response = client.multiGet(restKeys);
                    
                    if (response.success && response.data != null) {
                        for (Map.Entry<String, byte[]> entry : response.data.entrySet()) {
                            if (binary) {
                                System.out.printf("%s = %s%n", entry.getKey(), bytesToHex(entry.getValue()));
                            } else {
                                System.out.printf("%s = %s%n", entry.getKey(), 
                                    new String(entry.getValue(), StandardCharsets.UTF_8));
                            }
                        }
                        return 0;
                    } else {
                        System.err.printf("✗ Multi-get failed: %s%n", response.error);
                        return 1;
                    }
                }
            } catch (Exception e) {
                System.err.printf("Error: %s%n", e.getMessage());
                if (verbose) {
                    e.printStackTrace();
                }
                return 1;
            }
        }
    }

    @Command(name = "prefix", description = "Get values by key prefix")
    static class PrefixCommand implements Callable<Integer> {
        @Option(names = {"-h", "--host"}, description = "Server host", defaultValue = "127.0.0.1")
        String host;

        @Option(names = {"-p", "--port"}, description = "Server port")
        Integer port;

        @Option(names = {"-P", "--protocol"}, description = "Protocol: rest or grpc", defaultValue = "rest")
        String protocol;

        @Option(names = {"-v", "--verbose"}, description = "Verbose output")
        boolean verbose;

        @Option(names = {"-b", "--binary"}, description = "Output as binary (hex) instead of string")
        boolean binary;

        @Option(names = {"-l", "--limit"}, description = "Maximum number of results")
        Integer limit;

        @Parameters(index = "0", description = "Partition name")
        String partition;

        @Parameters(index = "1", description = "Key prefix")
        String keyPrefix;

        @Override
        public Integer call() throws Exception {
            try {
                int actualPort = port != null ? port : ("grpc".equals(protocol) ? 50051 : 8080);
                
                if (verbose) {
                    System.out.printf("Connecting to %s:%d using %s%n", host, actualPort, protocol.toUpperCase());
                    System.out.printf("PREFIX %s:%s*%n", partition, keyPrefix);
                }

                if ("grpc".equals(protocol)) {
                    GrpcClient client = new GrpcClient(host, actualPort);
                    try {
                        var response = client.getByPrefix(partition, keyPrefix, limit);
                        
                        if (response.success && response.data != null) {
                            for (var kv : response.data) {
                                if (binary) {
                                    System.out.printf("%s = %s%n", kv.key, bytesToHex(kv.value));
                                } else {
                                    System.out.printf("%s = %s%n", kv.key, 
                                        new String(kv.value, StandardCharsets.UTF_8));
                                }
                            }
                            System.out.printf("Found %d matching keys%n", response.data.size());
                            return 0;
                        } else {
                            System.err.printf("✗ Prefix search failed: %s%n", response.error);
                            return 1;
                        }
                    } finally {
                        client.shutdown();
                    }
                } else {
                    RestClient client = new RestClient(host, actualPort);
                    var response = client.getByPrefix(partition, keyPrefix, limit);
                    
                    if (response.success && response.data != null) {
                        for (var kv : response.data) {
                            if (binary) {
                                System.out.printf("%s = %s%n", kv.key, bytesToHex(kv.value));
                            } else {
                                System.out.printf("%s = %s%n", kv.key, 
                                    new String(kv.value, StandardCharsets.UTF_8));
                            }
                        }
                        System.out.printf("Found %d matching keys%n", response.data.size());
                        return 0;
                    } else {
                        System.err.printf("✗ Prefix search failed: %s%n", response.error);
                        return 1;
                    }
                }
            } catch (Exception e) {
                System.err.printf("Error: %s%n", e.getMessage());
                if (verbose) {
                    e.printStackTrace();
                }
                return 1;
            }
        }
    }

    @Command(name = "batch", description = "Batch import from CSV file")
    static class BatchCommand implements Callable<Integer> {
        @Option(names = {"-h", "--host"}, description = "Server host", defaultValue = "127.0.0.1")
        String host;

        @Option(names = {"-p", "--port"}, description = "Server port")
        Integer port;

        @Option(names = {"-P", "--protocol"}, description = "Protocol: rest or grpc", defaultValue = "rest")
        String protocol;

        @Option(names = {"-v", "--verbose"}, description = "Verbose output")
        boolean verbose;

        @Parameters(index = "0", description = "CSV file path")
        String csvFile;

        @Override
        public Integer call() throws Exception {
            try {
                int actualPort = port != null ? port : ("grpc".equals(protocol) ? 50051 : 8080);
                
                if (verbose) {
                    System.out.printf("Connecting to %s:%d using %s%n", host, actualPort, protocol.toUpperCase());
                    System.out.printf("BATCH import from %s%n", csvFile);
                }

                File file = new File(csvFile);
                if (!file.exists() || !file.isFile()) {
                    System.err.printf("CSV file not found: %s%n", csvFile);
                    return 1;
                }

                System.out.printf("Reading CSV file: %s%n", csvFile);
                var records = CsvProcessor.readCsvFile(csvFile);
                System.out.printf("Found %d records%n", records.size());

                System.out.println("Starting batch upload...");

                CsvProcessor.BatchResult result;
                if ("grpc".equals(protocol)) {
                    GrpcClient client = new GrpcClient(host, actualPort);
                    try {
                        result = CsvProcessor.executeBatchPut(records, client);
                    } finally {
                        client.shutdown();
                    }
                } else {
                    RestClient client = new RestClient(host, actualPort);
                    result = CsvProcessor.executeBatchPut(records, client);
                }

                CsvProcessor.printBatchResult(result);
                
                return result.failedRecords > 0 ? 1 : 0;
            } catch (Exception e) {
                System.err.printf("Error: %s%n", e.getMessage());
                if (verbose) {
                    e.printStackTrace();
                }
                return 1;
            }
        }
    }

    @Command(name = "health", description = "Check server health")
    static class HealthCommand implements Callable<Integer> {
        @Option(names = {"-h", "--host"}, description = "Server host", defaultValue = "127.0.0.1")
        String host;

        @Option(names = {"-p", "--port"}, description = "Server port")
        Integer port;

        @Option(names = {"-P", "--protocol"}, description = "Protocol: rest or grpc", defaultValue = "rest")
        String protocol;

        @Option(names = {"-v", "--verbose"}, description = "Verbose output")
        boolean verbose;

        @Override
        public Integer call() throws Exception {
            try {
                int actualPort = port != null ? port : ("grpc".equals(protocol) ? 50051 : 8080);
                
                if (verbose) {
                    System.out.printf("Connecting to %s:%d using %s%n", host, actualPort, protocol.toUpperCase());
                }

                boolean healthy;
                if ("grpc".equals(protocol)) {
                    GrpcClient client = new GrpcClient(host, actualPort);
                    try {
                        healthy = client.health();
                    } finally {
                        client.shutdown();
                    }
                } else {
                    RestClient client = new RestClient(host, actualPort);
                    healthy = client.health();
                }

                if (healthy) {
                    System.out.println("✓ Server is healthy");
                    return 0;
                } else {
                    System.out.println("✗ Server is not responding");
                    return 1;
                }
            } catch (Exception e) {
                System.err.printf("Error: %s%n", e.getMessage());
                if (verbose) {
                    e.printStackTrace();
                }
                return 1;
            }
        }
    }

    @Override
    public Integer call() throws Exception {
        System.out.println("RustRocks Client - Use --help for usage information");
        return 0;
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new RustRocksClient())
                .addSubcommand("put", new PutCommand())
                .addSubcommand("get", new GetCommand())
                .addSubcommand("delete", new DeleteCommand())
                .addSubcommand("multi-get", new MultiGetCommand())
                .addSubcommand("prefix", new PrefixCommand())
                .addSubcommand("batch", new BatchCommand())
                .addSubcommand("health", new HealthCommand())
                .execute(args);
        System.exit(exitCode);
    }
}