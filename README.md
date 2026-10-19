<p align="center">
  <img src="assets/logo.png" alt="Rust Rocks 2 logo" width="180">
</p>

# RustRocks Service

A high-performance RocksDB service written in Rust that provides both REST and gRPC APIs with built-in replication support.

## Features

- **Dual APIs**: Both REST and gRPC interfaces for maximum compatibility
- **Partitioned Storage**: Support for multiple databases using partition/key composite keys
- **Replication**: Built-in replication between multiple nodes with automatic failover
- **High Performance**: Leverages RocksDB's performance with async Rust
- **Flexible Configuration**: Command-line arguments and TOML configuration files
- **Rich Operations**: 
  - PUT/GET operations
  - Multi-GET for batch operations
  - GET by prefix with optional limits
  - DELETE operations

## Quick Start

### Build the service

```bash
cargo build --release
```

### Run a single node

```bash
./target/release/rustrocks-service
```

This starts the service with:
- gRPC server on port 50051
- REST server on port 8080
- Data stored in `./data` directory

### Run with custom configuration

```bash
./target/release/rustrocks-service --config config.toml --grpc-port 50051 --rest-port 8080
```

### Run with replication

```bash
# Node 1
./target/release/rustrocks-service --grpc-port 50051 --rest-port 8080 --enable-replication

# Node 2
./target/release/rustrocks-service --grpc-port 50052 --rest-port 8081 --enable-replication --peers "node1:127.0.0.1:50051"

# Node 3
./target/release/rustrocks-service --grpc-port 50053 --rest-port 8082 --enable-replication --peers "node1:127.0.0.1:50051,node2:127.0.0.1:50052"
```

## API Usage

### REST API

All REST endpoints accept and return JSON.

#### Put a key-value pair

```bash
curl -X POST http://localhost:8080/put \
  -H "Content-Type: application/json" \
  -d '{
    "partition": "users",
    "key": "user123",
    "value": [72,101,108,108,111]
  }'
```

#### Get a value

```bash
curl -X POST http://localhost:8080/get \
  -H "Content-Type: application/json" \
  -d '{
    "partition": "users",
    "key": "user123"
  }'
```

#### Multi-get

```bash
curl -X POST http://localhost:8080/multi_get \
  -H "Content-Type: application/json" \
  -d '{
    "keys": [
      {"partition": "users", "key": "user123"},
      {"partition": "users", "key": "user456"}
    ]
  }'
```

#### Get by prefix

```bash
curl -X POST http://localhost:8080/get_by_prefix \
  -H "Content-Type: application/json" \
  -d '{
    "partition": "users",
    "key_prefix": "user",
    "limit": 10
  }'
```

#### Delete a key

```bash
curl -X POST http://localhost:8080/delete \
  -H "Content-Type: application/json" \
  -d '{
    "partition": "users",
    "key": "user123"
  }'
```

### gRPC API

Use tools like `grpcurl` or generate clients in your preferred language.

#### List available services

```bash
grpcurl -plaintext localhost:50051 list
```

#### Put a key-value pair

```bash
grpcurl -plaintext -d '{
  "partition": "users",
  "key": "user123", 
  "value": "SGVsbG8gV29ybGQ="
}' localhost:50051 rocksdb_service.RocksDbService/Put
```

#### Get a value

```bash
grpcurl -plaintext -d '{
  "partition": "users",
  "key": "user123"
}' localhost:50051 rocksdb_service.RocksDbService/Get
```

## Configuration

### Command Line Options

```
A RocksDB service with REST and gRPC APIs

Usage: rustrocks-service [OPTIONS]

Options:
  -c, --config <FILE>              Configuration file path
      --grpc-port <GRPC_PORT>      gRPC server port [default: 50051]
      --rest-port <REST_PORT>      REST server port [default: 8080]
      --data-dir <DATA_DIR>        RocksDB data directory [default: ./data]
      --node-id <NODE_ID>          Node ID (auto-generated if not provided)
      --enable-replication         Enable replication
      --peers <PEERS>              Peer nodes in format "node_id:host:port"
      --log-level <LOG_LEVEL>      Log level [default: info]
      --log-format <LOG_FORMAT>    Log format (json or pretty) [default: pretty]
  -h, --help                       Print help
```

### Configuration File

See `config.example.toml` for a complete example.

```toml
[server]
grpc_port = 50051
rest_port = 8080
rocksdb_path = "./data"

[replication]
enabled = true
[[replication.peers]]
node_id = "node-2"
address = "127.0.0.1"
port = 50052

[logging]
level = "info"
format = "pretty"
```

## Architecture

### Partitioning

The service uses a composite key system where each key is prefixed with a partition name. This allows for:
- Logical separation of data
- Efficient prefix-based queries within partitions
- Better organization and management

### Replication

The replication system uses a sequence-number based approach:
- Each write operation gets a monotonically increasing sequence number
- Nodes sync by requesting entries from their last known sequence
- Failed nodes are automatically detected and marked unhealthy
- Automatic recovery when nodes come back online

### Storage Format

Keys are stored in RocksDB with the format: `partition:key`

This allows for:
- Efficient prefix scans within a partition
- Multi-get operations across partitions
- Clear data organization

## Performance

The service is designed for high throughput and low latency:
- Async I/O throughout the stack
- Efficient RocksDB operations
- Batched replication
- Connection pooling for gRPC clients

## Development

### Running Tests

```bash
cargo test
```

### Building for Production

```bash
cargo build --release
```

### Docker Support

```dockerfile
FROM rust:1.70 as builder
WORKDIR /app
COPY . .
RUN cargo build --release

FROM ubuntu:20.04
RUN apt-get update && apt-get install -y ca-certificates
COPY --from=builder /app/target/release/rustrocks-service /usr/local/bin/
EXPOSE 50051 8080
CMD ["rustrocks-service"]
```

## Testing

### Running Unit Tests

```bash
# Run all tests
cargo test

# Run tests with output
cargo test -- --nocapture

# Run specific test module
cargo test models::tests
```

### Running Integration Tests

```bash
# Run integration tests
cargo test --test integration_tests

# Run with multiple threads
cargo test --test integration_tests -- --test-threads=1
```

### Running Benchmarks

```bash
# Run all benchmarks
cargo bench

# Run specific benchmark
cargo bench -- put_operations

# Generate HTML reports
cargo bench -- --output-format html
```

### Performance Testing

Use the included test script for basic performance testing:

```bash
./test_service.sh
```

## Build Instructions

### Prerequisites

- **Rust**: 1.70 or later
- **System Dependencies**:
  - `clang` (for RocksDB compilation)
  - `cmake` (for building native dependencies)
  - `pkg-config`
  - `libssl-dev` (on Ubuntu/Debian) or equivalent

#### macOS Setup

```bash
# Install Rust
curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh

# Install system dependencies
brew install cmake pkg-config openssl
```

#### Ubuntu/Debian Setup

```bash
# Install Rust
curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh

# Install system dependencies
sudo apt update
sudo apt install -y build-essential cmake pkg-config libssl-dev clang
```

#### RHEL/CentOS/Fedora Setup

```bash
# Install Rust
curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh

# Install system dependencies
sudo dnf install -y gcc gcc-c++ cmake pkg-config openssl-devel clang
```

### Building from Source

```bash
# Clone the repository
git clone <repository-url>
cd rustrocks-service

# Build in development mode
cargo build

# Build in release mode (optimized)
cargo build --release

# Build and run
cargo run

# Build with specific features
cargo build --release --features "feature1,feature2"
```

### Cross-Compilation

```bash
# Install target
rustup target add x86_64-unknown-linux-musl

# Build for target
cargo build --release --target x86_64-unknown-linux-musl
```

## Deployment

### Docker Deployment

#### Single Container

```bash
# Build image
docker build -t rustrocks-service .

# Run container
docker run -d \
  --name rustrocks \
  -p 50051:50051 \
  -p 8080:8080 \
  -v rustrocks-data:/app/data \
  rustrocks-service
```

#### Docker Compose Multi-Node

```bash
# Start 3-node cluster
docker-compose up -d

# Scale to 5 nodes
docker-compose up -d --scale rustrocks-node2=2 --scale rustrocks-node3=3

# Run tests
docker-compose --profile test run test-client

# View logs
docker-compose logs -f rustrocks-node1

# Stop cluster
docker-compose down
```

### Kubernetes Deployment

#### Prerequisites

- Kubernetes cluster (1.20+)
- `kubectl` configured
- Container registry access (optional)

#### Quick Deploy

```bash
# Navigate to k8s directory
cd k8s

# Deploy with default settings
./deploy.sh

# Deploy with custom settings
REPLICAS=5 STORAGE_CLASS=fast-ssd ./deploy.sh

# Check status
./deploy.sh status

# Run tests
./deploy.sh test

# Cleanup
./deploy.sh cleanup
```

#### Manual Deployment

```bash
# Create namespace
kubectl apply -f k8s/namespace.yaml

# Deploy storage classes
kubectl apply -f k8s/storage.yaml

# Deploy RBAC
kubectl apply -f k8s/rbac.yaml

# Deploy configuration
kubectl apply -f k8s/configmap.yaml

# Deploy services
kubectl apply -f k8s/services.yaml

# Deploy StatefulSet
kubectl apply -f k8s/statefulset.yaml
```

#### Monitoring Deployment

```bash
# Watch pods
kubectl get pods -n rustrocks -w

# Check logs
kubectl logs -f statefulset/rustrocks-service -n rustrocks

# Port forward for testing
kubectl port-forward svc/rustrocks-service 8080:8080 -n rustrocks

# Check resource usage
kubectl top pods -n rustrocks
```

### Production Configuration

#### Storage Classes

Choose appropriate storage class for your workload:

- **High Performance**: `rustrocks-fast-ssd` (NVMe SSD, high IOPS)
- **Standard**: `rustrocks-standard-ssd` (Standard SSD)
- **Development**: `local-storage` (Local development only)

```yaml
# Example: Update StatefulSet for high-performance storage
spec:
  volumeClaimTemplates:
  - metadata:
      name: data
    spec:
      storageClassName: rustrocks-fast-ssd  # Change this
      resources:
        requests:
          storage: 500Gi  # Adjust size as needed
```

#### Resource Limits

Adjust based on your workload:

```yaml
resources:
  requests:
    memory: "4Gi"    # Minimum memory
    cpu: "1"         # Minimum CPU
  limits:
    memory: "8Gi"    # Maximum memory
    cpu: "4"         # Maximum CPU
```

#### Replication Configuration

```yaml
spec:
  replicas: 3  # Odd numbers recommended (3, 5, 7)
```

### Health Checks and Monitoring

#### Built-in Health Endpoint

```bash
# Check health
curl http://localhost:8080/health

# Expected response
"OK"
```

#### Kubernetes Health Checks

The service includes comprehensive health checks:

- **Startup Probe**: Allows up to 5 minutes for startup
- **Liveness Probe**: Restarts pod if service becomes unresponsive
- **Readiness Probe**: Removes pod from service if not ready

#### Metrics Collection

```bash
# Port forward for metrics collection
kubectl port-forward svc/rustrocks-service 8080:8080 -n rustrocks

# Collect basic metrics
curl -s http://localhost:8080/health | jq .
```

### Backup and Recovery

#### Volume Snapshots

```bash
# Create snapshot
kubectl create -f - <<EOF
apiVersion: snapshot.storage.k8s.io/v1
kind: VolumeSnapshot
metadata:
  name: rustrocks-snapshot-$(date +%Y%m%d%H%M%S)
  namespace: rustrocks
spec:
  volumeSnapshotClassName: rustrocks-snapshot-class
  source:
    persistentVolumeClaimName: data-rustrocks-service-0
EOF

# List snapshots
kubectl get volumesnapshots -n rustrocks
```

#### Data Export/Import

```bash
# Export data using kubectl cp
kubectl exec rustrocks-service-0 -n rustrocks -- tar czf - /data > backup.tar.gz

# Import data
kubectl exec rustrocks-service-0 -n rustrocks -- tar xzf - < backup.tar.gz
```

### Troubleshooting

#### Common Issues

1. **Pod Stuck in Pending**:
   ```bash
   # Check PVC status
   kubectl get pvc -n rustrocks
   
   # Check storage class availability
   kubectl get storageclass
   
   # Describe pod for events
   kubectl describe pod rustrocks-service-0 -n rustrocks
   ```

2. **Connection Issues**:
   ```bash
   # Check service endpoints
   kubectl get endpoints -n rustrocks
   
   # Test internal connectivity
   kubectl run -it --rm debug --image=busybox --restart=Never -- \
     nc -zv rustrocks-service-internal 50051
   ```

3. **Performance Issues**:
   ```bash
   # Check resource usage
   kubectl top pods -n rustrocks
   
   # Check disk I/O
   kubectl exec rustrocks-service-0 -n rustrocks -- iostat -x 1 5
   ```

4. **Replication Issues**:
   ```bash
   # Check logs for replication errors
   kubectl logs rustrocks-service-0 -n rustrocks | grep replication
   
   # Check network policies
   kubectl get networkpolicy -n rustrocks
   ```

#### Debug Mode

```bash
# Run with debug logging
RUST_LOG=debug cargo run

# Or in Kubernetes
kubectl set env statefulset/rustrocks-service RUST_LOG=debug -n rustrocks
```

## Configuration Reference

### Command Line Options

See `cargo run -- --help` for complete options list.

### Configuration File Format

```toml
[server]
grpc_port = 50051
rest_port = 8080
rocksdb_path = "/data"
node_id = "node-1"  # Optional: auto-generated if not provided

[replication]
enabled = true
sync_interval_seconds = 10
health_check_interval_seconds = 30

[[replication.peers]]
node_id = "node-2"
address = "node-2.example.com"
port = 50051

[logging]
level = "info"        # trace, debug, info, warn, error
format = "json"       # json or pretty
file = "/var/log/rustrocks.log"  # Optional: log to file
```

### Environment Variables

- `RUST_LOG`: Override log level (e.g., `info`, `debug`)
- `RUSTROCKS_CONFIG`: Path to configuration file
- `RUSTROCKS_DATA_DIR`: Override data directory
- `RUSTROCKS_NODE_ID`: Override node ID

## License

This project is licensed under the MIT License.
