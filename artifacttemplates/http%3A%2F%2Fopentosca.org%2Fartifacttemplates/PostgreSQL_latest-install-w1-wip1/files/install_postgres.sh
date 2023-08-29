#!/bin/bash

sudo DEBIAN_FRONTEND="noninteractive" TZ="UTC" apt-get update -qq

sudo DEBIAN_FRONTEND="noninteractive" TZ="UTC" apt-get install -y postgresql

echo "Port: "$DBPort

echo "Postgres User Password: "$DBRootPassword

postgresconf=$(find ~/../etc/postgresql -path "*/postgresql.conf");
hbaconf=$(find ~/../etc/postgresql -path "*/pg_hba.conf");
confline=$(grep -n 'all.*postgres.*' $hbaconf | cut -f1 -d:);
ipline=$(grep -n 'host.*all.*all.*127.*md5' $hbaconf | cut -f1 -d:);

echo "Config Path: "$postgresconf

echo "port = $DBPort" >> $postgresconf

echo "listen_addresses = '*'" >> $postgresconf

sudo sed -i "${confline}clocal   all             postgres                                trust" $hbaconf

sudo service postgresql start

psql -p $DBPort -U postgres -c "ALTER USER postgres with password '$DBRootPassword';"

sudo sed -i "${confline}clocal   all             postgres                                md5" $hbaconf

sudo sed -i "${ipline}chost    all             all             all                     md5" $hbaconf

sudo service postgresql stop

echo "Successfull installed postgresql"