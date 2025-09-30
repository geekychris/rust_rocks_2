package com.rustrocks.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class RestClient {
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;

    public RestClient(String host, int port) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
        this.baseUrl = String.format("http://%s:%d", host, port);
    }

    public static class PutRequest {
        public String partition;
        public String key;
        public byte[] value;

        public PutRequest(String partition, String key, byte[] value) {
            this.partition = partition;
            this.key = key;
            this.value = value;
        }
    }

    public static class GetRequest {
        public String partition;
        public String key;

        public GetRequest(String partition, String key) {
            this.partition = partition;
            this.key = key;
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

    public static class MultiGetRequest {
        public List<CompositeKey> keys;

        public MultiGetRequest(List<CompositeKey> keys) {
            this.keys = keys;
        }
    }

    public static class GetByPrefixRequest {
        public String partition;
        public String key_prefix;
        public Integer limit;

        public GetByPrefixRequest(String partition, String keyPrefix, Integer limit) {
            this.partition = partition;
            this.key_prefix = keyPrefix;
            this.limit = limit;
        }
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

    public static class KeyValue {
        public String key;
        public byte[] value;
    }

    public ApiResponse<Void> put(String partition, String key, byte[] value) {
        try {
            // Create JSON manually to ensure proper byte array format
            ObjectNode jsonNode = objectMapper.createObjectNode();
            jsonNode.put("partition", partition);
            jsonNode.put("key", key);
            
            // Convert byte array to JSON array of integers
            ArrayNode valueArray = objectMapper.createArrayNode();
            for (byte b : value) {
                valueArray.add(b & 0xFF); // Convert to unsigned int
            }
            jsonNode.set("value", valueArray);
            
            String jsonRequest = objectMapper.writeValueAsString(jsonNode);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/put"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonRequest))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, 
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                var responseBody = objectMapper.readTree(response.body());
                boolean success = responseBody.get("success").asBoolean();
                String error = responseBody.has("error") && !responseBody.get("error").isNull() 
                    ? responseBody.get("error").asText() : null;
                
                return new ApiResponse<>(success, null, error);
            } else {
                return new ApiResponse<>(false, null, 
                    "HTTP " + response.statusCode() + ": " + response.body());
            }
        } catch (Exception e) {
            return new ApiResponse<>(false, null, "Error: " + e.getMessage());
        }
    }

    public ApiResponse<byte[]> get(String partition, String key) {
        try {
            GetRequest request = new GetRequest(partition, key);
            String jsonRequest = objectMapper.writeValueAsString(request);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/get"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonRequest))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, 
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                var responseBody = objectMapper.readTree(response.body());
                boolean found = responseBody.get("found").asBoolean();
                String error = responseBody.has("error") && !responseBody.get("error").isNull() 
                    ? responseBody.get("error").asText() : null;
                
                if (found && responseBody.has("value") && !responseBody.get("value").isNull()) {
                    var valueArray = responseBody.get("value");
                    byte[] value = new byte[valueArray.size()];
                    for (int i = 0; i < valueArray.size(); i++) {
                        value[i] = (byte) valueArray.get(i).asInt();
                    }
                    return new ApiResponse<>(true, value, error);
                } else {
                    return new ApiResponse<>(false, null, error != null ? error : "Key not found");
                }
            } else {
                return new ApiResponse<>(false, null, 
                    "HTTP " + response.statusCode() + ": " + response.body());
            }
        } catch (Exception e) {
            return new ApiResponse<>(false, null, "Error: " + e.getMessage());
        }
    }

    public ApiResponse<Void> delete(String partition, String key) {
        try {
            GetRequest request = new GetRequest(partition, key);
            String jsonRequest = objectMapper.writeValueAsString(request);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/delete"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonRequest))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, 
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                var responseBody = objectMapper.readTree(response.body());
                boolean success = responseBody.get("success").asBoolean();
                String error = responseBody.has("error") && !responseBody.get("error").isNull() 
                    ? responseBody.get("error").asText() : null;
                
                return new ApiResponse<>(success, null, error);
            } else {
                return new ApiResponse<>(false, null, 
                    "HTTP " + response.statusCode() + ": " + response.body());
            }
        } catch (Exception e) {
            return new ApiResponse<>(false, null, "Error: " + e.getMessage());
        }
    }

    public ApiResponse<Map<String, byte[]>> multiGet(List<CompositeKey> keys) {
        try {
            MultiGetRequest request = new MultiGetRequest(keys);
            String jsonRequest = objectMapper.writeValueAsString(request);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/multi_get"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonRequest))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, 
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                var responseBody = objectMapper.readTree(response.body());
                String error = responseBody.has("error") && !responseBody.get("error").isNull() 
                    ? responseBody.get("error").asText() : null;
                
                if (error != null) {
                    return new ApiResponse<>(false, null, error);
                }

                Map<String, byte[]> results = new HashMap<>();
                var resultsNode = responseBody.get("results");
                if (resultsNode != null && resultsNode.isObject()) {
                    resultsNode.fields().forEachRemaining(entry -> {
                        String keyStr = entry.getKey();
                        var valueNode = entry.getValue();
                        if (valueNode != null && !valueNode.isNull() && valueNode.isArray()) {
                            byte[] value = new byte[valueNode.size()];
                            for (int i = 0; i < valueNode.size(); i++) {
                                value[i] = (byte) valueNode.get(i).asInt();
                            }
                            results.put(keyStr, value);
                        }
                    });
                }
                
                return new ApiResponse<>(true, results, null);
            } else {
                return new ApiResponse<>(false, null, 
                    "HTTP " + response.statusCode() + ": " + response.body());
            }
        } catch (Exception e) {
            return new ApiResponse<>(false, null, "Error: " + e.getMessage());
        }
    }

    public ApiResponse<List<KeyValue>> getByPrefix(String partition, String keyPrefix, Integer limit) {
        try {
            GetByPrefixRequest request = new GetByPrefixRequest(partition, keyPrefix, limit);
            String jsonRequest = objectMapper.writeValueAsString(request);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/get_by_prefix"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonRequest))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, 
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                var responseBody = objectMapper.readTree(response.body());
                String error = responseBody.has("error") && !responseBody.get("error").isNull() 
                    ? responseBody.get("error").asText() : null;
                
                if (error != null) {
                    return new ApiResponse<>(false, null, error);
                }

                List<KeyValue> results = new java.util.ArrayList<>();
                var resultsNode = responseBody.get("results");
                if (resultsNode != null && resultsNode.isArray()) {
                    for (var resultNode : resultsNode) {
                        KeyValue kv = new KeyValue();
                        kv.key = resultNode.get("key").asText();
                        var valueArray = resultNode.get("value");
                        kv.value = new byte[valueArray.size()];
                        for (int i = 0; i < valueArray.size(); i++) {
                            kv.value[i] = (byte) valueArray.get(i).asInt();
                        }
                        results.add(kv);
                    }
                }
                
                return new ApiResponse<>(true, results, null);
            } else {
                return new ApiResponse<>(false, null, 
                    "HTTP " + response.statusCode() + ": " + response.body());
            }
        } catch (Exception e) {
            return new ApiResponse<>(false, null, "Error: " + e.getMessage());
        }
    }

    public boolean health() {
        try {
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/health"))
                    .header("Content-Type", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, 
                    HttpResponse.BodyHandlers.ofString());

            return response.statusCode() == 200 && "OK".equals(response.body());
        } catch (Exception e) {
            return false;
        }
    }
}