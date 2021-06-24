#!/bin/bash

function check () {
  echo "Check if "$1" is available"
  if ! [[ -x "$(command -v "$1")" ]]; then
    echo "INFO: "$1" is not installed" >&2
    return 1
  fi
}

check systemctl
if [[ $? -eq 0 ]]; then
    /bin/cat <<EOM >/etc/systemd/system/tomcat.service
[Unit]
Description=Tomcat 9 servlet container
After=network.target

[Service]
Type=forking

User=tomcat
Group=tomcat

Environment="JAVA_HOME=`dirname $(dirname $(readlink -f $(which java)))`"
Environment="JAVA_OPTS=-Djava.security.egd=file:///dev/urandom -Djava.awt.headless=true"

Environment="CATALINA_BASE=/opt/tomcat"
Environment="CATALINA_HOME=/opt/tomcat"
Environment="CATALINA_PID=/opt/tomcat/temp/tomcat.pid"
Environment="CATALINA_OPTS=-Xms512M -Xmx1024M -server -XX:+UseParallelGC"

ExecStart=/opt/tomcat/bin/startup.sh
ExecStop=/opt/tomcat/bin/shutdown.sh

[Install]
WantedBy=multi-user.target
EOM
    systemctl daemon-reload
    systemctl enable tomcat
    systemctl start tomcat
else
    echo "INFO: Starting Tomcat in foreground"
    export CATALINA_BASE="/opt/tomcat"
    export CATALINA_HOME="/opt/tomcat"
    export CATALINA_OPTS="-Xms512M -Xmx1024M -server -XX:+UseParallelGC"
    ${CATALINA_HOME}/bin/catalina.sh run
fi

sleep 10
exit 0
