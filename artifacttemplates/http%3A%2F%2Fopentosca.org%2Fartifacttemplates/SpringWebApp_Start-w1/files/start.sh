#!/bin/bash
cd /var/www/java/${AppName}

nohup java -jar ${AppName}.jar --server.port=$Port > /var/log/${AppName}.log 2>&1 &
echo $! > ${AppName}.pid

ln -s /var/log/${AppName}.log

# Inhibit some race condition
sleep 5

echo "Started ${AppName} with process ID: $(cat ${AppName}.pid)"
