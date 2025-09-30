#!/bin/bash

# RustRocks Java Client Build Script

set -e

echo "🚀 Building RustRocks Java Client..."

# Check if Java 21 is available
java -version

# Check if Maven is available
mvn --version

# Clean and compile
echo "📦 Cleaning previous build..."
mvn clean

echo "🏗️  Compiling and generating gRPC stubs..."
mvn compile

echo "📋 Running tests..."
mvn test

echo "🎁 Creating JAR packages..."
mvn package

echo "✅ Build completed successfully!"

# Check if the JAR was created
JAR_FILE="target/rustrocks-client-1.0.0.jar"
if [ -f "$JAR_FILE" ]; then
    echo "📦 JAR created: $JAR_FILE"
    echo "📊 JAR size: $(ls -lh $JAR_FILE | awk '{print $5}')"
else
    echo "❌ JAR file not found!"
    exit 1
fi

echo ""
echo "🎯 You can now run the client using:"
echo "  ./run.sh --help"
echo "  OR"
echo "  java -jar target/rustrocks-client-1.0.0.jar --help"