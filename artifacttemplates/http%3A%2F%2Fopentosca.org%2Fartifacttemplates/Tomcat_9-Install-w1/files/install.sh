#! /bin/bash

sudo -E bash -c "echo '127.0.0.1' $(cat /etc/hostname) >> /etc/hosts"
sudo groupadd tomcat > ~/tomcat_install.log
sudo useradd -s /bin/false -g tomcat -d /opt/tomcat tomcat >> ~/tomcat_install.log
wget http://ftp-stud.hs-esslingen.de/pub/Mirrors/ftp.apache.org/dist/tomcat/tomcat-9/v9.0.0.M22/bin/apache-tomcat-9.0.0.M22.tar.gz >> ~/tomcat_install.log
sudo mkdir /opt/tomcat >> ~/tomcat_install.log
sudo tar xf apache-tomcat-9*.tar.gz -C /opt/tomcat --strip-components=1 >> ~/tomcat_install.log

cd /opt
sudo chmod -R 777 tomcat >> ~/tomcat_install.log
cd tomcat
sudo chgrp -R tomcat conf >> ~/tomcat_install.log
sudo chmod g+r conf/* >> ~/tomcat_install.log
sudo chown -R tomcat work/ temp/ logs/ >> ~/tomcat_install.log
sudo chown -R tomcat:tomcat /opt/tomcat/
