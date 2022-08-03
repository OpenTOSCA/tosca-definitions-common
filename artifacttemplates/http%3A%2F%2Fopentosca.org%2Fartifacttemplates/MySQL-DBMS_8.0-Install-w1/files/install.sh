#!/bin/sh
sudo sh -c "echo '127.0.0.1' $(hostname) >> /etc/hosts";
sudo apt-get update -qq;

# disables setting the root password with gui, root password etc. will be set in the configure.sh
export DEBIAN_FRONTEND=noninteractive;

sudo apt-get install -y mysql-server-8.0

sleep 5

# verifying the installation
sudo /etc/init.d/mysql start

sleep 5
