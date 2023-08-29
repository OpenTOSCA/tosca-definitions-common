#!/bin/bash

# This script sets up Minio

# Update package list

export TZ=Europe/Berlin
ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

sudo DEBIAN_FRONTEND="noninteractive" TZ="UTC" apt-get update -qq
apt-get install -y wget

# Install MinIO
wget https://dl.min.io/server/minio/release/linux-amd64/minio
chmod +x minio
sudo mv minio /usr/local/bin/

# Create data directory for MinIO
sudo mkdir /data1
sudo mkdir /data2

echo "MinIO installed successful!"
