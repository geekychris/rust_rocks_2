use crate::models::*;
use anyhow::Result;
use rocksdb::{
    ColumnFamily, ColumnFamilyDescriptor, Direction, IteratorMode, Options, DB,
};
use std::collections::HashMap;
use std::path::Path;
use std::sync::Arc;
use tokio::sync::RwLock;
use tracing::{debug, info};

/// Database manager that handles multiple RocksDB instances and partitioning
pub struct DatabaseManager {
    db: Arc<DB>,
    replication_log: Arc<RwLock<Vec<ReplicationEntry>>>,
    sequence_number: Arc<RwLock<u64>>,
}

impl DatabaseManager {
    /// Create a new database manager
    pub fn new<P: AsRef<Path>>(path: P) -> Result<Self> {
        let mut opts = Options::default();
        opts.create_if_missing(true);
        opts.create_missing_column_families(true);

        // Define column families
        let cfs = vec![
            ColumnFamilyDescriptor::new("default", Options::default()),
            ColumnFamilyDescriptor::new("replication_log", Options::default()),
            ColumnFamilyDescriptor::new("metadata", Options::default()),
        ];

        let db = DB::open_cf_descriptors(&opts, path, cfs)?;
        
        info!("Database opened successfully");
        
        Ok(Self {
            db: Arc::new(db),
            replication_log: Arc::new(RwLock::new(Vec::new())),
            sequence_number: Arc::new(RwLock::new(0)),
        })
    }

    /// Put a key-value pair into the database
    pub async fn put(&self, partition: &str, key: &str, value: &[u8]) -> Result<()> {
        let composite_key = CompositeKey::new(partition.to_string(), key.to_string());
        let encoded_key = composite_key.encode();

        // Write to main database
        self.db.put(&encoded_key, value)?;

        // Add to replication log
        let seq_num = {
            let mut seq = self.sequence_number.write().await;
            *seq += 1;
            *seq
        };

        let replication_entry = ReplicationEntry {
            sequence_number: seq_num,
            operation: ReplicationOperation::Put {
                partition: partition.to_string(),
                key: key.to_string(),
                value: value.to_vec(),
            },
            timestamp: std::time::SystemTime::now()
                .duration_since(std::time::UNIX_EPOCH)?
                .as_secs(),
        };

        // Store replication entry in database
        let replication_cf = self.get_cf("replication_log")?;
        let replication_key = seq_num.to_be_bytes();
        let replication_value = bincode::serialize(&replication_entry)?;
        self.db.put_cf(replication_cf, &replication_key, &replication_value)?;

        // Also keep in memory for fast access
        self.replication_log.write().await.push(replication_entry);

        debug!("Put successful: {}:{}", partition, key);
        Ok(())
    }

    /// Get a value by composite key
    pub async fn get(&self, partition: &str, key: &str) -> Result<Option<Vec<u8>>> {
        let composite_key = CompositeKey::new(partition.to_string(), key.to_string());
        let encoded_key = composite_key.encode();

        match self.db.get(&encoded_key)? {
            Some(value) => {
                debug!("Get successful: {}:{}", partition, key);
                Ok(Some(value))
            }
            None => {
                debug!("Key not found: {}:{}", partition, key);
                Ok(None)
            }
        }
    }

    /// Get multiple values by composite keys
    pub async fn multi_get(&self, keys: &[CompositeKey]) -> Result<HashMap<CompositeKey, Option<Vec<u8>>>> {
        let encoded_keys: Vec<Vec<u8>> = keys.iter().map(|k| k.encode()).collect();
        let values = self.db.multi_get(&encoded_keys);

        let mut results = HashMap::new();
        for (i, result) in values.into_iter().enumerate() {
            let key = &keys[i];
            match result? {
                Some(value) => {
                    results.insert(key.clone(), Some(value));
                }
                None => {
                    results.insert(key.clone(), None);
                }
            }
        }

        debug!("Multi-get successful for {} keys", keys.len());
        Ok(results)
    }

    /// Get values by prefix within a partition
    pub async fn get_by_prefix(
        &self,
        partition: &str,
        key_prefix: &str,
        limit: Option<u32>,
    ) -> Result<Vec<KeyValue>> {
        let prefix = CompositeKey::key_prefix(partition, key_prefix);
        let mut results = Vec::new();
        let mut count = 0u32;

        let iter = self.db.iterator(IteratorMode::From(&prefix, Direction::Forward));
        
        for item in iter {
            let (key_bytes, value_bytes) = item?;
            
            // Check if we're still within the prefix
            if !key_bytes.starts_with(&prefix) {
                break;
            }

            // Decode the composite key to extract just the key part
            if let Ok(composite_key) = CompositeKey::decode(&key_bytes) {
                if composite_key.partition == partition {
                    results.push(KeyValue {
                        key: composite_key.key,
                        value: value_bytes.to_vec(),
                    });
                    
                    count += 1;
                    if let Some(limit) = limit {
                        if count >= limit {
                            break;
                        }
                    }
                }
            }
        }

        debug!(
            "Get by prefix successful: {}:{} returned {} results",
            partition,
            key_prefix,
            results.len()
        );
        Ok(results)
    }

    /// Delete a key-value pair
    pub async fn delete(&self, partition: &str, key: &str) -> Result<()> {
        let composite_key = CompositeKey::new(partition.to_string(), key.to_string());
        let encoded_key = composite_key.encode();

        self.db.delete(&encoded_key)?;

        // Add to replication log
        let seq_num = {
            let mut seq = self.sequence_number.write().await;
            *seq += 1;
            *seq
        };

        let replication_entry = ReplicationEntry {
            sequence_number: seq_num,
            operation: ReplicationOperation::Delete {
                partition: partition.to_string(),
                key: key.to_string(),
            },
            timestamp: std::time::SystemTime::now()
                .duration_since(std::time::UNIX_EPOCH)?
                .as_secs(),
        };

        // Store replication entry in database
        let replication_cf = self.get_cf("replication_log")?;
        let replication_key = seq_num.to_be_bytes();
        let replication_value = bincode::serialize(&replication_entry)?;
        self.db.put_cf(replication_cf, &replication_key, &replication_value)?;

        // Also keep in memory for fast access
        self.replication_log.write().await.push(replication_entry);

        debug!("Delete successful: {}:{}", partition, key);
        Ok(())
    }

    /// Get replication entries starting from a specific sequence number
    pub async fn get_replication_entries(&self, from_seq: u64) -> Result<Vec<ReplicationEntry>> {
        let replication_cf = self.get_cf("replication_log")?;
        let start_key = from_seq.to_be_bytes();
        
        let mut entries = Vec::new();
        let iter = self.db.iterator_cf(
            replication_cf,
            IteratorMode::From(&start_key, Direction::Forward),
        );

        for item in iter {
            let (_key, value) = item?;
            let entry: ReplicationEntry = bincode::deserialize(&value)?;
            entries.push(entry);
        }

        debug!("Retrieved {} replication entries from sequence {}", entries.len(), from_seq);
        Ok(entries)
    }

    /// Get the latest sequence number
    pub async fn get_latest_sequence_number(&self) -> u64 {
        *self.sequence_number.read().await
    }

    /// Helper method to get column family handle
    fn get_cf(&self, name: &str) -> Result<&ColumnFamily> {
        self.db
            .cf_handle(name)
            .ok_or_else(|| anyhow::anyhow!("Column family '{}' not found", name))
    }

    /// Initialize sequence number from existing replication log
    pub async fn initialize_sequence_number(&self) -> Result<()> {
        let replication_cf = self.get_cf("replication_log")?;
        let mut max_seq = 0u64;

        let mut iter = self.db.iterator_cf(replication_cf, IteratorMode::End);
        if let Some(Ok((key, _))) = iter.next() {
            if key.len() == 8 {
                max_seq = u64::from_be_bytes(key.as_ref().try_into().unwrap_or([0; 8]));
            }
        }

        *self.sequence_number.write().await = max_seq;
        info!("Initialized sequence number to {}", max_seq);
        Ok(())
    }
}

