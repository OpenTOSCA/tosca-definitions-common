#!/usr/bin/env bash

sudo apt-get update -qq

echo "Installing NGINX..."
sudo DEBIAN_FRONTEND="noninteractive" TZ="UTC" apt-get install -qqy nginx

echo "Successfully installed NGINX!"
