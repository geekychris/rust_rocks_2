use crate::models::{NodeInfo, ServerConfig};
use anyhow::Result;
use clap::Parser;
use serde::{Deserialize, Serialize};
use std::path::PathBuf;
use tracing::{info, warn};
use tracing_subscriber::{layer::SubscriberExt, util::SubscriberInitExt};

#[derive(Parser, Debug)]
#[command(name = "rustrocks-service")]
#[command(about = "A RocksDB service with REST and gRPC APIs")]
pub struct Args {
    /// Configuration file path
    #[arg(short, long, value_name = "FILE")]
    pub config: Option<PathBuf>,

    /// gRPC server port
    #[arg(long, default_value = "50051")]
    pub grpc_port: u16,

    /// REST server port
    #[arg(long, default_value = "8080")]
    pub rest_port: u16,

    /// RocksDB data directory
    #[arg(long, default_value = "./data")]
    pub data_dir: String,

    /// Node ID (auto-generated if not provided)
    #[arg(long)]
    pub node_id: Option<String>,

    /// Enable replication
    #[arg(long)]
    pub enable_replication: bool,

    /// Peer nodes in format "node_id:host:port"
    #[arg(long, value_delimiter = ',')]
    pub peers: Vec<String>,

    /// Log level
    #[arg(long, default_value = "info")]
    pub log_level: String,

    /// Log format (json or pretty)
    #[arg(long, default_value = "pretty")]
    pub log_format: String,
}

#[derive(Debug, Serialize, Deserialize)]
pub struct ConfigFile {
    pub server: ServerSettings,
    pub replication: Option<ReplicationSettings>,
    pub logging: Option<LoggingSettings>,
}

#[derive(Debug, Serialize, Deserialize)]
pub struct ServerSettings {
    pub grpc_port: Option<u16>,
    pub rest_port: Option<u16>,
    pub rocksdb_path: Option<String>,
    pub node_id: Option<String>,
}

#[derive(Debug, Serialize, Deserialize)]
pub struct ReplicationSettings {
    pub enabled: Option<bool>,
    pub peers: Option<Vec<PeerConfig>>,
    pub sync_interval_seconds: Option<u64>,
    pub health_check_interval_seconds: Option<u64>,
}

#[derive(Debug, Serialize, Deserialize)]
pub struct PeerConfig {
    pub node_id: String,
    pub address: String,
    pub port: u16,
}

#[derive(Debug, Serialize, Deserialize)]
pub struct LoggingSettings {
    pub level: Option<String>,
    pub format: Option<String>,
    pub file: Option<String>,
}

impl Default for ConfigFile {
    fn default() -> Self {
        Self {
            server: ServerSettings {
                grpc_port: Some(50051),
                rest_port: Some(8080),
                rocksdb_path: Some("./data".to_string()),
                node_id: None,
            },
            replication: Some(ReplicationSettings {
                enabled: Some(false),
                peers: Some(vec![]),
                sync_interval_seconds: Some(10),
                health_check_interval_seconds: Some(30),
            }),
            logging: Some(LoggingSettings {
                level: Some("info".to_string()),
                format: Some("pretty".to_string()),
                file: None,
            }),
        }
    }
}

pub struct ConfigManager;

impl ConfigManager {
    /// Initialize logging based on configuration
    pub fn init_logging(args: &Args, config_file: &Option<ConfigFile>) -> Result<()> {
        let log_level = config_file
            .as_ref()
            .and_then(|c| c.logging.as_ref())
            .and_then(|l| l.level.as_ref())
            .unwrap_or(&args.log_level);

        let log_format = config_file
            .as_ref()
            .and_then(|c| c.logging.as_ref())
            .and_then(|l| l.format.as_ref())
            .unwrap_or(&args.log_format);

        // Parse log level
        let level = match log_level.to_lowercase().as_str() {
            "trace" => tracing::Level::TRACE,
            "debug" => tracing::Level::DEBUG,
            "info" => tracing::Level::INFO,
            "warn" => tracing::Level::WARN,
            "error" => tracing::Level::ERROR,
            _ => {
                eprintln!("Invalid log level '{}', using 'info'", log_level);
                tracing::Level::INFO
            }
        };

        let env_filter = tracing_subscriber::EnvFilter::builder()
            .with_default_directive(level.into())
            .from_env_lossy();

        // Configure output format
        match log_format.to_lowercase().as_str() {
            "json" => {
                tracing_subscriber::fmt()
                    .with_env_filter(env_filter)
                    .with_target(false)
                    .with_thread_ids(true)
                    .with_thread_names(true)
                    .init();
            }
            "pretty" | _ => {
                tracing_subscriber::fmt()
                    .with_env_filter(env_filter)
                    .pretty()
                    .with_thread_ids(true)
                    .with_thread_names(true)
                    .init();
            }
        }

        info!("Logging initialized with level: {}, format: {}", log_level, log_format);
        Ok(())
    }

    /// Load configuration from file and command line arguments
    pub fn load_config(args: &Args) -> Result<ServerConfig> {
        let config_file = if let Some(config_path) = &args.config {
            match std::fs::read_to_string(config_path) {
                Ok(content) => {
                    info!("Loading configuration from {:?}", config_path);
                    Some(toml::from_str::<ConfigFile>(&content)?)
                }
                Err(e) => {
                    warn!("Failed to read config file {:?}: {}", config_path, e);
                    None
                }
            }
        } else {
            None
        };

        // Merge configuration from file and command line arguments
        let server_config = Self::merge_config(args, &config_file)?;
        
        info!("Configuration loaded successfully");
        info!("gRPC port: {}", server_config.grpc_port);
        info!("REST port: {}", server_config.rest_port);
        info!("Data directory: {}", server_config.rocksdb_path);
        info!("Node ID: {}", server_config.node_id);
        info!("Replication enabled: {}", server_config.replication_enabled);
        info!("Number of peers: {}", server_config.peers.len());

        Ok(server_config)
    }

    /// Merge configuration from file and command line arguments
    fn merge_config(args: &Args, config_file: &Option<ConfigFile>) -> Result<ServerConfig> {

        // Start with defaults
        let mut config = ServerConfig {
            grpc_port: args.grpc_port,
            rest_port: args.rest_port,
            rocksdb_path: args.data_dir.clone(),
            node_id: args.node_id.clone().unwrap_or_else(|| uuid::Uuid::new_v4().to_string()),
            replication_enabled: args.enable_replication,
            peers: Vec::new(),
        };

        // Override with config file values if present
        if let Some(file_config) = config_file {
            if let Some(port) = file_config.server.grpc_port {
                if args.grpc_port == 50051 { // Only override if using default
                    config.grpc_port = port;
                }
            }

            if let Some(port) = file_config.server.rest_port {
                if args.rest_port == 8080 { // Only override if using default
                    config.rest_port = port;
                }
            }

            if let Some(path) = &file_config.server.rocksdb_path {
                if args.data_dir == "./data" { // Only override if using default
                    config.rocksdb_path = path.clone();
                }
            }

            if let Some(node_id) = &file_config.server.node_id {
                if args.node_id.is_none() { // Only use file config if not specified in args
                    config.node_id = node_id.clone();
                }
            }

            if let Some(replication) = &file_config.replication {
                if let Some(enabled) = replication.enabled {
                    if !args.enable_replication { // Only override if not explicitly enabled via args
                        config.replication_enabled = enabled;
                    }
                }

                if let Some(peers) = &replication.peers {
                    for peer_config in peers {
                        config.peers.push(NodeInfo {
                            node_id: peer_config.node_id.clone(),
                            address: peer_config.address.clone(),
                            port: peer_config.port,
                            last_sync_sequence: 0,
                        });
                    }
                }
            }
        }

        // Parse peer arguments
        for peer_str in &args.peers {
            let peer_info = Self::parse_peer_string(peer_str)?;
            config.peers.push(peer_info);
        }

        Ok(config)
    }

    /// Parse peer string in format "node_id:host:port"
    fn parse_peer_string(peer_str: &str) -> Result<NodeInfo> {
        let parts: Vec<&str> = peer_str.split(':').collect();
        if parts.len() != 3 {
            return Err(anyhow::anyhow!(
                "Invalid peer format '{}'. Expected 'node_id:host:port'",
                peer_str
            ));
        }

        let node_id = parts[0].to_string();
        let address = parts[1].to_string();
        let port = parts[2].parse::<u16>()?;

        Ok(NodeInfo {
            node_id,
            address,
            port,
            last_sync_sequence: 0,
        })
    }

    /// Generate example configuration file
    pub fn generate_example_config() -> String {
        let config = ConfigFile::default();
        toml::to_string_pretty(&config).unwrap_or_else(|_| "# Failed to generate example config".to_string())
    }

    /// Save configuration to file
    pub fn save_config(config: &ServerConfig, path: &PathBuf) -> Result<()> {
        let config_file = ConfigFile {
            server: ServerSettings {
                grpc_port: Some(config.grpc_port),
                rest_port: Some(config.rest_port),
                rocksdb_path: Some(config.rocksdb_path.clone()),
                node_id: Some(config.node_id.clone()),
            },
            replication: Some(ReplicationSettings {
                enabled: Some(config.replication_enabled),
                peers: Some(
                    config
                        .peers
                        .iter()
                        .map(|peer| PeerConfig {
                            node_id: peer.node_id.clone(),
                            address: peer.address.clone(),
                            port: peer.port,
                        })
                        .collect(),
                ),
                sync_interval_seconds: Some(10),
                health_check_interval_seconds: Some(30),
            }),
            logging: Some(LoggingSettings {
                level: Some("info".to_string()),
                format: Some("pretty".to_string()),
                file: None,
            }),
        };

        let content = toml::to_string_pretty(&config_file)?;
        std::fs::write(path, content)?;
        info!("Configuration saved to {:?}", path);

        Ok(())
    }
}