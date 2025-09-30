# Kubernetes Deployment Guide

This directory contains Kubernetes manifests and deployment scripts for running RustRocks Service in production.

## Quick Start

```bash
# Deploy with default configuration
./deploy.sh

# Check deployment status
kubectl get pods -n rustrocks -w

# Test the deployment
./deploy.sh test
```

## Files Overview

- **`namespace.yaml`**: Creates the `rustrocks` namespace
- **`configmap.yaml`**: Configuration settings and initialization scripts
- **`statefulset.yaml`**: Main application deployment with persistent storage
- **`services.yaml`**: Service definitions for internal and external access
- **`storage.yaml`**: Storage classes and persistent volume configurations
- **`rbac.yaml`**: Security and access control configurations
- **`deploy.sh`**: Automated deployment script

## Configuration Options

### Environment Variables

The deployment script accepts several environment variables:

```bash
# Image configuration
export IMAGE_TAG="latest"
export DOCKER_REGISTRY="your-registry.com"

# Cluster configuration
export REPLICAS=3
export STORAGE_CLASS="rustrocks-fast-ssd"

# Deployment options
export BUILD_IMAGE="true"
export RUN_TESTS="true"
```

### Storage Configuration

Choose the appropriate storage class for your workload:

#### High Performance (Production)
```yaml
storageClassName: rustrocks-fast-ssd
```
- Uses high IOPS SSDs (10,000+ IOPS)
- Recommended for production workloads
- Higher cost but best performance

#### Standard (Development/Testing)
```yaml
storageClassName: rustrocks-standard-ssd
```
- Uses standard SSDs
- Good balance of performance and cost
- Suitable for development and light production

#### Local Storage (Development Only)
```yaml
storageClassName: local-storage
```
- Uses local node storage
- Highest performance but no durability guarantees
- Only suitable for testing

### Resource Requirements

#### Minimum Requirements (per pod)
```yaml
resources:
  requests:
    memory: "2Gi"
    cpu: "500m"
  limits:
    memory: "4Gi"
    cpu: "2"
```

#### Production Requirements (per pod)
```yaml
resources:
  requests:
    memory: "4Gi"
    cpu: "1"
  limits:
    memory: "8Gi"
    cpu: "4"
```

## Cloud Provider Specific Configurations

### AWS

#### EBS Storage Classes
```yaml
# High performance
parameters:
  type: gp3
  iops: "10000"
  throughput: "500"

# Standard
parameters:
  type: gp2
```

#### Load Balancer Annotations
```yaml
metadata:
  annotations:
    service.beta.kubernetes.io/aws-load-balancer-type: "nlb"
    service.beta.kubernetes.io/aws-load-balancer-internal: "true"
```

### GCP

#### Persistent Disk Classes
```yaml
# High performance
parameters:
  type: pd-ssd
  replication-type: regional-pd

# Standard
parameters:
  type: pd-standard
```

### Azure

#### Managed Disk Classes
```yaml
# High performance
parameters:
  kind: managed
  storageaccounttype: Premium_LRS

# Standard
parameters:
  kind: managed
  storageaccounttype: StandardSSD_LRS
```

## Deployment Scenarios

### Single Region Deployment

For applications that don't require geographic distribution:

```bash
# Deploy 3-node cluster
REPLICAS=3 ./deploy.sh
```

### Multi-Region Deployment

For geographic distribution, deploy separate clusters in each region:

```bash
# Region 1
NAMESPACE=rustrocks-us-west ./deploy.sh

# Region 2
NAMESPACE=rustrocks-us-east ./deploy.sh

# Configure cross-region replication manually
```

### Development Environment

```bash
# Minimal resources for development
REPLICAS=1 STORAGE_CLASS=local-storage ./deploy.sh
```

### Production Environment

```bash
# Production deployment
REPLICAS=5 STORAGE_CLASS=rustrocks-fast-ssd BUILD_IMAGE=false ./deploy.sh
```

## Monitoring and Observability

### Built-in Health Checks

The StatefulSet includes comprehensive health checks:

- **Startup Probe**: 5-minute startup window
- **Liveness Probe**: Restarts unhealthy containers
- **Readiness Probe**: Removes pods from load balancer when not ready

### Log Aggregation

```bash
# Stream logs from all pods
kubectl logs -f -l app.kubernetes.io/name=rustrocks-service -n rustrocks

# Get logs from specific pod
kubectl logs rustrocks-service-0 -n rustrocks

# Filter logs for errors
kubectl logs -l app.kubernetes.io/name=rustrocks-service -n rustrocks | grep ERROR
```

### Metrics Collection

#### Port Forward for Metrics
```bash
kubectl port-forward svc/rustrocks-service 8080:8080 -n rustrocks &
curl http://localhost:8080/health
```

#### Integration with Prometheus

Add these annotations to the StatefulSet:

```yaml
metadata:
  annotations:
    prometheus.io/scrape: "true"
    prometheus.io/port: "8080"
    prometheus.io/path: "/metrics"
```

## Security Configuration

### Pod Security Standards

The deployment uses restrictive security contexts:

```yaml
securityContext:
  runAsUser: 1000
  runAsGroup: 1000
  runAsNonRoot: true
  fsGroup: 1000
  allowPrivilegeEscalation: false
  readOnlyRootFilesystem: true
  capabilities:
    drop:
    - ALL
```

### Network Policies

The deployment includes NetworkPolicy to restrict traffic:

- Allows ingress on ports 50051 and 8080
- Allows inter-pod communication for replication
- Blocks all other traffic

### RBAC

Minimal RBAC permissions are granted:

- Read access to services and endpoints (for peer discovery)
- Read access to pods (for replication setup)

## Backup and Recovery

### Volume Snapshots

#### Create Backup
```bash
kubectl create -f - <<EOF
apiVersion: snapshot.storage.k8s.io/v1
kind: VolumeSnapshot
metadata:
  name: rustrocks-backup-$(date +%Y%m%d)
  namespace: rustrocks
spec:
  volumeSnapshotClassName: rustrocks-snapshot-class
  source:
    persistentVolumeClaimName: data-rustrocks-service-0
EOF
```

#### Restore from Backup
```bash
# Create PVC from snapshot
kubectl create -f - <<EOF
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: data-rustrocks-service-0-restored
  namespace: rustrocks
spec:
  storageClassName: rustrocks-fast-ssd
  dataSource:
    name: rustrocks-backup-20240101
    kind: VolumeSnapshot
    apiGroup: snapshot.storage.k8s.io
  accessModes:
    - ReadWriteOnce
  resources:
    requests:
      storage: 100Gi
EOF
```

### Data Export

```bash
# Export all data
kubectl exec rustrocks-service-0 -n rustrocks -- tar czf - /data > rustrocks-backup.tar.gz

# Import data
kubectl exec rustrocks-service-0 -n rustrocks -- tar xzf - < rustrocks-backup.tar.gz
```

## Scaling Operations

### Horizontal Scaling

```bash
# Scale to 5 replicas
kubectl scale statefulset rustrocks-service --replicas=5 -n rustrocks

# Or update the StatefulSet manifest
kubectl patch statefulset rustrocks-service -n rustrocks -p '{"spec":{"replicas":5}}'
```

### Vertical Scaling

```bash
# Update resource limits
kubectl patch statefulset rustrocks-service -n rustrocks -p '{
  "spec": {
    "template": {
      "spec": {
        "containers": [{
          "name": "rustrocks-service",
          "resources": {
            "limits": {"memory": "16Gi", "cpu": "8"},
            "requests": {"memory": "8Gi", "cpu": "4"}
          }
        }]
      }
    }
  }
}'
```

### Storage Scaling

```bash
# Expand volume (if storage class supports it)
kubectl patch pvc data-rustrocks-service-0 -n rustrocks -p '{
  "spec": {
    "resources": {
      "requests": {
        "storage": "500Gi"
      }
    }
  }
}'
```

## Troubleshooting

### Common Issues

#### 1. Pod Stuck in Pending
```bash
# Check events
kubectl describe pod rustrocks-service-0 -n rustrocks

# Common causes:
# - Insufficient resources
# - PVC creation failure
# - Node selector constraints
```

#### 2. Service Not Accessible
```bash
# Check service endpoints
kubectl get endpoints rustrocks-service -n rustrocks

# Check if pods are ready
kubectl get pods -n rustrocks

# Test internal connectivity
kubectl run -it --rm debug --image=busybox --restart=Never -- nc -zv rustrocks-service-internal 50051
```

#### 3. High Memory Usage
```bash
# Check current resource usage
kubectl top pods -n rustrocks

# Increase memory limits
kubectl patch statefulset rustrocks-service -n rustrocks -p '{
  "spec": {
    "template": {
      "spec": {
        "containers": [{
          "name": "rustrocks-service",
          "resources": {
            "limits": {"memory": "16Gi"}
          }
        }]
      }
    }
  }
}'
```

#### 4. Replication Issues
```bash
# Check logs for replication errors
kubectl logs rustrocks-service-0 -n rustrocks | grep -i replication

# Check network connectivity between pods
kubectl exec rustrocks-service-0 -n rustrocks -- nc -zv rustrocks-service-1.rustrocks-service-headless 50051
```

### Debug Commands

```bash
# Get detailed pod information
kubectl describe pod rustrocks-service-0 -n rustrocks

# Access pod shell for debugging
kubectl exec -it rustrocks-service-0 -n rustrocks -- /bin/sh

# Check resource usage
kubectl top pods -n rustrocks

# Monitor pod events
kubectl get events -n rustrocks --sort-by=.metadata.creationTimestamp
```

## Performance Tuning

### RocksDB Optimization

The service automatically optimizes RocksDB settings, but you can tune further by:

1. Adjusting memory allocation
2. Configuring compaction settings
3. Optimizing for your workload pattern

### Kubernetes Optimization

#### Node Affinity
```yaml
spec:
  template:
    spec:
      affinity:
        podAntiAffinity:
          preferredDuringSchedulingIgnoredDuringExecution:
          - weight: 100
            podAffinityTerm:
              labelSelector:
                matchLabels:
                  app.kubernetes.io/name: rustrocks-service
              topologyKey: kubernetes.io/hostname
```

#### Resource Requests
```yaml
resources:
  requests:
    memory: "8Gi"
    cpu: "2"
    ephemeral-storage: "10Gi"
  limits:
    memory: "16Gi"
    cpu: "4"
```

## Maintenance

### Rolling Updates

```bash
# Update image version
kubectl set image statefulset/rustrocks-service rustrocks-service=rustrocks-service:v2.0.0 -n rustrocks

# Check rollout status
kubectl rollout status statefulset/rustrocks-service -n rustrocks
```

### Configuration Updates

```bash
# Update ConfigMap
kubectl patch configmap rustrocks-config -n rustrocks --patch '{"data":{"config.toml":"..."}}'

# Restart pods to pick up new configuration
kubectl rollout restart statefulset/rustrocks-service -n rustrocks
```

This guide covers the most common deployment scenarios and operational tasks. For specific issues not covered here, check the application logs and Kubernetes events for more detailed error information.