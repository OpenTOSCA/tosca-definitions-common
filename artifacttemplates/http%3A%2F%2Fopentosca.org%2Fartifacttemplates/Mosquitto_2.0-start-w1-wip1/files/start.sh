#!/bin/sh

echo "Starting mosquitto service..."

service mosquitto stop

sleep 5

service mosquitto start

sleep 5

cat /var/log/mosquitto/mosquitto.log
