#!/bin/bash

# RustRocks Service Kubernetes Deployment Script
set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
NAMESPACE="rustrocks"
IMAGE_TAG="${IMAGE_TAG:-latest}"
STORAGE_CLASS="${STORAGE_CLASS:-rustrocks-standard-ssd}"
REPLICAS="${REPLICAS:-3}"

# Functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

# Check if kubectl is available
check_kubectl() {
    if ! command -v kubectl &> /dev/null; then
        log_error "kubectl is not installed or not in PATH"
        exit 1
    fi
    
    # Check if we can connect to the cluster
    if ! kubectl cluster-info &> /dev/null; then
        log_error "Cannot connect to Kubernetes cluster"
        exit 1
    fi
    
    log_success "kubectl is available and connected to cluster"
}

# Build and push Docker image
build_image() {
    log_info "Building Docker image..."
    
    # Build image
    docker build -t rustrocks-service:${IMAGE_TAG} ..
    
    # Tag for registry if specified
    if [[ -n "${DOCKER_REGISTRY}" ]]; then
        docker tag rustrocks-service:${IMAGE_TAG} ${DOCKER_REGISTRY}/rustrocks-service:${IMAGE_TAG}
        
        log_info "Pushing image to registry..."
        docker push ${DOCKER_REGISTRY}/rustrocks-service:${IMAGE_TAG}
    fi
    
    log_success "Docker image built successfully"
}

# Deploy namespace and RBAC
deploy_namespace() {
    log_info "Deploying namespace and RBAC..."
    
    kubectl apply -f namespace.yaml
    kubectl apply -f rbac.yaml
    
    log_success "Namespace and RBAC deployed"
}

# Deploy storage resources
deploy_storage() {
    log_info "Deploying storage resources..."
    
    # Apply storage configurations
    kubectl apply -f storage.yaml
    
    # Update StatefulSet to use specified storage class
    if [[ "${STORAGE_CLASS}" != "rustrocks-standard-ssd" ]]; then
        log_info "Using custom storage class: ${STORAGE_CLASS}"
        sed -i.bak "s/# storageClassName: fast-ssd/storageClassName: ${STORAGE_CLASS}/" statefulset.yaml
    fi
    
    log_success "Storage resources deployed"
}

# Deploy configuration
deploy_config() {
    log_info "Deploying configuration..."
    
    kubectl apply -f configmap.yaml
    
    log_success "Configuration deployed"
}

# Deploy services
deploy_services() {
    log_info "Deploying services..."
    
    kubectl apply -f services.yaml
    
    log_success "Services deployed"
}

# Deploy StatefulSet
deploy_statefulset() {
    log_info "Deploying StatefulSet..."
    
    # Update image tag if specified
    if [[ -n "${DOCKER_REGISTRY}" ]]; then
        sed -i.bak "s|image: rustrocks-service:latest|image: ${DOCKER_REGISTRY}/rustrocks-service:${IMAGE_TAG}|" statefulset.yaml
    else
        sed -i.bak "s|image: rustrocks-service:latest|image: rustrocks-service:${IMAGE_TAG}|" statefulset.yaml
    fi
    
    # Update replica count if specified
    if [[ "${REPLICAS}" != "3" ]]; then
        sed -i.bak "s/replicas: 3/replicas: ${REPLICAS}/" statefulset.yaml
        sed -i.bak "s/value: \"3\"/value: \"${REPLICAS}\"/" statefulset.yaml
    fi
    
    kubectl apply -f statefulset.yaml
    
    # Restore original file
    if [[ -f statefulset.yaml.bak ]]; then
        mv statefulset.yaml.bak statefulset.yaml
    fi
    
    log_success "StatefulSet deployed"
}

# Wait for deployment to be ready
wait_for_ready() {
    log_info "Waiting for pods to be ready..."
    
    kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=rustrocks-service -n ${NAMESPACE} --timeout=300s
    
    log_success "All pods are ready"
}

# Show deployment status
show_status() {
    log_info "Deployment Status:"
    
    echo ""
    echo "Pods:"
    kubectl get pods -n ${NAMESPACE} -l app.kubernetes.io/name=rustrocks-service
    
    echo ""
    echo "Services:"
    kubectl get svc -n ${NAMESPACE}
    
    echo ""
    echo "PersistentVolumeClaims:"
    kubectl get pvc -n ${NAMESPACE}
    
    echo ""
    echo "Access Information:"
    echo "  Internal (gRPC): rustrocks-service-internal.${NAMESPACE}.svc.cluster.local:50051"
    echo "  Internal (REST): rustrocks-service-internal.${NAMESPACE}.svc.cluster.local:8080"
    
    # Show external access if LoadBalancer service exists
    EXTERNAL_IP=$(kubectl get svc rustrocks-service -n ${NAMESPACE} -o jsonpath='{.status.loadBalancer.ingress[0].ip}' 2>/dev/null || echo "")
    if [[ -n "${EXTERNAL_IP}" ]]; then
        echo "  External (gRPC): ${EXTERNAL_IP}:50051"
        echo "  External (REST): ${EXTERNAL_IP}:8080"
    else
        echo "  External: Waiting for LoadBalancer IP assignment..."
    fi
}

# Test deployment
test_deployment() {
    log_info "Testing deployment..."
    
    # Port forward to test locally
    kubectl port-forward svc/rustrocks-service-internal 8080:8080 -n ${NAMESPACE} &
    PORT_FORWARD_PID=$!
    
    # Wait a moment for port forward to establish
    sleep 5
    
    # Test health endpoint
    if curl -f http://localhost:8080/health &> /dev/null; then
        log_success "Health check passed"
    else
        log_error "Health check failed"
        kill ${PORT_FORWARD_PID} 2>/dev/null || true
        return 1
    fi
    
    # Test PUT/GET operations
    log_info "Testing PUT/GET operations..."
    
    # Test PUT
    curl -s -X POST http://localhost:8080/put \
        -H "Content-Type: application/json" \
        -d '{"partition": "test", "key": "hello", "value": [72,101,108,108,111,32,87,111,114,108,100]}' | jq .
    
    # Test GET
    curl -s -X POST http://localhost:8080/get \
        -H "Content-Type: application/json" \
        -d '{"partition": "test", "key": "hello"}' | jq .
    
    kill ${PORT_FORWARD_PID} 2>/dev/null || true
    
    log_success "Basic functionality test passed"
}

# Cleanup deployment
cleanup() {
    log_warn "Cleaning up deployment..."
    
    kubectl delete -f statefulset.yaml --ignore-not-found=true
    kubectl delete -f services.yaml --ignore-not-found=true
    kubectl delete -f configmap.yaml --ignore-not-found=true
    kubectl delete -f storage.yaml --ignore-not-found=true
    kubectl delete -f rbac.yaml --ignore-not-found=true
    kubectl delete -f namespace.yaml --ignore-not-found=true
    
    log_success "Cleanup completed"
}

# Main deployment function
deploy() {
    log_info "Starting RustRocks Service deployment..."
    log_info "Configuration: Namespace=${NAMESPACE}, Replicas=${REPLICAS}, StorageClass=${STORAGE_CLASS}"
    
    check_kubectl
    
    if [[ "${BUILD_IMAGE:-true}" == "true" ]]; then
        build_image
    fi
    
    deploy_namespace
    deploy_storage
    deploy_config
    deploy_services
    deploy_statefulset
    
    wait_for_ready
    show_status
    
    if [[ "${RUN_TESTS:-true}" == "true" ]]; then
        test_deployment
    fi
    
    log_success "RustRocks Service deployed successfully!"
    
    echo ""
    echo "Next steps:"
    echo "1. Monitor the deployment: kubectl get pods -n ${NAMESPACE} -w"
    echo "2. Check logs: kubectl logs -f statefulset/rustrocks-service -n ${NAMESPACE}"
    echo "3. Test the service: kubectl port-forward svc/rustrocks-service 8080:8080 -n ${NAMESPACE}"
}

# Parse command line arguments
case "${1:-deploy}" in
    "deploy")
        deploy
        ;;
    "cleanup")
        cleanup
        ;;
    "status")
        show_status
        ;;
    "test")
        test_deployment
        ;;
    *)
        echo "Usage: $0 [deploy|cleanup|status|test]"
        echo ""
        echo "Environment variables:"
        echo "  IMAGE_TAG: Docker image tag (default: latest)"
        echo "  DOCKER_REGISTRY: Docker registry URL"
        echo "  STORAGE_CLASS: Kubernetes storage class (default: rustrocks-standard-ssd)"
        echo "  REPLICAS: Number of replicas (default: 3)"
        echo "  BUILD_IMAGE: Build Docker image (default: true)"
        echo "  RUN_TESTS: Run deployment tests (default: true)"
        exit 1
        ;;
esac