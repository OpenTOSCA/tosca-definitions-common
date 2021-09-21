#!/bin/bash

# https://askubuntu.com/questions/909277/avoiding-user-interaction-with-tzdata-when-installing-certbot-in-a-docker-contai
export TZ=Europe/Berlin
ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

sudo apt update

echo "installing openjdk 11"
sudo apt -qy install openjdk-11-jdk

echo "done."
sleep 5
