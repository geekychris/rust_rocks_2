use serde::{Deserialize, Serialize};
use std::collections::HashMap;

/// Composite key combining partition and key
#[derive(Debug, Clone, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub struct CompositeKey {
    pub partition: String,
    pub key: String,
}

impl CompositeKey {
    pub fn new(partition: String, key: String) -> Self {
        Self { partition, key }
    }

    /// Encode as bytes for RocksDB storage
    pub fn encode(&self) -> Vec<u8> {
        format!("{}:{}", self.partition, self.key).into_bytes()
    }

    /// Decode from bytes stored in RocksDB
    pub fn decode(bytes: &[u8]) -> Result<Self, anyhow::Error> {
        let s = String::from_utf8(bytes.to_vec())?;
        let parts: Vec<&str> = s.splitn(2, ':').collect();
        if parts.len() != 2 {
            return Err(anyhow::anyhow!("Invalid composite key format"));
        }
        Ok(Self {
            partition: parts[0].to_string(),
            key: parts[1].to_string(),
        })
    }

    /// Create prefix for partition-based queries
    pub fn partition_prefix(partition: &str) -> Vec<u8> {
        format!("{}:", partition).into_bytes()
    }

    /// Create prefix for key-based queries within a partition
    pub fn key_prefix(partition: &str, key_prefix: &str) -> Vec<u8> {
        format!("{}:{}", partition, key_prefix).into_bytes()
    }
}

/// Request/Response models for REST and gRPC APIs
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct PutRequest {
    pub partition: String,
    pub key: String,
    pub value: Vec<u8>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct PutResponse {
    pub success: bool,
    pub error: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct GetRequest {
    pub partition: String,
    pub key: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct GetResponse {
    pub found: bool,
    pub value: Option<Vec<u8>>,
    pub error: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct MultiGetRequest {
    pub keys: Vec<CompositeKey>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct MultiGetResponse {
    pub results: HashMap<String, Option<Vec<u8>>>,
    pub error: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct GetByPrefixRequest {
    pub partition: String,
    pub key_prefix: String,
    pub limit: Option<u32>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct KeyValue {
    pub key: String,
    pub value: Vec<u8>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct GetByPrefixResponse {
    pub results: Vec<KeyValue>,
    pub error: Option<String>,
}

/// Replication-related structures
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ReplicationEntry {
    pub sequence_number: u64,
    pub operation: ReplicationOperation,
    pub timestamp: u64,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub enum ReplicationOperation {
    Put {
        partition: String,
        key: String,
        value: Vec<u8>,
    },
    Delete {
        partition: String,
        key: String,
    },
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct NodeInfo {
    pub node_id: String,
    pub address: String,
    pub port: u16,
    pub last_sync_sequence: u64,
}

/// Configuration structures
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ServerConfig {
    pub grpc_port: u16,
    pub rest_port: u16,
    pub rocksdb_path: String,
    pub node_id: String,
    pub peers: Vec<NodeInfo>,
    pub replication_enabled: bool,
}

impl Default for ServerConfig {
    fn default() -> Self {
        Self {
            grpc_port: 50051,
            rest_port: 8080,
            rocksdb_path: "./data".to_string(),
            node_id: uuid::Uuid::new_v4().to_string(),
            peers: vec![],
            replication_enabled: false,
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_composite_key_encode_decode() {
        let key = CompositeKey::new("users".to_string(), "user123".to_string());
        let encoded = key.encode();
        let decoded = CompositeKey::decode(&encoded).unwrap();
        
        assert_eq!(key, decoded);
        assert_eq!(key.partition, "users");
        assert_eq!(key.key, "user123");
    }

    #[test]
    fn test_composite_key_encode_format() {
        let key = CompositeKey::new("partition".to_string(), "key".to_string());
        let encoded = key.encode();
        let expected = b"partition:key";
        
        assert_eq!(encoded, expected);
    }

    #[test]
    fn test_composite_key_decode_invalid() {
        let invalid_data = b"invalid_format";
        let result = CompositeKey::decode(invalid_data);
        
        assert!(result.is_err());
    }

    #[test]
    fn test_composite_key_decode_with_colon_in_key() {
        let data = b"partition:key:with:colons";
        let result = CompositeKey::decode(data).unwrap();
        
        assert_eq!(result.partition, "partition");
        assert_eq!(result.key, "key:with:colons");
    }

    #[test]
    fn test_partition_prefix() {
        let prefix = CompositeKey::partition_prefix("users");
        let expected = b"users:";
        
        assert_eq!(prefix, expected);
    }

    #[test]
    fn test_key_prefix() {
        let prefix = CompositeKey::key_prefix("users", "admin");
        let expected = b"users:admin";
        
        assert_eq!(prefix, expected);
    }

    #[test]
    fn test_put_request_serialization() {
        let request = PutRequest {
            partition: "test".to_string(),
            key: "key1".to_string(),
            value: vec![1, 2, 3, 4],
        };

        let json = serde_json::to_string(&request).unwrap();
        let deserialized: PutRequest = serde_json::from_str(&json).unwrap();

        assert_eq!(request.partition, deserialized.partition);
        assert_eq!(request.key, deserialized.key);
        assert_eq!(request.value, deserialized.value);
    }

    #[test]
    fn test_get_response_serialization() {
        let response = GetResponse {
            found: true,
            value: Some(vec![1, 2, 3, 4]),
            error: None,
        };

        let json = serde_json::to_string(&response).unwrap();
        let deserialized: GetResponse = serde_json::from_str(&json).unwrap();

        assert_eq!(response.found, deserialized.found);
        assert_eq!(response.value, deserialized.value);
        assert_eq!(response.error, deserialized.error);
    }

    #[test]
    fn test_multi_get_request() {
        let keys = vec![
            CompositeKey::new("users".to_string(), "user1".to_string()),
            CompositeKey::new("users".to_string(), "user2".to_string()),
        ];
        
        let request = MultiGetRequest { keys: keys.clone() };
        
        assert_eq!(request.keys.len(), 2);
        assert_eq!(request.keys[0].partition, "users");
        assert_eq!(request.keys[0].key, "user1");
    }

    #[test]
    fn test_replication_entry() {
        let entry = ReplicationEntry {
            sequence_number: 123,
            operation: ReplicationOperation::Put {
                partition: "test".to_string(),
                key: "key1".to_string(),
                value: vec![1, 2, 3],
            },
            timestamp: 1234567890,
        };

        match &entry.operation {
            ReplicationOperation::Put { partition, key, value } => {
                assert_eq!(partition, "test");
                assert_eq!(key, "key1");
                assert_eq!(value, &vec![1, 2, 3]);
            }
            _ => panic!("Expected Put operation"),
        }
    }

    #[test]
    fn test_node_info() {
        let node = NodeInfo {
            node_id: "node1".to_string(),
            address: "127.0.0.1".to_string(),
            port: 50051,
            last_sync_sequence: 0,
        };

        assert_eq!(node.node_id, "node1");
        assert_eq!(node.address, "127.0.0.1");
        assert_eq!(node.port, 50051);
    }

    #[test]
    fn test_server_config_default() {
        let config = ServerConfig::default();
        
        assert_eq!(config.grpc_port, 50051);
        assert_eq!(config.rest_port, 8080);
        assert_eq!(config.rocksdb_path, "./data");
        assert!(!config.replication_enabled);
        assert!(config.peers.is_empty());
        assert!(!config.node_id.is_empty()); // UUID should be generated
    }

    #[test]
    fn test_get_by_prefix_request() {
        let request = GetByPrefixRequest {
            partition: "users".to_string(),
            key_prefix: "admin".to_string(),
            limit: Some(10),
        };

        assert_eq!(request.partition, "users");
        assert_eq!(request.key_prefix, "admin");
        assert_eq!(request.limit, Some(10));
    }

    #[test]
    fn test_key_value() {
        let kv = KeyValue {
            key: "test_key".to_string(),
            value: vec![72, 101, 108, 108, 111], // "Hello"
        };

        assert_eq!(kv.key, "test_key");
        assert_eq!(kv.value, vec![72, 101, 108, 108, 111]);
    }
}
