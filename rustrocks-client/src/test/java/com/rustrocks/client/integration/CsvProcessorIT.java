package com.rustrocks.client.integration;

import com.rustrocks.client.CsvProcessor;
import com.rustrocks.client.RestClient;
import com.rustrocks.client.GrpcClient;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

class CsvProcessorIT extends IntegrationTestBase {
    private final String TEST_CSV_PATH = getClass()
        .getClassLoader()
        .getResource("test_data.csv")
        .getPath();

    @Test
    void testBatchImportWithRest() throws IOException {
        // Load CSV
        List<CsvProcessor.CsvRecord> records = CsvProcessor.readCsvFile(TEST_CSV_PATH);
        assertFalse(records.isEmpty(), "Should load records from CSV");
        assertEquals(10, records.size(), "Should load all records");

        // Execute batch with REST client
        RestClient restClient = new RestClient(TEST_HOST, TEST_REST_PORT);
        var result = CsvProcessor.executeBatchPut(records, restClient);

        // Verify results
        assertEquals(10, result.totalRecords, "Should process all records");
        assertEquals(10, result.successfulRecords, "All records should succeed");
        assertEquals(0, result.failedRecords, "No records should fail");
        assertEquals(100.0, result.getSuccessRate(), "Success rate should be 100%");

        // Verify data was actually stored
        var response = restClient.get("users", "john_doe");
        assertTrue(response.success, "Should be able to get stored data");
        assertEquals("John Doe", bytesToStr(response.data));
    }

    @Test
    void testBatchImportWithGrpc() throws IOException {
        // Load CSV
        List<CsvProcessor.CsvRecord> records = CsvProcessor.readCsvFile(TEST_CSV_PATH);
        assertFalse(records.isEmpty(), "Should load records from CSV");

        // Execute batch with gRPC client
        GrpcClient grpcClient = new GrpcClient(TEST_HOST, TEST_GRPC_PORT);
        var result = CsvProcessor.executeBatchPut(records, grpcClient);

        // Verify results
        assertEquals(10, result.totalRecords, "Should process all records");
        assertEquals(10, result.successfulRecords, "All records should succeed");
        assertEquals(0, result.failedRecords, "No records should fail");
        assertEquals(100.0, result.getSuccessRate(), "Success rate should be 100%");

        // Verify data was actually stored
        var response = grpcClient.get("users", "john_doe");
        assertTrue(response.success, "Should be able to get stored data");
        assertEquals("John Doe", bytesToStr(response.data));
    }

    @Test
    void testCsvWithInvalidHeaders() throws IOException {
        // Create temporary CSV with wrong headers
        Path tempFile = Files.createTempFile("test", ".csv");
        Files.writeString(tempFile, """
            wrong,headers,here
            users,john,John Doe
            """);

        try {
            assertThrows(
                IOException.class,
                () -> CsvProcessor.readCsvFile(tempFile.toString()),
                "Should throw when headers are invalid"
            );
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    void testCsvWithMalformedData() throws IOException {
        // Create temporary CSV with malformed data
        Path tempFile = Files.createTempFile("test", ".csv");
        Files.writeString(tempFile, """
            partition,key,value
            users,john,"unclosed quote
            users,valid,valid value
            """);

        try {
            assertThrows(
                IOException.class,
                () -> CsvProcessor.readCsvFile(tempFile.toString()),
                "Should throw on malformed CSV"
            );
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    void testBatchWithParallelClients() throws IOException {
        // Load test data
        List<CsvProcessor.CsvRecord> records = CsvProcessor.readCsvFile(TEST_CSV_PATH);

        // Process same data with both clients in parallel
        Thread restThread = new Thread(() -> {
            RestClient restClient = new RestClient(TEST_HOST, TEST_REST_PORT);
            var result = CsvProcessor.executeBatchPut(records, restClient);
            assertTrue(result.getSuccessRate() > 95.0, "REST batch should succeed");
        });

        Thread grpcThread = new Thread(() -> {
            GrpcClient grpcClient = new GrpcClient(TEST_HOST, TEST_GRPC_PORT);
            var result = CsvProcessor.executeBatchPut(records, grpcClient);
            assertTrue(result.getSuccessRate() > 95.0, "gRPC batch should succeed");
        });

        // Start both threads
        restThread.start();
        grpcThread.start();

        // Wait for completion
        try {
            restThread.join();
            grpcThread.join();
        } catch (InterruptedException e) {
            fail("Test interrupted", e);
        }
    }
}