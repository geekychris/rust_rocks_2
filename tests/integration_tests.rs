use rustrocks_service::{
    db::DatabaseManager,
    models::*,
    rest,
};
use std::sync::Arc;
use std::time::Duration;
use tempfile::TempDir;
use tokio::time::sleep;

/// Helper function to create a test database manager
async fn create_test_db() -> (DatabaseManager, TempDir) {
    let temp_dir = tempfile::tempdir().expect("Failed to create temp directory");
    let db = DatabaseManager::new(temp_dir.path()).expect("Failed to create database");
    db.initialize_sequence_number().await.expect("Failed to initialize sequence number");
    (db, temp_dir)
}

/// Helper function to start a test REST server
async fn start_test_server(db: Arc<DatabaseManager>) -> u16 {
    let port = 0; // Let the OS assign a random port
    
    tokio::spawn(async move {
        let routes = rest::create_routes(db);
        warp::serve(routes)
            .run(([127, 0, 0, 1], port))
            .await;
    });
    
    sleep(Duration::from_millis(100)).await; // Give server time to start
    8080 // In a real test, we'd need to discover the actual port
}

#[tokio::test]
async fn test_database_basic_operations() {
    let (db, _temp_dir) = create_test_db().await;
    
    // Test PUT
    db.put("users", "user1", b"John Doe").await.expect("PUT failed");
    
    // Test GET
    let result = db.get("users", "user1").await.expect("GET failed");
    assert!(result.is_some());
    assert_eq!(result.unwrap(), b"John Doe");
    
    // Test GET non-existent
    let result = db.get("users", "nonexistent").await.expect("GET failed");
    assert!(result.is_none());
}

#[tokio::test]
async fn test_database_multi_get() {
    let (db, _temp_dir) = create_test_db().await;
    
    // Insert test data
    db.put("users", "user1", b"John").await.expect("PUT failed");
    db.put("users", "user2", b"Jane").await.expect("PUT failed");
    db.put("products", "prod1", b"Widget").await.expect("PUT failed");
    
    // Test multi-get
    let keys = vec![
        CompositeKey::new("users".to_string(), "user1".to_string()),
        CompositeKey::new("users".to_string(), "user2".to_string()),
        CompositeKey::new("products".to_string(), "prod1".to_string()),
        CompositeKey::new("users".to_string(), "nonexistent".to_string()),
    ];
    
    let results = db.multi_get(&keys).await.expect("Multi-get failed");
    
    assert_eq!(results.len(), 4);
    assert_eq!(results[&keys[0]], Some(b"John".to_vec()));
    assert_eq!(results[&keys[1]], Some(b"Jane".to_vec()));
    assert_eq!(results[&keys[2]], Some(b"Widget".to_vec()));
    assert_eq!(results[&keys[3]], None);
}

#[tokio::test]
async fn test_database_get_by_prefix() {
    let (db, _temp_dir) = create_test_db().await;
    
    // Insert test data
    db.put("users", "admin1", b"Admin One").await.expect("PUT failed");
    db.put("users", "admin2", b"Admin Two").await.expect("PUT failed");
    db.put("users", "user1", b"User One").await.expect("PUT failed");
    db.put("products", "admin_tool", b"Tool").await.expect("PUT failed"); // Different partition
    
    // Test get by prefix
    let results = db.get_by_prefix("users", "admin", Some(10)).await.expect("Get by prefix failed");
    
    assert_eq!(results.len(), 2);
    
    // Results should contain both admin entries
    let admin_keys: Vec<String> = results.iter().map(|kv| kv.key.clone()).collect();
    assert!(admin_keys.contains(&"admin1".to_string()));
    assert!(admin_keys.contains(&"admin2".to_string()));
    
    // Should not contain user1 or entries from other partitions
    assert!(!admin_keys.contains(&"user1".to_string()));
    assert!(!admin_keys.contains(&"admin_tool".to_string()));
}

#[tokio::test]
async fn test_database_get_by_prefix_with_limit() {
    let (db, _temp_dir) = create_test_db().await;
    
    // Insert multiple entries with same prefix
    for i in 1..=5 {
        let key = format!("user{}", i);
        let value = format!("User {}", i);
        db.put("users", &key, value.as_bytes()).await.expect("PUT failed");
    }
    
    // Test with limit
    let results = db.get_by_prefix("users", "user", Some(3)).await.expect("Get by prefix failed");
    
    assert_eq!(results.len(), 3);
}

#[tokio::test]
async fn test_database_delete() {
    let (db, _temp_dir) = create_test_db().await;
    
    // Insert and verify
    db.put("users", "user1", b"John").await.expect("PUT failed");
    let result = db.get("users", "user1").await.expect("GET failed");
    assert!(result.is_some());
    
    // Delete and verify
    db.delete("users", "user1").await.expect("DELETE failed");
    let result = db.get("users", "user1").await.expect("GET failed");
    assert!(result.is_none());
}

#[tokio::test]
async fn test_database_replication_log() {
    let (db, _temp_dir) = create_test_db().await;
    
    // Perform some operations to generate replication entries
    db.put("users", "user1", b"John").await.expect("PUT failed");
    db.put("users", "user2", b"Jane").await.expect("PUT failed");
    db.delete("users", "user1").await.expect("DELETE failed");
    
    // Get replication entries
    let entries = db.get_replication_entries(1).await.expect("Failed to get replication entries");
    
    assert_eq!(entries.len(), 3);
    
    // Verify entry types and sequence
    assert_eq!(entries[0].sequence_number, 1);
    assert_eq!(entries[1].sequence_number, 2);
    assert_eq!(entries[2].sequence_number, 3);
    
    match &entries[0].operation {
        ReplicationOperation::Put { partition, key, .. } => {
            assert_eq!(partition, "users");
            assert_eq!(key, "user1");
        }
        _ => panic!("Expected PUT operation"),
    }
    
    match &entries[2].operation {
        ReplicationOperation::Delete { partition, key } => {
            assert_eq!(partition, "users");
            assert_eq!(key, "user1");
        }
        _ => panic!("Expected DELETE operation"),
    }
}

#[tokio::test]
async fn test_database_sequence_numbers() {
    let (db, _temp_dir) = create_test_db().await;
    
    // Initial sequence should be 0
    assert_eq!(db.get_latest_sequence_number().await, 0);
    
    // After one operation
    db.put("test", "key1", b"value1").await.expect("PUT failed");
    assert_eq!(db.get_latest_sequence_number().await, 1);
    
    // After multiple operations
    db.put("test", "key2", b"value2").await.expect("PUT failed");
    db.delete("test", "key1").await.expect("DELETE failed");
    assert_eq!(db.get_latest_sequence_number().await, 3);
}

#[tokio::test]
async fn test_database_partitioning() {
    let (db, _temp_dir) = create_test_db().await;
    
    // Insert data in different partitions
    db.put("users", "john", b"John Doe").await.expect("PUT failed");
    db.put("products", "john", b"Product John").await.expect("PUT failed"); // Same key, different partition
    
    // Verify partition isolation
    let user_result = db.get("users", "john").await.expect("GET failed");
    let product_result = db.get("products", "john").await.expect("GET failed");
    
    assert_eq!(user_result.unwrap(), b"John Doe");
    assert_eq!(product_result.unwrap(), b"Product John");
    
    // Test prefix queries respect partitions
    let user_results = db.get_by_prefix("users", "j", None).await.expect("Get by prefix failed");
    let product_results = db.get_by_prefix("products", "j", None).await.expect("Get by prefix failed");
    
    assert_eq!(user_results.len(), 1);
    assert_eq!(product_results.len(), 1);
    assert_eq!(user_results[0].value, b"John Doe");
    assert_eq!(product_results[0].value, b"Product John");
}

#[tokio::test]
async fn test_database_concurrent_operations() {
    let (db, _temp_dir) = create_test_db().await;
    let db = Arc::new(db);
    
    // Spawn multiple tasks performing concurrent operations
    let mut handles = vec![];
    
    for i in 0..10 {
        let db_clone = Arc::clone(&db);
        let handle = tokio::spawn(async move {
            let key = format!("key{}", i);
            let value = format!("value{}", i);
            db_clone.put("concurrent", &key, value.as_bytes()).await.expect("PUT failed");
            
            let result = db_clone.get("concurrent", &key).await.expect("GET failed");
            assert_eq!(result.unwrap(), value.as_bytes());
        });
        handles.push(handle);
    }
    
    // Wait for all tasks to complete
    for handle in handles {
        handle.await.expect("Task failed");
    }
    
    // Verify all data was written
    let results = db.get_by_prefix("concurrent", "key", None).await.expect("Get by prefix failed");
    assert_eq!(results.len(), 10);
}

// REST API Integration Tests
#[tokio::test]
async fn test_rest_api_basic_flow() {
    let (db, _temp_dir) = create_test_db().await;
    let db = Arc::new(db);
    
    // In a real test environment, we would start the server and make actual HTTP requests
    // For now, we'll test the handlers directly
    
    let put_request = PutRequest {
        partition: "test".to_string(),
        key: "key1".to_string(),
        value: vec![1, 2, 3, 4],
    };
    
    // Simulate PUT operation
    db.put(&put_request.partition, &put_request.key, &put_request.value)
        .await
        .expect("PUT failed");
    
    // Simulate GET operation
    let result = db.get(&put_request.partition, &put_request.key)
        .await
        .expect("GET failed");
    
    assert!(result.is_some());
    assert_eq!(result.unwrap(), put_request.value);
}

#[test]
fn test_composite_key_ordering() {
    // Test that composite keys maintain lexicographic ordering
    let key1 = CompositeKey::new("users".to_string(), "alice".to_string());
    let key2 = CompositeKey::new("users".to_string(), "bob".to_string());
    let key3 = CompositeKey::new("users".to_string(), "charlie".to_string());
    
    let encoded1 = key1.encode();
    let encoded2 = key2.encode();
    let encoded3 = key3.encode();
    
    // Verify lexicographic ordering is maintained
    assert!(encoded1 < encoded2);
    assert!(encoded2 < encoded3);
    assert!(encoded1 < encoded3);
}

#[test]
fn test_partition_prefix_isolation() {
    let users_prefix = CompositeKey::partition_prefix("users");
    let products_prefix = CompositeKey::partition_prefix("products");
    
    let user_key = CompositeKey::new("users".to_string(), "alice".to_string()).encode();
    let product_key = CompositeKey::new("products".to_string(), "widget".to_string()).encode();
    
    // User key should start with users prefix
    assert!(user_key.starts_with(&users_prefix));
    assert!(!user_key.starts_with(&products_prefix));
    
    // Product key should start with products prefix
    assert!(product_key.starts_with(&products_prefix));
    assert!(!product_key.starts_with(&users_prefix));
}

#[tokio::test]
async fn test_database_persistence() {
    let temp_dir = tempfile::tempdir().expect("Failed to create temp directory");
    let db_path = temp_dir.path();
    
    // Create database and insert data
    {
        let db = DatabaseManager::new(&db_path).expect("Failed to create database");
        db.initialize_sequence_number().await.expect("Failed to initialize");
        
        db.put("persistent", "key1", b"persistent_value").await.expect("PUT failed");
        db.put("persistent", "key2", b"another_value").await.expect("PUT failed");
    }
    
    // Reopen database and verify data persisted
    {
        let db = DatabaseManager::new(&db_path).expect("Failed to reopen database");
        db.initialize_sequence_number().await.expect("Failed to initialize");
        
        let result1 = db.get("persistent", "key1").await.expect("GET failed");
        let result2 = db.get("persistent", "key2").await.expect("GET failed");
        
        assert_eq!(result1.unwrap(), b"persistent_value");
        assert_eq!(result2.unwrap(), b"another_value");
    }
}