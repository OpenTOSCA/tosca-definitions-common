#!/bin/bash
cd /var/www/java/${AppName}

nohup java -jar ${AppName}.jar --server.port=$Port > /var/log/${AppName}.log 2>&1 &

# Inhibit some race condition
sleep 5

echo "Started Spring Web Application $AppName"
