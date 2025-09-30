mod config;
mod db;
mod grpc;
mod models;
mod replication;
mod rest;

use crate::config::{Args, ConfigManager};
use crate::db::DatabaseManager;
use crate::grpc::rocksdb_service::rocks_db_service_server::RocksDbServiceServer;
use crate::grpc::RocksDbServiceImpl;
use crate::replication::ReplicationManager;
use anyhow::Result;
use clap::Parser;
use std::sync::Arc;
use tokio::sync::RwLock;
use tonic::transport::Server;
use tracing::{error, info};

#[tokio::main]
async fn main() -> Result<()> {
    // Parse command line arguments
    let args = Args::parse();

    // Initialize logging first (before any logging calls)
    ConfigManager::init_logging(&args, &None)?;

    info!("Starting Rustrocks Service");
    info!("Version: {}", env!("CARGO_PKG_VERSION"));

    // Load configuration
    let config = ConfigManager::load_config(&args)?;
    let config = Arc::new(RwLock::new(config));

    // Initialize database
    let db_path = {
        let config_read = config.read().await;
        config_read.rocksdb_path.clone()
    };
    
    info!("Initializing database at: {}", db_path);
    let db = Arc::new(DatabaseManager::new(&db_path)?);
    
    // Initialize sequence number from existing data
    db.initialize_sequence_number().await?;
    
    info!("Database initialized successfully");

    // Initialize replication manager
    let replication_manager = Arc::new(ReplicationManager::new(
        Arc::clone(&db),
        Arc::clone(&config),
    ));
    
    if config.read().await.replication_enabled {
        replication_manager.start().await?;
        info!("Replication manager started");
    }

    // Start servers
    let grpc_port = config.read().await.grpc_port;
    let rest_port = config.read().await.rest_port;

    // Start gRPC server
    let grpc_service = RocksDbServiceImpl::new(Arc::clone(&db));
    let grpc_server = start_grpc_server(grpc_service, grpc_port);

    // Start REST server
    let rest_server = rest::start_rest_server(Arc::clone(&db), rest_port);

    info!("All servers started successfully");
    info!("gRPC server listening on port {}", grpc_port);
    info!("REST server listening on port {}", rest_port);
    info!("Service is ready to accept requests");

    // Run both servers concurrently and handle shutdown
    tokio::select! {
        result = grpc_server => {
            if let Err(e) = result {
                error!("gRPC server error: {}", e);
            }
        }
        result = rest_server => {
            if let Err(e) = result {
                error!("REST server error: {}", e);
            }
        }
        _ = tokio::signal::ctrl_c() => {
            info!("Received shutdown signal");
        }
    }

    info!("Shutting down service...");
    Ok(())
}

async fn start_grpc_server(
    service: RocksDbServiceImpl,
    port: u16,
) -> Result<(), tonic::transport::Error> {
    let addr = format!("0.0.0.0:{}", port).parse().unwrap();

    // Start gRPC server
    Server::builder()
        .add_service(RocksDbServiceServer::new(service))
        .serve(addr)
        .await
}
