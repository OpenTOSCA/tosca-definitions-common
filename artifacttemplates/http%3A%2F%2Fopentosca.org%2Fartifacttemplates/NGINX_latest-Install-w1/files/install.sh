#!/usr/bin/env bash

# https://askubuntu.com/questions/909277/avoiding-user-interaction-with-tzdata-when-installing-certbot-in-a-docker-contai
export TZ=Europe/Berlin
ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

sudo apt update

echo "Installing NGINX..."
sudo apt install nginx -qqy

echo "Successfully installed NGINX!"
