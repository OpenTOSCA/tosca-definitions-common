#!/bin/bash

apt-get update -y

apt-get install -y curl

apt-get install -y cron

mkdir ~/monitoring

cat > ~/monitoring/monitoring.sh<< EOF
# Get CPU usage as a percentage
cpu_usage=\$(top -bn 1 | grep "Cpu(s)" | awk '{print \$2 + \$4}')

# Get memory usage as a percentage
mem_usage=\$(free | grep Mem | awk '{print (\$3 / \$2) * 100}')

# Create a JSON payload
payload="{\"cpu_usage\": \$cpu_usage, \"mem_usage\": \$mem_usage}"

# Send data to the endpoint using cURL
echo \$payload
curl -X POST -H "Content-Type: application/json" -d "\$payload" "$QProvEndpoint"
EOF

chmod +x ~/monitoring/monitoring.sh

sudo crontab -l | { cat; echo "* * * * * ~/monitoring/monitoring.sh >> ~/monitoring/crontab_log.txt"; } | sudo crontab -

service cron start

echo "Successfulls set up Monitoring Agent."