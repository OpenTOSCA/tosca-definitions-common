#!/bin/sh
echo "Starting Redis..."

redis-server --port $RedisPort --protected-mode no --daemonize yes

echo "Started Redis on Port {$RedisPort}"

sleep 2