#! /bin/bash

echo $Port

if [ ! -z "$Port" ]
then
  sudo sed -i "s/port=\"8080\"/port=\"$Port\"/g" /opt/tomcat/conf/server.xml
  echo "Started tomcat on port $Port"
else
  echo "Using default port 8080"
fi

sudo systemctl restart tomcat
