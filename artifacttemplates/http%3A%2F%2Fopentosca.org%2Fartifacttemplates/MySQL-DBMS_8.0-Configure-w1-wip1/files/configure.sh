#!/bin/sh

# complete this installation using the configuration script according to https://www.digitalocean.com/community/tutorials/how-to-install-mysql-on-ubuntu-22-04
cat << EOF >> /etc/mysql/my.cnf
[mysqld]
bind-address            = 0.0.0.0
port                    = $DBMSPort
EOF

sudo /etc/init.d/mysql restart

sleep 5
