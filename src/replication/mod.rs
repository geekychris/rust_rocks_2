use crate::db::DatabaseManager;
use crate::models::*;
use anyhow::Result;
use std::sync::Arc;
use std::time::Duration;
use tokio::sync::RwLock;
use tokio::time::{interval, sleep};
use tracing::{debug, error, info, warn};

/// Replication manager that handles syncing with peer nodes
pub struct ReplicationManager {
    db: Arc<DatabaseManager>,
    config: Arc<RwLock<ServerConfig>>,
    node_states: Arc<RwLock<Vec<NodeState>>>,
}

#[derive(Debug, Clone)]
struct NodeState {
    node_info: NodeInfo,
    last_successful_sync: u64,
    consecutive_failures: u32,
    is_healthy: bool,
}

impl ReplicationManager {
    pub fn new(db: Arc<DatabaseManager>, config: Arc<RwLock<ServerConfig>>) -> Self {
        Self {
            db,
            config,
            node_states: Arc::new(RwLock::new(Vec::new())),
        }
    }

    /// Initialize the replication manager and start background sync tasks
    pub async fn start(&self) -> Result<()> {
        if !self.config.read().await.replication_enabled {
            info!("Replication is disabled");
            return Ok(());
        }

        info!("Starting replication manager");

        // Initialize node states from config
        let config = self.config.read().await;
        let mut node_states = self.node_states.write().await;
        
        for peer in &config.peers {
            node_states.push(NodeState {
                node_info: peer.clone(),
                last_successful_sync: 0,
                consecutive_failures: 0,
                is_healthy: true,
            });
        }
        drop(node_states);
        drop(config);

        // Start background sync tasks
        let self_clone = self.clone();
        tokio::spawn(async move {
            if let Err(e) = self_clone.sync_loop().await {
                error!("Sync loop error: {}", e);
            }
        });

        let self_clone = self.clone();
        tokio::spawn(async move {
            if let Err(e) = self_clone.health_check_loop().await {
                error!("Health check loop error: {}", e);
            }
        });

        Ok(())
    }

    /// Main sync loop that periodically syncs with peer nodes
    async fn sync_loop(&self) -> Result<()> {
        let mut interval = interval(Duration::from_secs(10)); // Sync every 10 seconds

        loop {
            interval.tick().await;

            let node_states = self.node_states.read().await.clone();
            for node_state in node_states {
                if !node_state.is_healthy {
                    debug!("Skipping unhealthy node: {}", node_state.node_info.node_id);
                    continue;
                }

                let result = self.sync_with_node(&node_state).await;
                self.update_node_state(&node_state.node_info.node_id, result).await;
            }
        }
    }

    /// Health check loop that monitors node health
    async fn health_check_loop(&self) -> Result<()> {
        let mut interval = interval(Duration::from_secs(30)); // Health check every 30 seconds

        loop {
            interval.tick().await;

            let node_states = self.node_states.read().await.clone();
            for node_state in node_states {
                let health_result = self.check_node_health(&node_state).await;
                self.update_node_health(&node_state.node_info.node_id, health_result).await;
            }
        }
    }

    /// Sync replication entries with a specific node
    async fn sync_with_node(&self, node_state: &NodeState) -> Result<u64> {
        let latest_local_seq = self.db.get_latest_sequence_number().await;
        let from_seq = node_state.last_successful_sync + 1;

        if from_seq > latest_local_seq {
            debug!("Node {} is up to date (seq: {})", node_state.node_info.node_id, latest_local_seq);
            return Ok(latest_local_seq);
        }

        debug!(
            "Syncing with node {} from sequence {} to {}",
            node_state.node_info.node_id, from_seq, latest_local_seq
        );

        // Get replication entries to send
        let entries = self.db.get_replication_entries(from_seq).await?;
        if entries.is_empty() {
            debug!("No entries to sync with node {}", node_state.node_info.node_id);
            return Ok(latest_local_seq);
        }

        // Send entries to peer node via gRPC client
        match self.send_replication_entries(&node_state.node_info, &entries).await {
            Ok(last_applied) => {
                info!(
                    "Successfully synced {} entries with node {} up to sequence {}",
                    entries.len(),
                    node_state.node_info.node_id,
                    last_applied
                );
                Ok(last_applied)
            }
            Err(e) => {
                error!("Failed to sync with node {}: {}", node_state.node_info.node_id, e);
                Err(e)
            }
        }
    }

    /// Send replication entries to a peer node via gRPC
    async fn send_replication_entries(
        &self,
        node_info: &NodeInfo,
        entries: &[ReplicationEntry],
    ) -> Result<u64> {
        use crate::grpc::rocksdb_service::{rocks_db_service_client::RocksDbServiceClient, ReplicationEntry as ProtoReplicationEntry};
        use tonic::transport::Channel;
        
        let endpoint = format!("http://{}:{}", node_info.address, node_info.port);
        
        // Create gRPC client with timeout
        let channel = tonic::transport::Endpoint::from_shared(endpoint)?
            .timeout(Duration::from_secs(30))
            .connect()
            .await?;
            
        let mut client = RocksDbServiceClient::new(channel);

        // Convert internal replication entries to protobuf format
        let proto_entries: Vec<ProtoReplicationEntry> = entries
            .iter()
            .map(|entry| self.convert_to_proto_entry(entry))
            .collect();

        // Create streaming request
        let request = tonic::Request::new(tokio_stream::iter(proto_entries));

        // Send the stream
        let response = client.sync_replication(request).await?;
        let response = response.into_inner();

        if !response.success {
            return Err(anyhow::anyhow!(
                "Replication failed: {}",
                response.error.unwrap_or_else(|| "Unknown error".to_string())
            ));
        }

        Ok(response.last_sequence_applied)
    }

    /// Convert internal ReplicationEntry to protobuf format
    fn convert_to_proto_entry(&self, entry: &ReplicationEntry) -> crate::grpc::rocksdb_service::ReplicationEntry {
        use crate::grpc::rocksdb_service::*;

        let operation = match &entry.operation {
            crate::models::ReplicationOperation::Put { partition, key, value } => {
                replication_operation::OperationType::Put(PutOperation {
                    partition: partition.clone(),
                    key: key.clone(),
                    value: value.clone(),
                })
            }
            crate::models::ReplicationOperation::Delete { partition, key } => {
                replication_operation::OperationType::Delete(DeleteOperation {
                    partition: partition.clone(),
                    key: key.clone(),
                })
            }
        };

        ReplicationEntry {
            sequence_number: entry.sequence_number,
            operation: Some(crate::grpc::rocksdb_service::ReplicationOperation {
                operation_type: Some(operation),
            }),
            timestamp: entry.timestamp,
        }
    }

    /// Check health of a peer node
    async fn check_node_health(&self, node_state: &NodeState) -> Result<()> {
        use crate::grpc::rocksdb_service::{rocks_db_service_client::RocksDbServiceClient, GetRequest};
        use tonic::transport::Channel;

        let endpoint = format!("http://{}:{}", node_state.node_info.address, node_state.node_info.port);
        
        let channel = tonic::transport::Endpoint::from_shared(endpoint)?
            .timeout(Duration::from_secs(5))
            .connect()
            .await?;
            
        let mut client = RocksDbServiceClient::new(channel);

        // Simple health check - try to get a non-existent key
        let request = tonic::Request::new(GetRequest {
            partition: "_health_check".to_string(),
            key: "ping".to_string(),
        });

        client.get(request).await?;
        Ok(())
    }

    /// Update node state after sync attempt
    async fn update_node_state(&self, node_id: &str, sync_result: Result<u64>) {
        let mut node_states = self.node_states.write().await;
        
        if let Some(node_state) = node_states.iter_mut().find(|n| n.node_info.node_id == node_id) {
            match sync_result {
                Ok(last_seq) => {
                    node_state.last_successful_sync = last_seq;
                    node_state.consecutive_failures = 0;
                    if !node_state.is_healthy {
                        info!("Node {} is now healthy", node_id);
                        node_state.is_healthy = true;
                    }
                }
                Err(_) => {
                    node_state.consecutive_failures += 1;
                    if node_state.consecutive_failures >= 3 && node_state.is_healthy {
                        warn!("Node {} marked as unhealthy after {} consecutive failures", node_id, node_state.consecutive_failures);
                        node_state.is_healthy = false;
                    }
                }
            }
        }
    }

    /// Update node health status
    async fn update_node_health(&self, node_id: &str, health_result: Result<()>) {
        let mut node_states = self.node_states.write().await;
        
        if let Some(node_state) = node_states.iter_mut().find(|n| n.node_info.node_id == node_id) {
            match health_result {
                Ok(()) => {
                    if !node_state.is_healthy {
                        info!("Node {} health check passed, marking as healthy", node_id);
                        node_state.is_healthy = true;
                        node_state.consecutive_failures = 0;
                    }
                }
                Err(e) => {
                    debug!("Health check failed for node {}: {}", node_id, e);
                    node_state.consecutive_failures += 1;
                    if node_state.consecutive_failures >= 3 && node_state.is_healthy {
                        warn!("Node {} marked as unhealthy after health check failures", node_id);
                        node_state.is_healthy = false;
                    }
                }
            }
        }
    }

    /// Get status of all peer nodes
    pub async fn get_node_status(&self) -> Vec<NodeState> {
        self.node_states.read().await.clone()
    }

    /// Add a new peer node
    pub async fn add_peer(&self, node_info: NodeInfo) {
        let mut node_states = self.node_states.write().await;
        
        // Check if node already exists
        if !node_states.iter().any(|n| n.node_info.node_id == node_info.node_id) {
            node_states.push(NodeState {
                node_info,
                last_successful_sync: 0,
                consecutive_failures: 0,
                is_healthy: true,
            });
            info!("Added new peer node");
        }
    }

    /// Remove a peer node
    pub async fn remove_peer(&self, node_id: &str) {
        let mut node_states = self.node_states.write().await;
        node_states.retain(|n| n.node_info.node_id != node_id);
        info!("Removed peer node: {}", node_id);
    }
}

impl Clone for ReplicationManager {
    fn clone(&self) -> Self {
        Self {
            db: Arc::clone(&self.db),
            config: Arc::clone(&self.config),
            node_states: Arc::clone(&self.node_states),
        }
    }
}