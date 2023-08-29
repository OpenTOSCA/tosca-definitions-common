#!/bin/bash

PGPASSWORD=$DBRootPassword psql -p $DBPort -U postgres -c "CREATE USER $DBUser WITH PASSWORD '$DBPassword';"
PGPASSWORD=$DBRootPassword psql -p $DBPort -U postgres -c "CREATE DATABASE $DBName OWNER $DBUser;"

echo "Successfully configured new postgresdb"