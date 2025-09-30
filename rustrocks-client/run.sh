#!/bin/bash

# RustRocks Java Client Run Script

# Set the JAR file path
JAR_FILE="target/rustrocks-client-1.0.0.jar"

# Check if JAR exists
if [ ! -f "$JAR_FILE" ]; then
    echo "❌ JAR file not found: $JAR_FILE"
    echo "Please run './build.sh' first to build the project."
    exit 1
fi

# Run the JAR with all passed arguments
java -jar "$JAR_FILE" "$@"