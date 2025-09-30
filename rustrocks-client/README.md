# RustRocks Java Client

A command-line client for the RustRocks key-value service that supports both REST and gRPC protocols.

## Features

- 🚀 **Multi-Protocol Support**: REST and gRPC communication
- 🔧 **Full CRUD Operations**: PUT, GET, DELETE operations
- 🔍 **Advanced Queries**: Multi-get and prefix search
- 📊 **Batch Processing**: CSV file upload support
- ⚡ **High Performance**: Built with Java 21 and modern libraries
- 📋 **Rich CLI**: Comprehensive command-line interface with help

## Prerequisites

- **Java 21** or later (uses Amazon Corretto 23 JDK from your environment)
- **Maven 3.6+** for building
- **RustRocks Server** running on localhost or remote host

## Quick Start

### 1. Build the Client

```bash
cd rustrocks-client
./build.sh
```

This will:
- Generate gRPC client stubs from protobuf definitions
- Compile all Java sources
- Run tests
- Create a fat JAR with all dependencies

### 2. Start the RustRocks Server

In another terminal, start your RustRocks server:
```bash
cargo run -- --rest-port 8081 --grpc-port 50051
```

### 3. Test the Connection

```bash
./run.sh health
```

## Usage

### Basic Syntax

```bash
./run.sh [GLOBAL_OPTIONS] COMMAND [COMMAND_OPTIONS] [ARGUMENTS]
```

### Global Options

- `-h, --host`: Server hostname (default: localhost)
- `-p, --port`: Server port (default: 8080 for REST, 50051 for gRPC)
- `-P, --protocol`: Protocol to use: `rest` or `grpc` (default: rest)
- `-v, --verbose`: Enable verbose output
- `--help`: Show help information

## Commands

For a comprehensive list of examples for both REST and gRPC usage, see [EXAMPLES.md](EXAMPLES.md).

### 1. Health Check

Check if the server is responding:

```bash
# REST health check
./run.sh health

# gRPC health check
./run.sh health -P grpc

# Custom host/port
./run.sh health -h myserver.com -p 8081
```

### 2. PUT - Store Key-Value Pairs

Store a value in the database:

```bash
# Basic put operation
./run.sh put users john "John Doe"

# Using gRPC
./run.sh put -P grpc users jane "Jane Smith"

# With verbose output
./run.sh put -v products laptop "Gaming Laptop"
```

**Parameters:**
- `partition`: Logical partition name
- `key`: Unique key within partition
- `value`: String value to store

### 3. GET - Retrieve Values

Retrieve a value by key:

```bash
# Basic get operation
./run.sh get users john

# Using gRPC
./run.sh get -P grpc users jane

# Binary output (hex format)
./run.sh get -b users john
```

**Parameters:**
- `partition`: Logical partition name
- `key`: Key to retrieve

**Options:**
- `-b, --binary`: Output value as hex instead of string

### 4. DELETE - Remove Keys

Delete a key from the database:

```bash
# Basic delete
./run.sh delete users john

# Using gRPC
./run.sh delete -P grpc users jane

# With verbose output
./run.sh delete -v users obsolete_user
```

### 5. MULTI-GET - Batch Retrieval

Retrieve multiple keys in a single operation:

```bash
# Multiple keys from same partition
./run.sh multi-get users:john users:jane users:bob

# Mixed partitions
./run.sh multi-get users:admin products:laptop orders:12345

# Using gRPC
./run.sh multi-get -P grpc users:john products:laptop

# Binary output
./run.sh multi-get -b users:john users:jane
```

**Key Format:** `partition:key`

### 6. PREFIX - Search by Key Prefix

Find all keys matching a prefix pattern:

```bash
# Find all keys starting with "user"
./run.sh prefix users user

# Limit results
./run.sh prefix -l 10 products gaming

# Using gRPC
./run.sh prefix -P grpc logs 2024-01

# Binary output
./run.sh prefix -b cache session_
```

**Parameters:**
- `partition`: Partition to search in
- `key_prefix`: Prefix pattern to match

**Options:**
- `-l, --limit`: Maximum number of results
- `-b, --binary`: Output values as hex

### 7. BATCH - CSV File Upload

Upload data from a CSV file:

```bash
# Basic batch upload
./run.sh batch data.csv

# Using gRPC
./run.sh batch -P grpc users.csv

# With verbose output
./run.sh batch -v large_dataset.csv

# Custom server
./run.sh batch -h production-server -p 8081 import.csv
```

## CSV File Format

The CSV file must have a header row with the following columns:

### Required Headers

- `partition`: Partition name
- `key`: Key name  
- `value`: Value data

### Alternative Headers (also supported)

- `partition_name` (instead of `partition`)
- `key_name` (instead of `key`)
- `value_data` (instead of `value`)

### Example CSV

```csv
partition,key,value
users,john,John Doe
users,jane,Jane Smith
products,laptop,Gaming Laptop 16GB RAM
orders,12345,{"customer":"john","total":1299.99}
cache,session_abc123,{"user_id":1,"expires":"2024-12-31"}
logs,2024-01-15,Application started successfully
```

## Examples

### Complete Workflow Example

```bash
# 1. Check server health
./run.sh health

# 2. Store some users
./run.sh put users john "John Doe"
./run.sh put users jane "Jane Smith"
./run.sh put users admin "System Administrator"

# 3. Retrieve individual users
./run.sh get users john
./run.sh get users jane

# 4. Get multiple users at once
./run.sh multi-get users:john users:jane users:admin

# 5. Search for users starting with 'j'
./run.sh prefix users j

# 6. Delete a user
./run.sh delete users admin

# 7. Verify deletion
./run.sh get users admin  # Should return "Key not found"
```

### Performance Testing Example

```bash
# Create test data CSV
cat > test_data.csv << EOF
partition,key,value
test,key1,Value 1
test,key2,Value 2
test,key3,Value 3
perf,benchmark1,Large data payload here
perf,benchmark2,Another large payload
EOF

# Batch upload with timing
time ./run.sh batch test_data.csv

# Verify upload
./run.sh prefix test key
./run.sh prefix perf benchmark
```

### Protocol Comparison

```bash
# Time REST operations
time ./run.sh put -P rest users test_user "Test User"
time ./run.sh get -P rest users test_user

# Time gRPC operations  
time ./run.sh put -P grpc users test_user2 "Test User 2"
time ./run.sh get -P grpc users test_user2
```

## Error Handling

The client provides clear error messages and appropriate exit codes:

- **Exit Code 0**: Success
- **Exit Code 1**: Operation failed or error occurred

### Common Error Scenarios

```bash
# Server not running
./run.sh health
# Output: ✗ Server is not responding

# Key not found
./run.sh get users nonexistent
# Output: ✗ Get failed: Key not found

# Invalid CSV format
./run.sh batch invalid.csv
# Output: Error: Missing required columns...

# Connection refused
./run.sh -h wronghost health
# Output: Error: Connection refused
```

## Advanced Usage

### Custom Ports

```bash
# REST on custom port
./run.sh -p 9090 health

# gRPC on custom port  
./run.sh -P grpc -p 50052 health
```

### Remote Servers

```bash
# Connect to remote server
./run.sh -h production.example.com -p 8080 health

# Production batch upload
./run.sh -h prod-db.company.com -P grpc batch production_data.csv
```

### Binary Data Handling

```bash
# Store binary data (will be UTF-8 encoded)
./run.sh put files image1 "$(cat image.jpg | base64)"

# Retrieve as hex
./run.sh get -b files image1
```

### Verbose Debugging

```bash
# Debug connection issues
./run.sh -v health

# Debug batch operations
./run.sh -v batch large_file.csv

# See all request/response details
./run.sh -v -P grpc multi-get users:u1 users:u2
```

## Performance Tips

1. **Use gRPC for High Performance**: gRPC is generally faster than REST
   ```bash
   ./run.sh -P grpc batch large_dataset.csv
   ```

2. **Batch Operations**: Use multi-get instead of multiple single gets
   ```bash
   # Efficient
   ./run.sh multi-get users:u1 users:u2 users:u3
   
   # Less efficient
   ./run.sh get users u1
   ./run.sh get users u2  
   ./run.sh get users u3
   ```

3. **Prefix Queries**: Use limits for large result sets
   ```bash
   ./run.sh prefix -l 100 logs 2024-01
   ```

## Troubleshooting

### Build Issues

```bash
# Clean rebuild
./build.sh

# Manual Maven commands
mvn clean compile package
```

### Connection Issues

```bash
# Test different protocols
./run.sh health
./run.sh -P grpc health

# Check server logs
# Verify firewall settings
# Confirm server is listening on expected ports
```

### CSV Issues

```bash
# Validate CSV format
head -5 your_file.csv

# Check for required headers
grep -i "partition.*key.*value" your_file.csv
```

## Technical Details

- **Java Version**: 21 (compatible with your Amazon Corretto 23 JDK)
- **Build Tool**: Maven 3.x
- **gRPC Version**: 1.58.0
- **HTTP Client**: Java 11+ HttpClient
- **CSV Processing**: Apache Commons CSV
- **CLI Framework**: PicoCLI 4.7.5

## Development

To extend or modify the client:

1. **Source Code**: Located in `src/main/java/com/rustrocks/client/`
2. **Protocol Buffers**: `src/main/proto/rocksdb_service.proto`
3. **Build Configuration**: `pom.xml`

```bash
# Development build
mvn compile exec:java -Dexec.mainClass="com.rustrocks.client.RustRocksClient" -Dexec.args="--help"

# Run tests
mvn test

# Package without tests
mvn package -DskipTests
```

---

For more information or issues, check the server logs and ensure the RustRocks service is running with the correct port configuration.