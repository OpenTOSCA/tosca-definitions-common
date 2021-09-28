#!/bin/sh
apt-get update -qq
apt-get install wget -qq
apt-get install build-essential -qq
wget https://download.redis.io/releases/redis-6.2.5.tar.gz
tar xzf redis-6.2.5.tar.gz
cd redis-6.2.5
echo "Installing Redis"
make install > instal.log
echo "Finished"