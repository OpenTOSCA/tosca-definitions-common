#!/bin/sh
rootpassword=$DBMSPassword

echo "Received, $DBMSPassword as password for user 'root' and $DBMSPort as port";

mySqlConfig='/etc/mysql/my.cnf'

# remove bind so that it can be accessed later
sudo sed -i "\$a \ \n[mysqld]\n    bind-address = 0.0.0.0" $mySqlConfig

#set root user and pw (we assume here that the install.sh was executed before and the root user is still available, in a management plan this script might fail hard)
sudo mysql -uroot -e "use mysql; UPDATE user SET authentication_string=PASSWORD('$rootpassword') WHERE User='root'; GRANT ALL PRIVILEGES ON *.* to 'root'; FLUSH PRIVILEGES;"

#set port
sudo sed -i -e "/port	/c\port=$DBMSPort" $mySqlConfig;

#configure iptables
sudo iptables -A INPUT -p tcp -m tcp --dport $DBMSPort -j ACCEPT

sudo service mysql restart;
