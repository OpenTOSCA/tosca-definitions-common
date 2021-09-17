#!/bin/sh
rootpassword=$DBMSPassword

echo "Received, $DBMSPassword as password for user 'root' and $DBMSPort as port"

mySqlConfig='/etc/mysql/my.cnf'

# remove bind so that it can be accessed later
sudo sed -i "\$a \ \n[mysqld]\n    bind-address = 0.0.0.0" $mySqlConfig

mysql --user=root <<_EOF_
UPDATE mysql.user SET Password=PASSWORD('$rootpassword') WHERE User='root';
DELETE FROM mysql.user WHERE User='';
DELETE FROM mysql.user WHERE User='root' AND Host NOT IN ('localhost', '127.0.0.1', '::1');
DROP DATABASE IF EXISTS test;
DELETE FROM mysql.db WHERE Db='test' OR Db='test\\_%';
FLUSH PRIVILEGES;
_EOF_

#set port
sudo sed -i -e "/port	/c\port=$DBMSPort" $mySqlConfig;

#configure iptables
sudo iptables -A INPUT -p tcp -m tcp --dport $DBMSPort -j ACCEPT