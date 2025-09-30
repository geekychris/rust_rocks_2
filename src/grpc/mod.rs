use crate::db::DatabaseManager;
use crate::models::CompositeKey;
use std::collections::HashMap;
use std::sync::Arc;
use tonic::{Request, Response, Status, Streaming};
use tracing::{debug, error};

// Include generated protobuf code
pub mod rocksdb_service {
    tonic::include_proto!("rocksdb_service");
}

use rocksdb_service::rocks_db_service_server::RocksDbService;
use rocksdb_service::*;

pub struct RocksDbServiceImpl {
    db: Arc<DatabaseManager>,
}

impl RocksDbServiceImpl {
    pub fn new(db: Arc<DatabaseManager>) -> Self {
        Self { db }
    }
}

#[tonic::async_trait]
impl RocksDbService for RocksDbServiceImpl {
    async fn put(&self, request: Request<PutRequest>) -> Result<Response<PutResponse>, Status> {
        let req = request.into_inner();
        debug!("gRPC Put request: {}:{}", req.partition, req.key);

        match self.db.put(&req.partition, &req.key, &req.value).await {
            Ok(()) => Ok(Response::new(PutResponse {
                success: true,
                error: None,
            })),
            Err(e) => {
                error!("Put operation failed: {}", e);
                Ok(Response::new(PutResponse {
                    success: false,
                    error: Some(e.to_string()),
                }))
            }
        }
    }

    async fn get(&self, request: Request<GetRequest>) -> Result<Response<GetResponse>, Status> {
        let req = request.into_inner();
        debug!("gRPC Get request: {}:{}", req.partition, req.key);

        match self.db.get(&req.partition, &req.key).await {
            Ok(Some(value)) => Ok(Response::new(GetResponse {
                found: true,
                value: Some(value),
                error: None,
            })),
            Ok(None) => Ok(Response::new(GetResponse {
                found: false,
                value: None,
                error: None,
            })),
            Err(e) => {
                error!("Get operation failed: {}", e);
                Ok(Response::new(GetResponse {
                    found: false,
                    value: None,
                    error: Some(e.to_string()),
                }))
            }
        }
    }

    async fn multi_get(
        &self,
        request: Request<MultiGetRequest>,
    ) -> Result<Response<MultiGetResponse>, Status> {
        let req = request.into_inner();
        debug!("gRPC MultiGet request for {} keys", req.keys.len());

        // Convert protobuf keys to internal CompositeKey format
        let composite_keys: Vec<CompositeKey> = req
            .keys
            .iter()
            .map(|k| CompositeKey::new(k.partition.clone(), k.key.clone()))
            .collect();

        match self.db.multi_get(&composite_keys).await {
            Ok(results) => {
                let mut response_results = Vec::new();
                for (key, value) in results {
                    response_results.push(KeyValueResult {
                        key: Some(rocksdb_service::CompositeKey {
                            partition: key.partition,
                            key: key.key,
                        }),
                        value,
                    });
                }

                Ok(Response::new(MultiGetResponse {
                    results: response_results,
                    error: None,
                }))
            }
            Err(e) => {
                error!("MultiGet operation failed: {}", e);
                Ok(Response::new(MultiGetResponse {
                    results: vec![],
                    error: Some(e.to_string()),
                }))
            }
        }
    }

    async fn get_by_prefix(
        &self,
        request: Request<GetByPrefixRequest>,
    ) -> Result<Response<GetByPrefixResponse>, Status> {
        let req = request.into_inner();
        debug!(
            "gRPC GetByPrefix request: {}:{}",
            req.partition, req.key_prefix
        );

        match self
            .db
            .get_by_prefix(&req.partition, &req.key_prefix, req.limit)
            .await
        {
            Ok(results) => {
                let response_results: Vec<rocksdb_service::KeyValue> = results
                    .into_iter()
                    .map(|kv| rocksdb_service::KeyValue {
                        key: kv.key,
                        value: kv.value,
                    })
                    .collect();

                Ok(Response::new(GetByPrefixResponse {
                    results: response_results,
                    error: None,
                }))
            }
            Err(e) => {
                error!("GetByPrefix operation failed: {}", e);
                Ok(Response::new(GetByPrefixResponse {
                    results: vec![],
                    error: Some(e.to_string()),
                }))
            }
        }
    }

    async fn delete(
        &self,
        request: Request<DeleteRequest>,
    ) -> Result<Response<DeleteResponse>, Status> {
        let req = request.into_inner();
        debug!("gRPC Delete request: {}:{}", req.partition, req.key);

        match self.db.delete(&req.partition, &req.key).await {
            Ok(()) => Ok(Response::new(DeleteResponse {
                success: true,
                error: None,
            })),
            Err(e) => {
                error!("Delete operation failed: {}", e);
                Ok(Response::new(DeleteResponse {
                    success: false,
                    error: Some(e.to_string()),
                }))
            }
        }
    }

    async fn get_replication_entries(
        &self,
        request: Request<ReplicationEntriesRequest>,
    ) -> Result<Response<ReplicationEntriesResponse>, Status> {
        let req = request.into_inner();
        debug!("gRPC GetReplicationEntries request from sequence {}", req.from_sequence);

        match self.db.get_replication_entries(req.from_sequence).await {
            Ok(entries) => {
                let response_entries: Vec<ReplicationEntry> = entries
                    .into_iter()
                    .take(req.limit.unwrap_or(1000) as usize)
                    .map(|entry| {
                        let operation = match entry.operation {
                            crate::models::ReplicationOperation::Put { partition, key, value } => {
                                ReplicationOperation {
                                    operation_type: Some(
                                        replication_operation::OperationType::Put(PutOperation {
                                            partition,
                                            key,
                                            value,
                                        }),
                                    ),
                                }
                            }
                            crate::models::ReplicationOperation::Delete { partition, key } => {
                                ReplicationOperation {
                                    operation_type: Some(
                                        replication_operation::OperationType::Delete(DeleteOperation {
                                            partition,
                                            key,
                                        }),
                                    ),
                                }
                            }
                        };

                        ReplicationEntry {
                            sequence_number: entry.sequence_number,
                            operation: Some(operation),
                            timestamp: entry.timestamp,
                        }
                    })
                    .collect();

                Ok(Response::new(ReplicationEntriesResponse {
                    entries: response_entries,
                    error: None,
                }))
            }
            Err(e) => {
                error!("GetReplicationEntries operation failed: {}", e);
                Ok(Response::new(ReplicationEntriesResponse {
                    entries: vec![],
                    error: Some(e.to_string()),
                }))
            }
        }
    }

    async fn sync_replication(
        &self,
        request: Request<Streaming<ReplicationEntry>>,
    ) -> Result<Response<SyncReplicationResponse>, Status> {
        let mut stream = request.into_inner();
        let mut last_sequence = 0u64;
        let mut applied_count = 0u32;

        debug!("gRPC SyncReplication stream started");

        while let Some(entry) = stream.message().await? {
            if let Some(operation) = entry.operation {
                match operation.operation_type {
                    Some(replication_operation::OperationType::Put(put_op)) => {
                        if let Err(e) = self.db.put(&put_op.partition, &put_op.key, &put_op.value).await {
                            error!("Failed to apply replication put: {}", e);
                            return Ok(Response::new(SyncReplicationResponse {
                                success: false,
                                last_sequence_applied: last_sequence,
                                error: Some(e.to_string()),
                            }));
                        }
                    }
                    Some(replication_operation::OperationType::Delete(delete_op)) => {
                        if let Err(e) = self.db.delete(&delete_op.partition, &delete_op.key).await {
                            error!("Failed to apply replication delete: {}", e);
                            return Ok(Response::new(SyncReplicationResponse {
                                success: false,
                                last_sequence_applied: last_sequence,
                                error: Some(e.to_string()),
                            }));
                        }
                    }
                    None => {
                        error!("Invalid replication operation");
                        return Ok(Response::new(SyncReplicationResponse {
                            success: false,
                            last_sequence_applied: last_sequence,
                            error: Some("Invalid replication operation".to_string()),
                        }));
                    }
                }
            }

            last_sequence = entry.sequence_number;
            applied_count += 1;
        }

        debug!("Sync replication completed: applied {} entries, last sequence {}", applied_count, last_sequence);

        Ok(Response::new(SyncReplicationResponse {
            success: true,
            last_sequence_applied: last_sequence,
            error: None,
        }))
    }
}