#!/bin/bash

sudo apt-get update -qq

echo "installing openjdk 11"
sudo DEBIAN_FRONTEND="noninteractive" TZ="UTC" apt-get -qy install openjdk-11-jdk

echo "done."
sleep 5
