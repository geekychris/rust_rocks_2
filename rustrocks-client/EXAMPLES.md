# RustRocks Client Examples

This document provides examples of using the RustRocks client with both REST and gRPC protocols.

## Basic Usage

The client supports two protocols:
- REST (default port: 8080)
- gRPC (default port: 50051)

**NOTE**: In this setup, the gRPC protocol is the recommended choice, as it's been tested and confirmed working.

The protocol can be specified using the `-P` or `--protocol` flag:
- `-P rest` for REST API
- `-P grpc` for gRPC API

## Common Operations

### Put Operation

Store a key-value pair:

```bash
# Using REST (default)
./run.sh put users user1 "Hello REST"

# Using gRPC
./run.sh put -P grpc users user1 "Hello gRPC"

# With verbose output
./run.sh put -P grpc -v users user1 "Hello gRPC"
```

### Get Operation

Retrieve a value by key:

```bash
# Using REST
./run.sh get users user1

# Using gRPC
./run.sh get -P grpc users user1

# Get with binary output
./run.sh get -b users user1
```

### Delete Operation

Delete a key-value pair:

```bash
# Using REST
./run.sh delete users user1

# Using gRPC
./run.sh delete -P grpc users user1
```

### Multi-Get Operation

Retrieve multiple values in a single request:

```bash
# Using REST
./run.sh multi-get users:user1 users:user2 config:settings

# Using gRPC
./run.sh multi-get -P grpc users:user1 users:user2 config:settings
```

### Prefix Search

Search for keys with a specific prefix:

```bash
# Using REST
./run.sh prefix users user

# Using gRPC
./run.sh prefix -P grpc users user

# With result limit
./run.sh prefix -l 10 users user
```

### Batch Import

Import data from a CSV file:

```bash
# Using REST
./run.sh batch sample_data.csv

# Using gRPC
./run.sh batch -P grpc sample_data.csv
```

### Health Check

Check server health:

```bash
# Using REST
./run.sh health

# Using gRPC
./run.sh health -P grpc
```

## Custom Port Configuration

You can specify custom ports using the `-p` or `--port` flag:

```bash
# REST on custom port
./run.sh put -p 8081 users user1 "Hello REST"

# gRPC on custom port
./run.sh put -P grpc -p 50052 users user1 "Hello gRPC"
```

## Verbose Output

Add the `-v` or `--verbose` flag for detailed output:

```bash
# Verbose REST output
./run.sh put -v users user1 "Hello REST"

# Verbose gRPC output
./run.sh put -P grpc -v users user1 "Hello gRPC"
```

## Binary Data Handling

For binary data operations, use the `-b` or `--binary` flag:

```bash
# Store binary data (REST)
./run.sh put binary_data key1 "$(cat binary_file)"

# Retrieve binary data (prints as hex)
./run.sh get -b binary_data key1
```

## Practical Examples

Here are some practical examples that have been tested and confirmed working:

### User Management

```bash
# Create users
./run.sh put -P grpc users john "John Doe"
./run.sh put -P grpc users jane "Jane Smith"

# List all users starting with 'j'
./run.sh prefix -P grpc users j

# Get specific users
./run.sh multi-get -P grpc users:john users:jane
```

### Configuration Management

```bash
# Store configs
./run.sh put -P grpc config app.theme "dark"
./run.sh put -P grpc config app.lang "en_US"

# List all config keys
./run.sh prefix -P grpc config app

# Get specific configs
./run.sh multi-get -P grpc config:app.theme config:app.lang
```

### Session Management

```bash
# Store session data
./run.sh put -P grpc sessions user123 '{"id":"user123","name":"John"}'

# Retrieve session
./run.sh get -P grpc sessions user123

# Delete expired session
./run.sh delete -P grpc sessions expired_session
```

### Data Partitioning

```bash
# Store data in different partitions
./run.sh put -P grpc users u1 "User 1"
./run.sh put -P grpc products p1 "Product 1"
./run.sh put -P grpc orders o1 "Order 1"

# Cross-partition retrieval
./run.sh multi-get -P grpc users:u1 products:p1 orders:o1
```

### Logging

```bash
# Store logs with timestamps
./run.sh put -P grpc logs "2024-01-01_1" "System started"
./run.sh put -P grpc logs "2024-01-01_2" "User logged in"

# Retrieve logs for a specific date
./run.sh prefix -P grpc logs "2024-01-01"
```

## CSV Batch Import Format

The batch import expects a CSV file with the following format:

```csv
partition,key,value
users,user1,John Doe
users,user2,Jane Smith
config,theme,dark
```

## Error Handling

The client provides meaningful error messages:

- Connection errors
- Key not found
- Server errors
- Invalid input format

Example error outputs:

```bash
# Key not found
./run.sh get users nonexistent
✗ Get failed: Key not found

# Invalid partition
./run.sh put "" key1 "value"
Error: Partition name cannot be empty
```