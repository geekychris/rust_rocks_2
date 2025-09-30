package com.rustrocks.client.integration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;

public class IntegrationTestBase {
    protected static final String TEST_HOST = "localhost";
    protected static final int TEST_REST_PORT = 8081;
    protected static final int TEST_GRPC_PORT = 50051;
    protected static final Path TEST_DB_PATH = Paths.get("target/test-db");
    
    private static Process serverProcess;
    private static final Runtime runtime = Runtime.getRuntime();
    
    @BeforeAll
    public static void startServer() throws IOException, InterruptedException {
        // Create test DB directory
        Files.createDirectories(TEST_DB_PATH);
        
        // Build and start the server
        ProcessBuilder builder = new ProcessBuilder(
            "cargo", "run", "--",
            "--rest-port", String.valueOf(TEST_REST_PORT),
            "--grpc-port", String.valueOf(TEST_GRPC_PORT),
            "--data-dir", TEST_DB_PATH.toString()
        );
        
        // Set working directory to Rust project root
        builder.directory(Paths.get("..").toFile());
        
        // Redirect error stream to output stream
        builder.redirectErrorStream(true);
        
        // Start the server
        serverProcess = builder.start();
        
        // Add shutdown hook to ensure server is stopped
        runtime.addShutdownHook(new Thread(() -> stopServer()));
        
        // Wait for server to start
        waitForServerToStart();
    }
    
    @AfterAll
    public static void stopServer() {
        if (serverProcess != null && serverProcess.isAlive()) {
            serverProcess.destroy();
            try {
                serverProcess.waitFor(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                serverProcess.destroyForcibly();
            }
        }
    }
    
    @BeforeEach
    public void clearTestData() throws IOException {
        // Clear all test data before each test
        Files.walk(TEST_DB_PATH)
            .filter(Files::isRegularFile)
            .forEach(path -> {
                try {
                    Files.delete(path);
                } catch (IOException e) {
                    // Ignore errors
                }
            });
    }
    
    private static void waitForServerToStart() throws IOException, InterruptedException {
        // Read server output to wait for "Service is ready" message
        BufferedReader reader = new BufferedReader(
            new InputStreamReader(serverProcess.getInputStream(), StandardCharsets.UTF_8)
        );
        
        String line;
        boolean serverReady = false;
        long startTime = System.currentTimeMillis();
        long timeout = TimeUnit.SECONDS.toMillis(30); // 30 second timeout
        
        while ((line = reader.readLine()) != null) {
            if (line.contains("Service is ready to accept requests")) {
                serverReady = true;
                break;
            }
            
            // Check timeout
            if (System.currentTimeMillis() - startTime > timeout) {
                throw new RuntimeException("Server failed to start within timeout");
            }
        }
        
        if (!serverReady) {
            throw new RuntimeException("Server failed to start");
        }
        
        // Give a little extra time for ports to be ready
        Thread.sleep(1000);
    }
    
    protected byte[] strToBytes(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }
    
    protected String bytesToStr(byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8);
    }
}