#!/bin/sh
sudo apt-get update;
sudo apt-get -y install tomcat7 tomcat7-admin;
# for apps writing into home
sudo chown -R tomcat7:tomcat7 /usr/share/tomcat7;
