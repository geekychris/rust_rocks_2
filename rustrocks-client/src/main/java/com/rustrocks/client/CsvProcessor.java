package com.rustrocks.client;

import org.apache.commons.csv.*;

import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class CsvProcessor {
    
    public static class CsvRecord {
        public String partition;
        public String key;
        public byte[] value;

        public CsvRecord(String partition, String key, byte[] value) {
            this.partition = partition;
            this.key = key;
            this.value = value;
        }

        public CsvRecord(String partition, String key, String value) {
            this.partition = partition;
            this.key = key;
            this.value = value.getBytes(StandardCharsets.UTF_8);
        }
    }

    public static class BatchResult {
        public int totalRecords;
        public int successfulRecords;
        public int failedRecords;
        public List<String> errors;
        public long processingTimeMs;

        public BatchResult() {
            this.errors = new ArrayList<>();
        }

        public void addError(String error) {
            this.errors.add(error);
            this.failedRecords++;
        }

        public void recordSuccess() {
            this.successfulRecords++;
        }

        public double getSuccessRate() {
            return totalRecords > 0 ? (double) successfulRecords / totalRecords * 100.0 : 0.0;
        }
    }

    public static List<CsvRecord> readCsvFile(String filePath) throws IOException {
        List<CsvRecord> records = new ArrayList<>();
        
        try (FileReader reader = new FileReader(filePath)) {
            CSVFormat format = CSVFormat.DEFAULT
                    .withFirstRecordAsHeader()
                    .withIgnoreHeaderCase()
                    .withTrim();
            
            CSVParser parser = format.parse(reader);
            
            for (CSVRecord record : parser) {
                // Expected columns: partition, key, value
                // Alternative headers: partition_name, key_name, value_data
                String partition = getColumnValue(record, "partition", "partition_name");
                String key = getColumnValue(record, "key", "key_name");
                String value = getColumnValue(record, "value", "value_data");
                
                if (partition == null || key == null || value == null) {
                    throw new IOException(String.format(
                        "Missing required columns. Expected: partition, key, value. " +
                        "Found headers: %s. Record %d: partition=%s, key=%s, value=%s",
                        parser.getHeaderNames(), record.getRecordNumber(), partition, key, value
                    ));
                }
                
                records.add(new CsvRecord(partition, key, value));
            }
        }
        
        return records;
    }

    private static String getColumnValue(CSVRecord record, String... possibleHeaders) {
        for (String header : possibleHeaders) {
            if (record.isMapped(header)) {
                return record.get(header);
            }
        }
        return null;
    }

    public static BatchResult executeBatchPut(List<CsvRecord> records, RestClient restClient) {
        return executeBatch(records, restClient, null);
    }

    public static BatchResult executeBatchPut(List<CsvRecord> records, GrpcClient grpcClient) {
        return executeBatch(records, null, grpcClient);
    }

    private static BatchResult executeBatch(List<CsvRecord> records, RestClient restClient, GrpcClient grpcClient) {
        BatchResult result = new BatchResult();
        result.totalRecords = records.size();
        
        long startTime = System.currentTimeMillis();
        
        int recordNum = 1;
        for (CsvRecord record : records) {
            try {
                boolean success;
                String error = null;
                
                if (restClient != null) {
                    RestClient.ApiResponse<Void> response = restClient.put(
                        record.partition, record.key, record.value);
                    success = response.success;
                    error = response.error;
                } else if (grpcClient != null) {
                    GrpcClient.ApiResponse<Void> response = grpcClient.put(
                        record.partition, record.key, record.value);
                    success = response.success;
                    error = response.error;
                } else {
                    throw new IllegalStateException("No client provided");
                }
                
                if (success) {
                    result.recordSuccess();
                } else {
                    result.addError(String.format("Record %d (%s:%s): %s", 
                        recordNum, record.partition, record.key, 
                        error != null ? error : "Unknown error"));
                }
            } catch (Exception e) {
                result.addError(String.format("Record %d (%s:%s): Exception - %s", 
                    recordNum, record.partition, record.key, e.getMessage()));
            }
            
            recordNum++;
        }
        
        result.processingTimeMs = System.currentTimeMillis() - startTime;
        return result;
    }

    public static void printBatchResult(BatchResult result) {
        System.out.println("\n=== Batch Processing Results ===");
        System.out.printf("Total Records:    %d%n", result.totalRecords);
        System.out.printf("Successful:       %d%n", result.successfulRecords);
        System.out.printf("Failed:           %d%n", result.failedRecords);
        System.out.printf("Success Rate:     %.1f%%%n", result.getSuccessRate());
        System.out.printf("Processing Time:  %d ms%n", result.processingTimeMs);
        
        if (!result.errors.isEmpty()) {
            System.out.println("\nErrors:");
            int maxErrors = Math.min(10, result.errors.size());
            for (int i = 0; i < maxErrors; i++) {
                System.out.printf("  %d. %s%n", i + 1, result.errors.get(i));
            }
            if (result.errors.size() > maxErrors) {
                System.out.printf("  ... and %d more errors%n", result.errors.size() - maxErrors);
            }
        }
        System.out.println("================================");
    }
}