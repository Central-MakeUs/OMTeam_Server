#!/bin/bash
# OMT Server Deployment Script
# Usage: ./deploy.sh [dev|prod]

set -e

ENVIRONMENT=${1:-prod}
APP_DIR="/home/$(whoami)/omt"

echo "=========================================="
echo "  Deploying OMT Server ($ENVIRONMENT)"
echo "=========================================="

cd $APP_DIR

# Check required files
if [ ! -f "docker-compose.yml" ]; then
    echo "Error: docker-compose.yml not found"
    exit 1
fi

if [ ! -f ".env" ]; then
    echo "Error: .env file not found"
    exit 1
fi

# Load environment variables
source .env

# Login to ECR
echo "[1/5] Logging in to ECR..."
aws ecr get-login-password --region ap-northeast-2 | docker login --username AWS --password-stdin $ECR_REGISTRY

# Pull latest images
echo "[2/5] Pulling latest images..."
if [ "$ENVIRONMENT" == "prod" ]; then
    docker compose -f docker-compose.yml -f docker-compose.prod.yml pull
else
    docker compose pull
fi

# Stop old containers gracefully
echo "[3/5] Stopping old containers..."
if [ "$ENVIRONMENT" == "prod" ]; then
    docker compose -f docker-compose.yml -f docker-compose.prod.yml down --timeout 30
else
    docker compose --profile dev down --timeout 30
fi

# Start new containers
echo "[4/5] Starting new containers..."
if [ "$ENVIRONMENT" == "prod" ]; then
    docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d
else
    docker compose --profile dev up -d
fi

# Cleanup
echo "[5/5] Cleaning up old images..."
docker image prune -f

# Health check
echo ""
echo "Waiting for services to start..."
sleep 30

echo ""
echo "Checking service health..."

# Check App Server
if curl -sf http://localhost:8080/actuator/health > /dev/null; then
    echo "✅ App Server: healthy"
else
    echo "❌ App Server: unhealthy"
    docker logs omt-app --tail 50
fi

# Check AI Server
if curl -sf http://localhost:8000/health > /dev/null; then
    echo "✅ AI Server: healthy"
else
    echo "❌ AI Server: unhealthy"
    docker logs omt-ai-server --tail 50
fi

# Check MySQL (dev only)
if [ "$ENVIRONMENT" == "dev" ]; then
    if docker exec omt-mysql mysqladmin ping -h localhost -u root -p$MYSQL_ROOT_PASSWORD > /dev/null 2>&1; then
        echo "✅ MySQL: healthy"
    else
        echo "❌ MySQL: unhealthy"
    fi
fi

echo ""
echo "=========================================="
echo "  Deployment Complete!"
echo "=========================================="
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
