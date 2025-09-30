use crate::db::DatabaseManager;
use crate::models::*;
use std::collections::HashMap;
use std::convert::Infallible;
use std::sync::Arc;
use warp::http::StatusCode;
use warp::{Filter, Rejection, Reply};
use tracing::{debug, error, info};

pub fn create_routes(
    db: Arc<DatabaseManager>,
) -> impl Filter<Extract = impl warp::Reply> + Clone {
    let db_filter = warp::any().map(move || Arc::clone(&db));

    let cors = warp::cors()
        .allow_any_origin()
        .allow_headers(vec!["content-type"])
        .allow_methods(vec!["GET", "POST", "PUT", "DELETE"]);

    let health = warp::path("health")
        .and(warp::get())
        .map(|| warp::reply::with_status("OK", StatusCode::OK));

    let put_route = warp::path("put")
        .and(warp::post())
        .and(warp::body::json())
        .and(db_filter.clone())
        .and_then(handle_put);

    let get_route = warp::path("get")
        .and(warp::post())
        .and(warp::body::json())
        .and(db_filter.clone())
        .and_then(handle_get);

    let multi_get_route = warp::path("multi_get")
        .and(warp::post())
        .and(warp::body::json())
        .and(db_filter.clone())
        .and_then(handle_multi_get);

    let get_by_prefix_route = warp::path("get_by_prefix")
        .and(warp::post())
        .and(warp::body::json())
        .and(db_filter.clone())
        .and_then(handle_get_by_prefix);

    let delete_route = warp::path("delete")
        .and(warp::post())
        .and(warp::body::json())
        .and(db_filter.clone())
        .and_then(handle_delete);

    let replication_entries_route = warp::path("replication")
        .and(warp::path("entries"))
        .and(warp::get())
        .and(warp::query::<ReplicationEntriesQuery>())
        .and(db_filter.clone())
        .and_then(handle_get_replication_entries);

    let routes = health
        .or(put_route)
        .or(get_route)
        .or(multi_get_route)
        .or(get_by_prefix_route)
        .or(delete_route)
        .or(replication_entries_route);

    routes.with(cors).recover(handle_rejection)
}

#[derive(serde::Deserialize)]
struct ReplicationEntriesQuery {
    from_sequence: u64,
    limit: Option<u32>,
}

async fn handle_put(
    request: PutRequest,
    db: Arc<DatabaseManager>,
) -> Result<impl Reply, Rejection> {
    debug!("REST Put request: {}:{}", request.partition, request.key);

    match db.put(&request.partition, &request.key, &request.value).await {
        Ok(()) => Ok(warp::reply::json(&PutResponse {
            success: true,
            error: None,
        })),
        Err(e) => {
            error!("Put operation failed: {}", e);
            Ok(warp::reply::json(&PutResponse {
                success: false,
                error: Some(e.to_string()),
            }))
        }
    }
}

async fn handle_get(
    request: GetRequest,
    db: Arc<DatabaseManager>,
) -> Result<impl Reply, Rejection> {
    debug!("REST Get request: {}:{}", request.partition, request.key);

    match db.get(&request.partition, &request.key).await {
        Ok(Some(value)) => Ok(warp::reply::json(&GetResponse {
            found: true,
            value: Some(value),
            error: None,
        })),
        Ok(None) => Ok(warp::reply::json(&GetResponse {
            found: false,
            value: None,
            error: None,
        })),
        Err(e) => {
            error!("Get operation failed: {}", e);
            Ok(warp::reply::json(&GetResponse {
                found: false,
                value: None,
                error: Some(e.to_string()),
            }))
        }
    }
}

async fn handle_multi_get(
    request: MultiGetRequest,
    db: Arc<DatabaseManager>,
) -> Result<impl Reply, Rejection> {
    debug!("REST MultiGet request for {} keys", request.keys.len());

    match db.multi_get(&request.keys).await {
        Ok(results) => {
            // Convert results HashMap<CompositeKey, Option<Vec<u8>>> into HashMap<String, Option<Vec<u8>>>
            let results_str_keys: HashMap<String, Option<Vec<u8>>> = results
                .into_iter()
                .map(|(k, v)| (format!("{}:{}", k.partition, k.key), v))
                .collect();

            Ok(warp::reply::json(&MultiGetResponse {
                results: results_str_keys,
                error: None,
            }))
        },
        Err(e) => {
            error!("MultiGet operation failed: {}", e);
            Ok(warp::reply::json(&MultiGetResponse {
                results: HashMap::new(),
                error: Some(e.to_string()),
            }))
        }
    }
}

async fn handle_get_by_prefix(
    request: GetByPrefixRequest,
    db: Arc<DatabaseManager>,
) -> Result<impl Reply, Rejection> {
    debug!(
        "REST GetByPrefix request: {}:{}",
        request.partition, request.key_prefix
    );

    match db
        .get_by_prefix(&request.partition, &request.key_prefix, request.limit)
        .await
    {
        Ok(results) => Ok(warp::reply::json(&GetByPrefixResponse {
            results,
            error: None,
        })),
        Err(e) => {
            error!("GetByPrefix operation failed: {}", e);
            Ok(warp::reply::json(&GetByPrefixResponse {
                results: vec![],
                error: Some(e.to_string()),
            }))
        }
    }
}

async fn handle_delete(
    request: GetRequest, // Using GetRequest as it has the same fields as DeleteRequest
    db: Arc<DatabaseManager>,
) -> Result<impl Reply, Rejection> {
    debug!("REST Delete request: {}:{}", request.partition, request.key);

    match db.delete(&request.partition, &request.key).await {
        Ok(()) => {
            #[derive(serde::Serialize)]
            struct DeleteResponse {
                success: bool,
                error: Option<String>,
            }

            Ok(warp::reply::json(&DeleteResponse {
                success: true,
                error: None,
            }))
        }
        Err(e) => {
            error!("Delete operation failed: {}", e);

            #[derive(serde::Serialize)]
            struct DeleteResponse {
                success: bool,
                error: Option<String>,
            }

            Ok(warp::reply::json(&DeleteResponse {
                success: false,
                error: Some(e.to_string()),
            }))
        }
    }
}

async fn handle_get_replication_entries(
    query: ReplicationEntriesQuery,
    db: Arc<DatabaseManager>,
) -> Result<impl Reply, Rejection> {
    debug!("REST GetReplicationEntries request from sequence {}", query.from_sequence);

    match db.get_replication_entries(query.from_sequence).await {
        Ok(entries) => {
            let limited_entries: Vec<_> = entries
                .into_iter()
                .take(query.limit.unwrap_or(1000) as usize)
                .collect();

            #[derive(serde::Serialize)]
            struct ReplicationEntriesResponse {
                entries: Vec<ReplicationEntry>,
                error: Option<String>,
            }

            Ok(warp::reply::json(&ReplicationEntriesResponse {
                entries: limited_entries,
                error: None,
            }))
        }
        Err(e) => {
            error!("GetReplicationEntries operation failed: {}", e);

            #[derive(serde::Serialize)]
            struct ReplicationEntriesResponse {
                entries: Vec<ReplicationEntry>,
                error: Option<String>,
            }

            Ok(warp::reply::json(&ReplicationEntriesResponse {
                entries: vec![],
                error: Some(e.to_string()),
            }))
        }
    }
}

async fn handle_rejection(err: Rejection) -> Result<impl Reply, Infallible> {
    let (code, message) = if err.is_not_found() {
        (StatusCode::NOT_FOUND, "Not Found")
    } else if let Some(_) = err.find::<warp::filters::body::BodyDeserializeError>() {
        (StatusCode::BAD_REQUEST, "Invalid JSON body")
    } else if let Some(_) = err.find::<warp::reject::MethodNotAllowed>() {
        (StatusCode::METHOD_NOT_ALLOWED, "Method Not Allowed")
    } else {
        error!("Unhandled rejection: {:?}", err);
        (StatusCode::INTERNAL_SERVER_ERROR, "Internal Server Error")
    };

    let json = warp::reply::json(&serde_json::json!({
        "error": message
    }));

    Ok(warp::reply::with_status(json, code))
}

pub async fn start_rest_server(
    db: Arc<DatabaseManager>,
    port: u16,
) -> Result<(), Box<dyn std::error::Error + Send + Sync>> {
    let routes = create_routes(db);

    info!("Starting REST server on port {}", port);

    warp::serve(routes)
        .run(([0, 0, 0, 0], port))
        .await;

    Ok(())
}