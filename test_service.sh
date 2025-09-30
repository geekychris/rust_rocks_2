#!/bin/bash

# Test script for RustRocks Service
set -e

echo "🚀 Starting RustRocks Service test..."

# Start the service in background
cargo run &
SERVER_PID=$!

# Wait for servers to start
echo "⏳ Waiting for service to start..."
sleep 5

# Test REST API
echo "🔗 Testing REST API..."

# Test PUT operation
echo "📝 Testing PUT operation..."
curl -s -X POST http://localhost:8080/put \
  -H "Content-Type: application/json" \
  -d '{
    "partition": "users",
    "key": "user123",
    "value": [72,101,108,108,111,32,87,111,114,108,100]
  }' | jq .

# Test GET operation
echo "📖 Testing GET operation..."
curl -s -X POST http://localhost:8080/get \
  -H "Content-Type: application/json" \
  -d '{
    "partition": "users",
    "key": "user123"
  }' | jq .

# Test PUT more data
echo "📝 Adding more test data..."
curl -s -X POST http://localhost:8080/put \
  -H "Content-Type: application/json" \
  -d '{
    "partition": "users",
    "key": "user456",
    "value": [84,101,115,116,32,68,97,116,97]
  }' > /dev/null

curl -s -X POST http://localhost:8080/put \
  -H "Content-Type: application/json" \
  -d '{
    "partition": "products", 
    "key": "prod001",
    "value": [80,114,111,100,117,99,116,32,49]
  }' > /dev/null

# Test Multi-GET operation
echo "📚 Testing Multi-GET operation..."
curl -s -X POST http://localhost:8080/multi_get \
  -H "Content-Type: application/json" \
  -d '{
    "keys": [
      {"partition": "users", "key": "user123"},
      {"partition": "users", "key": "user456"},
      {"partition": "products", "key": "prod001"}
    ]
  }' | jq .

# Test GET by prefix
echo "🔍 Testing GET by prefix operation..."
curl -s -X POST http://localhost:8080/get_by_prefix \
  -H "Content-Type: application/json" \
  -d '{
    "partition": "users",
    "key_prefix": "user",
    "limit": 10
  }' | jq .

# Test DELETE operation
echo "🗑️ Testing DELETE operation..."
curl -s -X POST http://localhost:8080/delete \
  -H "Content-Type: application/json" \
  -d '{
    "partition": "users",
    "key": "user456"
  }' | jq .

# Verify deletion
echo "✅ Verifying deletion..."
curl -s -X POST http://localhost:8080/get \
  -H "Content-Type: application/json" \
  -d '{
    "partition": "users",
    "key": "user456"
  }' | jq .

# Test health endpoint
echo "🏥 Testing health endpoint..."
curl -s http://localhost:8080/health

echo ""
echo "🎉 All tests completed successfully!"

# Clean up
echo "🧹 Cleaning up..."
kill $SERVER_PID
wait $SERVER_PID 2>/dev/null || true

echo "✅ Test complete!"