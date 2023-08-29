#!/bin/bash

# https://askubuntu.com/questions/909277/avoiding-user-interaction-with-tzdata-when-installing-certbot-in-a-docker-contai
export TZ=Europe/Berlin
ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

sudo apt-get update -qq

echo "installing openjdk 17"
sudo DEBIAN_FRONTEND="noninteractive" TZ="UTC" apt-get -qy install openjdk-17-jdk

sleep 5

java --version

echo "done installing openjdk 17."
sleep 5
