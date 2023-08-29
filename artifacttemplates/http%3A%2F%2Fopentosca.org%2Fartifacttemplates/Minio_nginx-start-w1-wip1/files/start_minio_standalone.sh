#!/bin/bash

sudo chown -R $USER:$USER /data1

sudo chown -R $USER:$USER /data2

echo "Port:"$MinIOPort
echo "Console Port:"$ConsolePort

export CI=true

# Start MinIO in the background (replace ACCESS_KEY and SECRET_KEY with your own values)
nohup minio server --address :$MinIOPort --console-address :$ConsolePort /data{1...2} &

echo "MinIO  successfully started!"