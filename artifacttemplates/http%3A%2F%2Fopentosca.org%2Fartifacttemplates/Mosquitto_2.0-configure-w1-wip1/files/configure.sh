#!/bin/sh

port=${Port:-1883}

echo "Config using port: $port"

service mosquitto stop

cat << EOF >> /etc/mosquitto/conf.d/default.conf
listener         $port
allow_anonymous  true
EOF

