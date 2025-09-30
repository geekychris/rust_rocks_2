# Build stage
FROM rust:1.75-slim as builder

# Install necessary dependencies for RocksDB
RUN apt-get update && apt-get install -y \
    pkg-config \
    libssl-dev \
    libclang-dev \
    clang \
    cmake \
    build-essential \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy dependency files first for better caching
COPY Cargo.toml Cargo.lock ./
COPY build.rs ./
COPY proto/ ./proto/

# Create src directory with dummy main for dependency compilation
RUN mkdir src && echo "fn main() {}" > src/main.rs

# Build dependencies
RUN cargo build --release
RUN rm src/main.rs

# Copy source code
COPY src/ ./src/

# Build the application
RUN touch src/main.rs && cargo build --release

# Runtime stage
FROM debian:bookworm-slim

# Install runtime dependencies
RUN apt-get update && apt-get install -y \
    ca-certificates \
    && rm -rf /var/lib/apt/lists/*

# Create app user
RUN useradd -r -s /bin/false appuser

# Copy the binary
COPY --from=builder /app/target/release/rustrocks-service /usr/local/bin/

# Create data directory
RUN mkdir -p /app/data && chown appuser:appuser /app/data

# Switch to app user
USER appuser

# Set working directory
WORKDIR /app

# Expose ports
EXPOSE 50051 8080

# Set default command
CMD ["rustrocks-service"]