#!/bin/sh
echo "installing ballerina"

apt-get update -qq
apt-get install wget -qq
apt-get install unzip -qq
wget https://dist.ballerina.io/downloads/swan-lake-beta2/ballerina-swan-lake-beta2.zip
unzip ballerina-swan-lake-beta2.zip
mkdir app
mv ./ballerina-swan-lake-beta2 ./app
export PATH="${PATH}:/app/ballerina-swan-lake-beta2/bin"

echo "done"