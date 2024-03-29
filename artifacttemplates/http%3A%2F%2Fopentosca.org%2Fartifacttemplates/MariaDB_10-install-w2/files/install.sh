#!/bin/sh
databaseuser=$DBUser
databasepw=$DBPassword
databasename=$DBName
dbmsroot=$DBMSUser
dbmspw=$DBMSPassword

#create database
mariadb -h localhost -u$dbmsroot -p$dbmspw -e "use mysql; create database $databasename;"

#create user and set access rights
mariadb -h localhost -u$dbmsroot -p$dbmspw -e "use mysql; create user '$databaseuser'@'%' identified by '$databasepw'; grant all privileges on $databasename.* to '$databaseuser'@'%'; flush privileges;"
