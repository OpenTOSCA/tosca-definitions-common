#!/bin/sh
apt-get update -qq
apt-get install mariadb-server -qq
service mariadb restart