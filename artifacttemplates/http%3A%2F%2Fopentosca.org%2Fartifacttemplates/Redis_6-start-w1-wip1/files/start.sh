#!/bin/sh
redis-server --port $RedisPort --protected-mode no
echo "Started Redis on Port {$RedisPort}"

sleep 5