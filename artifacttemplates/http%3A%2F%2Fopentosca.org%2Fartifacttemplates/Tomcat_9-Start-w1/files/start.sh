#! /bin/bash

echo "starting tomcat..."
cd /opt/tomcat
nohup ./bin/startup.sh &

sleep 10
