#!/bin/sh
sudo apt-get update -qq;
sudo apt-get DEBIAN_FRONTEND=noninteractive install -y python3 -qq;
sudo apt-get install -y python3-pip -qq;
