#! /bin/bash
sudo sh -c "echo '127.0.0.1' $(hostname) >> /etc/hosts"
sudo groupadd tomcat
sudo useradd -s /bin/false -g tomcat -d /opt/tomcat tomcat
cd ~
wget https://archive.apache.org/dist/tomcat/tomcat-8/v8.5.28/bin/apache-tomcat-8.5.28.tar.gz
sudo mkdir -p /opt/tomcat
sudo tar xvf apache-tomcat-8*tar.gz -C /opt/tomcat --strip-components=1
cd /opt/tomcat
# DEBUG
sudo mkdir -p /opt/tomcat/conf/Catalina/localhost
sudo chmod -R 777 /opt/tomcat
sudo echo "<?xml version='1.0' encoding='UTF-8'?><Context privileged='true' antiResourceLocking='false' docBase='\${catalina.home}/webapps/manager'><Valve className='org.apache.catalina.valves.RemoteAddrValve' allow='^.*$' /></Context>" > /opt/tomcat/conf/Catalina/localhost/manager.xml
sudo echo "<?xml version='1.0' encoding='UTF-8'?><Context privileged='true' antiResourceLocking='false' docBase='\${catalina.home}/webapps/host-manager'><Valve className='org.apache.catalina.valves.RemoteAddrValve' allow='^.*$' /></Context>" > /opt/tomcat/conf/Catalina/localhost/host-manager.xml
sudo echo "<tomcat-users><role rolename='admin-gui'/><role rolename='manager-gui'/><role rolename='manager-script'/><user username='admin' password='admin' roles='admin-gui,manager-gui,manager-script'/></tomcat-users>" > /opt/tomcat/conf/tomcat-users.xml
sudo chmod -R 777 /opt/tomcat
# PROD
# sudo chgrp -R tomcat conf/
# sudo chmod g+rwx conf/
# sudo chmod g+r conf/*
# sudo chown -R tomcat work/ temp/ logs/
# sudo rm -rf /opt/tomcat/webapps/*
sleep 5
