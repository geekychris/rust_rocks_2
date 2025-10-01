# Instructions: Running Two RustRocks Servers with Replication

## Overview

Your RustRocks service implements a custom sequence-based replication system where:
- Each write operation gets a monotonically increasing sequence number
- Nodes sync by requesting entries from their last known sequence
- Replication happens via gRPC streaming between nodes
- Each node maintains a complete copy of the data

## Architecture

```
Node 1 (Primary)              Node 2 (Replica)
┌─────────────────┐          ┌─────────────────┐
│ gRPC: 50051     │◄────────►│ gRPC: 50052     │
│ REST: 8080      │          │ REST: 8081      │
│ Data: ./node1   │          │ Data: ./node2   │
│ ID: node-1      │          │ ID: node-2      │
└─────────────────┘          └─────────────────┘
         ▲                            ▲
         │                            │
         ▼                            ▼
┌─────────────────┐          ┌─────────────────┐
│ RocksDB         │          │ RocksDB         │
│ + Replication   │          │ + Replication   │
│ Log             │          │ Log             │
└─────────────────┘          └─────────────────┘
```

## Step 1: Build the Service

First, ensure the service builds successfully:

```bash
cd /Users/chris/code/warp_experiments/rustrocks2
cargo build --release
```

## Step 2: Prepare Configuration Files

Create configuration files for both nodes:

### Node 1 Configuration (node1.toml)

```bash
cat > node1.toml << 'EOF'
[server]
grpc_port = 50051
rest_port = 8080
rocksdb_path = "./node1_data"
node_id = "node-1"

[replication]
enabled = true
sync_interval_seconds = 10
health_check_interval_seconds = 30

[[replication.peers]]
node_id = "node-2"
address = "127.0.0.1"
port = 50052

[logging]
level = "info"
format = "pretty"
EOF
```

### Node 2 Configuration (node2.toml)

```bash
cat > node2.toml << 'EOF'
[server]
grpc_port = 50052
rest_port = 8081
rocksdb_path = "./node2_data"
node_id = "node-2"

[replication]
enabled = true
sync_interval_seconds = 10
health_check_interval_seconds = 30

[[replication.peers]]
node_id = "node-1"
address = "127.0.0.1"
port = 50051

[logging]
level = "info"
format = "pretty"
EOF
```

## Step 3: Prepare Clean Data Directories

```bash
# Clean up any existing data to start fresh
rm -rf node1_data node2_data
mkdir -p node1_data node2_data
```

## Step 4: Start Node 1 (Primary)

Open a new terminal window and start the first node:

```bash
cd /Users/chris/code/warp_experiments/rustrocks2
./target/release/rustrocks-service --config node1.toml
```

You should see output like:
```
INFO Starting Rustrocks Service
INFO Loading configuration from "node1.toml"
INFO Configuration loaded successfully
INFO gRPC port: 50051
INFO REST port: 8080
INFO Node ID: node-1
INFO Replication enabled: true
INFO Number of peers: 1
INFO Initializing database at: ./node1_data
INFO Database initialized successfully
INFO Starting replication manager
INFO All servers started successfully
INFO gRPC server listening on port 50051
INFO REST server listening on port 8080
INFO Service is ready to accept requests
```

## Step 5: Start Node 2 (Replica)

Open another terminal window and start the second node:

```bash
cd /Users/chris/code/warp_experiments/rustrocks2
./target/release/rustrocks-service --config node2.toml
```

You should see similar output for Node 2, plus replication sync logs:
```
INFO Starting replication manager
DEBUG Syncing with node node-1 from sequence 1 to 0
DEBUG Node node-1 is up to date (seq: 0)
```

## Step 6: Verify Replication Setup

### Check Server Status

In a third terminal, verify both servers are running:

```bash
# Check if both servers are listening
lsof -nP -iTCP:50051 -sTCP:LISTEN
lsof -nP -iTCP:50052 -sTCP:LISTEN
lsof -nP -iTCP:8080 -sTCP:LISTEN  
lsof -nP -iTCP:8081 -sTCP:LISTEN
```

### Test Health Endpoints

```bash
# Node 1 health
curl http://localhost:8080/health

# Node 2 health  
curl http://localhost:8081/health
```

Both should return `"OK"`.

## Step 7: Test Replication Functionality

### Test 1: Write to Node 1, Read from Node 2

```bash
# Write data to Node 1
curl -X POST http://localhost:8080/put \
  -H "Content-Type: application/json" \
  -d '{
    "partition": "users",
    "key": "alice",
    "value": [72,101,108,108,111,32,65,108,105,99,101]
  }'
```

Wait a few seconds for replication (sync happens every 10 seconds), then read from Node 2:

```bash
# Read from Node 2 (should have replicated data)
curl -X POST http://localhost:8081/get \
  -H "Content-Type: application/json" \
  -d '{
    "partition": "users", 
    "key": "alice"
  }' | jq .
```

### Test 2: Write to Node 2, Read from Node 1

```bash
# Write to Node 2
curl -X POST http://localhost:8081/put \
  -H "Content-Type: application/json" \
  -d '{
    "partition": "products",
    "key": "widget-001",
    "value": [87,105,100,103,101,116,32,49]
  }'
```

Wait and read from Node 1:

```bash
# Read from Node 1
curl -X POST http://localhost:8080/get \
  -H "Content-Type: application/json" \
  -d '{
    "partition": "products",
    "key": "widget-001" 
  }' | jq .
```

### Test 3: Batch Multi-GET Across Nodes

```bash
# Add more test data to Node 1
curl -X POST http://localhost:8080/put \
  -H "Content-Type: application/json" \
  -d '{
    "partition": "users",
    "key": "bob", 
    "value": [72,101,108,108,111,32,66,111,98]
  }'

# Wait for replication, then test multi-get from Node 2
sleep 12
curl -X POST http://localhost:8081/multi_get \
  -H "Content-Type: application/json" \
  -d '{
    "keys": [
      {"partition": "users", "key": "alice"},
      {"partition": "users", "key": "bob"},
      {"partition": "products", "key": "widget-001"}
    ]
  }' | jq .
```

## Step 8: Test Failure and Recovery Scenarios

### Test Node Failure Recovery

1. **Stop Node 2** (Ctrl+C in its terminal)
2. **Write data to Node 1** while Node 2 is down:

```bash
curl -X POST http://localhost:8080/put \
  -H "Content-Type: application/json" \
  -d '{
    "partition": "metrics",
    "key": "cpu_usage",
    "value": [57,46,53,37]
  }'
```

3. **Restart Node 2**:
```bash
./target/release/rustrocks-service --config node2.toml
```

4. **Verify automatic recovery** - Node 2 should catch up automatically. Check Node 1 logs for replication messages.

5. **Verify data consistency**:
```bash
curl -X POST http://localhost:8081/get \
  -H "Content-Type: application/json" \
  -d '{
    "partition": "metrics",
    "key": "cpu_usage"
  }' | jq .
```

## Step 9: Command-Line Alternative (Without Config Files)

You can also start the nodes using command-line arguments:

### Node 1 (Command Line)
```bash
./target/release/rustrocks-service \
  --grpc-port 50051 \
  --rest-port 8080 \
  --data-dir ./node1_data \
  --node-id node-1 \
  --enable-replication \
  --peers "node-2:127.0.0.1:50052" \
  --log-level info
```

### Node 2 (Command Line)
```bash
./target/release/rustrocks-service \
  --grpc-port 50052 \
  --rest-port 8081 \
  --data-dir ./node2_data \
  --node-id node-2 \
  --enable-replication \
  --peers "node-1:127.0.0.1:50051" \
  --log-level info
```

## Debugging and Troubleshooting

### Log Analysis

**Enable Debug Logging** for more detailed replication info:

```bash
# Set debug level in config file
[logging]
level = "debug"

# Or use environment variable
RUST_LOG=debug ./target/release/rustrocks-service --config node1.toml
```

**Key Log Messages to Watch For:**

- `Starting replication manager` - Replication enabled
- `Syncing with node X from sequence Y to Z` - Active replication
- `Successfully synced N entries with node X` - Successful sync  
- `Node X marked as unhealthy` - Node failure detected
- `Node X is now healthy` - Node recovery

### Network Connectivity Issues

**Check Port Availability:**
```bash
# Check if ports are free before starting
netstat -an | grep -E ':(50051|50052|8080|8081)' 
```

**Test gRPC Connectivity:**
```bash
# Install grpcurl if not available
brew install grpcurl

# List services on Node 1
grpcurl -plaintext localhost:50051 list

# Test basic gRPC call
grpcurl -plaintext -d '{
  "partition": "test",
  "key": "connectivity"
}' localhost:50051 rocksdb_service.RocksDbService/Get
```

### Replication State Inspection

**Monitor Sequence Numbers:**

Check the logs for sequence number progression:
- Node 1 logs: Look for "latest_local_seq" values
- Node 2 logs: Look for "last_successful_sync" updates

**Data Consistency Verification:**
```bash
# Write with timestamp to track replication timing
curl -X POST http://localhost:8080/put \
  -H "Content-Type: application/json" \
  -d "{
    \"partition\": \"debug\",
    \"key\": \"timestamp\",
    \"value\": [$(date +%s)]
  }"

# Check on both nodes
curl -X POST http://localhost:8080/get -H "Content-Type: application/json" -d '{"partition":"debug","key":"timestamp"}' 
curl -X POST http://localhost:8081/get -H "Content-Type: application/json" -d '{"partition":"debug","key":"timestamp"}'
```

### Common Issues and Solutions

**Issue: "Connection refused" errors**
- Ensure both nodes are started in correct order (Node 1 first)
- Check firewall settings aren't blocking ports
- Verify IP addresses in peer configuration

**Issue: Replication lag is high**
- Check network latency between nodes
- Adjust `sync_interval_seconds` in config (lower = more frequent sync)
- Monitor CPU/disk I/O on both nodes

**Issue: Node continuously marked as unhealthy** 
- Check if gRPC port is accessible
- Verify node_id matching between nodes
- Look for RocksDB file permission issues

**Issue: Split-brain behavior**
- This service doesn't implement leader election
- Both nodes can accept writes independently
- Conflicts resolved by sequence number (last writer wins)

### Performance Monitoring

**Basic Performance Test:**
```bash
# Create a simple load test
for i in {1..100}; do
  curl -s -X POST http://localhost:8080/put \
    -H "Content-Type: application/json" \
    -d "{
      \"partition\": \"load_test\",
      \"key\": \"key_$i\", 
      \"value\": [$(( RANDOM % 256 ))]
    }" > /dev/null
  echo "Written key_$i"
done

# Verify replication on Node 2
curl -X POST http://localhost:8081/get_by_prefix \
  -H "Content-Type: application/json" \
  -d '{
    "partition": "load_test",
    "key_prefix": "key_", 
    "limit": 10
  }' | jq '.results | length'
```

### Clean Shutdown

To cleanly stop the services:

1. Send SIGTERM (Ctrl+C) to each node
2. Wait for "Shutting down service..." message  
3. Clean up data directories if testing:
   ```bash
   rm -rf node1_data node2_data node1.toml node2.toml
   ```

## Configuration Reference

### Replication Settings

- `sync_interval_seconds`: How often nodes sync (default: 10 seconds)
- `health_check_interval_seconds`: How often to check peer health (default: 30 seconds)
- Nodes marked unhealthy after 3 consecutive failures
- Automatic recovery when nodes come back online

### Performance Tuning

- **Lower sync intervals** = Better consistency, higher network usage
- **Higher sync intervals** = Lower network usage, higher replication lag
- **More peers** = Higher replication overhead but better fault tolerance

This setup gives you a fully functional two-node RustRocks cluster with bi-directional replication, automatic failure detection, and recovery capabilities. The replication is eventually consistent with a configurable sync interval.