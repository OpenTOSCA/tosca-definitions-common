#!/bin/bash

echo "Simply restarting tomcat..."
systemctl restart tomcat
echo "Restarted tomcat..."

sleep 2
exit 0
