#!/bin/bash

apt-get update -y

apt-get install -y curl

apt-get install -y cron

name=$(hostname)

cpu_name=$(lscpu | grep 'Model name' | cut -f 2 -d ":" | awk '{$1=$1}1')

cpu_cores=$(grep -c processor /proc/cpuinfo)

ram=$(free -m | awk '/Mem:/ { print $2 }')

disk_size=$(df | awk '$NF=="/"{printf "%d", $2}')

payload="{\"name\": $name, \"cpu_name\": $cpu_name, \"cpu_cores\": $cpu_cores, \"ram\": $ram, \"disk_size\": $disk_size}"

curl -X POST -H "Content-Type: application/json" -d "$payload" "$QProvEndpoint"

mkdir ~/monitoring

cat > ~/monitoring/monitoring.sh<< EOF
# Get CPU usage as a percentage
cpu_usage=\$[100-\$(vmstat 1 2|tail -1|awk '{print \$15}')]

cpu_speed=\$(cat /proc/cpuinfo | grep 'cpu MHz' | cut -f 2 -d ":" | awk '{\$1=\$1}1' |head -1)

# Get memory usage as a percentage
mem_usage=\$(free | grep Mem | awk '{print (\$3 / \$2) * 100}')

disk_usage=\$(df | awk '\$NF=="/"{printf "%d", \$5}')


# Create a JSON payload
payload="{\"name\": $name, \"cpu_usage\": \$cpu_usage, \"cpu_speed\": \$cpu_speed, \"mem_usage\": \$mem_usage, \"disk_usage\": \$disk_usage}, \"timestamp\": \$(date +%s)}

# Send data to the endpoint using cURL
echo \$payload
curl -X POST -H "Content-Type: application/json" -d "\$payload" "$QProvEndpoint"
EOF

chmod +x ~/monitoring/monitoring.sh

sudo crontab -l | { cat; echo "* * * * * ~/monitoring/monitoring.sh >> ~/monitoring/crontab_log.txt"; } | sudo crontab -

service cron start

echo "Successfulls set up Monitoring Agent."