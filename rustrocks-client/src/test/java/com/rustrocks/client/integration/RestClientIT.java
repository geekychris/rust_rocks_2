package com.rustrocks.client.integration;

import com.rustrocks.client.RestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Map;

class RestClientIT extends IntegrationTestBase {
    private RestClient client;

    @BeforeEach
    void setUp() {
        client = new RestClient(TEST_HOST, TEST_REST_PORT);
    }

    @Test
    void testHealthCheck() {
        assertTrue(client.health(), "Health check should succeed");
    }

    @Test
    void testPutAndGetValue() {
        var putResponse = client.put("users", "john", strToBytes("John Doe"));
        assertTrue(putResponse.success, "Put operation should succeed");

        var getResponse = client.get("users", "john");
        assertTrue(getResponse.success, "Get operation should succeed");
        assertEquals("John Doe", bytesToStr(getResponse.data), "Retrieved value should match");
    }

    @Test
    void testGetNonexistentKey() {
        var response = client.get("users", "nonexistent");
        assertFalse(response.success, "Get should fail for nonexistent key");
        assertEquals("Key not found", response.error);
    }

    @Test
    void testDelete() {
        // Put a value first
        client.put("users", "john", strToBytes("John Doe"));

        // Delete it
        var deleteResponse = client.delete("users", "john");
        assertTrue(deleteResponse.success, "Delete operation should succeed");

        // Try to get it - should fail
        var getResponse = client.get("users", "john");
        assertFalse(getResponse.success, "Get should fail after delete");
        assertEquals("Key not found", getResponse.error);
    }

    @Test
    void testMultiGet() {
        // Put multiple values
        client.put("users", "john", strToBytes("John Doe"));
        client.put("users", "jane", strToBytes("Jane Smith"));
        client.put("products", "laptop", strToBytes("Gaming Laptop"));

        // Create composite keys
        var keys = Arrays.asList(
            new RestClient.CompositeKey("users", "john"),
            new RestClient.CompositeKey("users", "jane"),
            new RestClient.CompositeKey("products", "laptop")
        );

        // Multi-get
        var response = client.multiGet(keys);
        assertTrue(response.success, "Multi-get operation should succeed");
        assertNotNull(response.data, "Response data should not be null");

        Map<String, byte[]> results = response.data;
        assertEquals(3, results.size(), "Should return all requested keys");
        assertEquals("John Doe", bytesToStr(results.get("users:john")));
        assertEquals("Jane Smith", bytesToStr(results.get("users:jane")));
        assertEquals("Gaming Laptop", bytesToStr(results.get("products:laptop")));
    }

    @Test
    void testPrefixSearch() {
        // Put some values with common prefix
        client.put("users", "john_doe", strToBytes("John Doe"));
        client.put("users", "john_smith", strToBytes("John Smith"));
        client.put("users", "jane_doe", strToBytes("Jane Doe"));

        // Search by prefix
        var response = client.getByPrefix("users", "john_", null);
        assertTrue(response.success, "Prefix search should succeed");
        assertNotNull(response.data, "Response data should not be null");

        var results = response.data;
        assertEquals(2, results.size(), "Should find two matches");

        // Verify each result
        for (RestClient.KeyValue kv : results) {
            assertTrue(kv.key.startsWith("john_"), "Key should match prefix");
            assertTrue(
                Arrays.asList("John Doe", "John Smith").contains(bytesToStr(kv.value)),
                "Value should match expected"
            );
        }
    }

    @Test
    void testPrefixSearchWithLimit() {
        // Put several values
        for (int i = 0; i < 10; i++) {
            client.put("test", String.format("key_%03d", i), strToBytes("Value " + i));
        }

        // Search with limit
        var response = client.getByPrefix("test", "key_", 5);
        assertTrue(response.success, "Prefix search should succeed");
        assertEquals(5, response.data.size(), "Should respect the limit");
    }

    @Test
    void testPutBinaryData() {
        byte[] binaryData = new byte[256];
        for (int i = 0; i < binaryData.length; i++) {
            binaryData[i] = (byte)i;
        }

        // Put binary data
        var putResponse = client.put("binary", "test", binaryData);
        assertTrue(putResponse.success, "Put binary data should succeed");

        // Get it back
        var getResponse = client.get("binary", "test");
        assertTrue(getResponse.success, "Get binary data should succeed");
        assertArrayEquals(binaryData, getResponse.data, "Binary data should match exactly");
    }

    @Test
    void testConcurrentOperations() throws InterruptedException {
        // Run 100 concurrent operations
        Thread[] threads = new Thread[100];
        for (int i = 0; i < threads.length; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                String key = String.format("concurrent_%03d", index);
                String value = "Value " + index;
                
                var putResponse = client.put("test", key, strToBytes(value));
                assertTrue(putResponse.success, "Concurrent put should succeed");
                
                var getResponse = client.get("test", key);
                assertTrue(getResponse.success, "Concurrent get should succeed");
                assertEquals(value, bytesToStr(getResponse.data));
            });
            threads[i].start();
        }

        // Wait for all threads
        for (Thread thread : threads) {
            thread.join();
        }
    }
}