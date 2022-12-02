#! /bin/bash

sudo -E bash -c "echo '127.0.0.1' $(cat /etc/hostname) >> /etc/hosts"
sudo groupadd tomcat > ~/tomcat_install.log
sudo useradd -s /bin/false -g tomcat -d /opt/tomcat tomcat >> ~/tomcat_install.log
wget https://dlcdn.apache.org/tomcat/tomcat-9/v9.0.64/bin/apache-tomcat-9.0.64.tar.gz
sudo mkdir /opt/tomcat >> ~/tomcat_install.log
sudo tar xf apache-tomcat-9*.tar.gz -C /opt/tomcat --strip-components=1 >> ~/tomcat_install.log

cd /opt
sudo chmod -R 777 tomcat >> ~/tomcat_install.log
cd tomcat
sudo chgrp -R tomcat conf >> ~/tomcat_install.log
sudo chmod g+r conf/* >> ~/tomcat_install.log
sudo chown -R tomcat work/ temp/ logs/ >> ~/tomcat_install.log
sudo chown -R tomcat:tomcat /opt/tomcat/

sudo echo "CATALINA_HOME=/opt/tomcat" >> /etc/environment
sudo echo "CATALINA_BASE=/opt/tomcat" >> /etc/environment
