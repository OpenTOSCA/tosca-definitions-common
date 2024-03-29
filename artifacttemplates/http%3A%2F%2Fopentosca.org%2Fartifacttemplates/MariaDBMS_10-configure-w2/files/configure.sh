#!/bin/sh
rootpassword=$DBMSPassword
rootuser=$DBMSUser

echo "Received, $DBMSPassword as password for user 'root' and $DBMSPort as port"

mySqlConfig='/etc/mysql/my.cnf'

# remove bind so that it can be accessed later
sudo sed -i "\$a \ \n[mysqld]\n    bind-address = 0.0.0.0" $mySqlConfig

#set port
echo "    port = $DBMSPort" >> $mySqlConfig;

mariadb --user=root <<_EOF_
UPDATE mysql.user SET Password=PASSWORD('$rootpassword') WHERE User='root';
DELETE FROM mysql.user WHERE User='';
DELETE FROM mysql.user WHERE User='root' AND Host NOT IN ('localhost', '127.0.0.1', '::1');
DROP DATABASE IF EXISTS test;
DELETE FROM mysql.db WHERE Db='test' OR Db='test\\_%';
FLUSH PRIVILEGES;
_EOF_

mariadb --user=root <<_EOF_
CREATE USER '$rootuser'@'%' IDENTIFIED BY '$rootpassword';
GRANT ALL PRIVILEGES ON *.* TO '$rootuser'@'%' IDENTIFIED BY '$rootpassword' WITH GRANT OPTION;
FLUSH PRIVILEGES;
_EOF_

#configure iptables
sudo iptables -A INPUT -p tcp -m tcp --dport $DBMSPort -j ACCEPT