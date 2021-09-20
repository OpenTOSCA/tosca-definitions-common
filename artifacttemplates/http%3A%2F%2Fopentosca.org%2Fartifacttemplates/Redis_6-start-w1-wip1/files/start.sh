#!/bin/sh
redis-server --port $RedisPort
echo "Started Redis on Port {$RedisPort}"

sleep 5