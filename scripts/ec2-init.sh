#!/bin/bash
# EC2 Initial Setup Script for OMT Server
# Run this script once when setting up a new EC2 instance
# Usage: sudo bash ec2-init.sh

set -e

echo "=========================================="
echo "  OMT Server EC2 Initial Setup"
echo "=========================================="

# Update system
echo "[1/6] Updating system packages..."
sudo yum update -y || sudo apt-get update -y

# Install Docker
echo "[2/6] Installing Docker..."
if ! command -v docker &> /dev/null; then
    # Amazon Linux 2023 / Amazon Linux 2
    if command -v amazon-linux-extras &> /dev/null; then
        sudo amazon-linux-extras install docker -y
    else
        sudo yum install -y docker || sudo apt-get install -y docker.io
    fi
    sudo systemctl start docker
    sudo systemctl enable docker
    sudo usermod -aG docker $USER
    echo "Docker installed successfully"
else
    echo "Docker already installed"
fi

# Install Docker Compose
echo "[3/6] Installing Docker Compose..."
if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
    DOCKER_COMPOSE_VERSION="v2.24.0"
    sudo curl -L "https://github.com/docker/compose/releases/download/${DOCKER_COMPOSE_VERSION}/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
    sudo chmod +x /usr/local/bin/docker-compose

    # Also install as Docker plugin
    mkdir -p ~/.docker/cli-plugins/
    curl -SL "https://github.com/docker/compose/releases/download/${DOCKER_COMPOSE_VERSION}/docker-compose-$(uname -s)-$(uname -m)" -o ~/.docker/cli-plugins/docker-compose
    chmod +x ~/.docker/cli-plugins/docker-compose
    echo "Docker Compose installed successfully"
else
    echo "Docker Compose already installed"
fi

# Install AWS CLI
echo "[4/6] Installing AWS CLI..."
if ! command -v aws &> /dev/null; then
    curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
    unzip awscliv2.zip
    sudo ./aws/install
    rm -rf aws awscliv2.zip
    echo "AWS CLI installed successfully"
else
    echo "AWS CLI already installed"
fi

# Create application directory
echo "[5/6] Creating application directory..."
APP_DIR="/home/$(whoami)/omt"
mkdir -p $APP_DIR
cd $APP_DIR

# Create necessary subdirectories
mkdir -p logs data

echo "[6/6] Setting up log rotation..."
sudo tee /etc/logrotate.d/omt > /dev/null << 'EOF'
/home/*/omt/logs/*.log {
    daily
    missingok
    rotate 14
    compress
    delaycompress
    notifempty
    create 0640 root root
    sharedscripts
}
EOF

echo ""
echo "=========================================="
echo "  Setup Complete!"
echo "=========================================="
echo ""
echo "Next steps:"
echo "1. Configure AWS credentials: aws configure"
echo "2. Copy docker-compose files to $APP_DIR"
echo "3. Create .env file with required secrets"
echo "4. Run: docker compose up -d"
echo ""
echo "NOTE: Log out and log back in for docker group to take effect"
