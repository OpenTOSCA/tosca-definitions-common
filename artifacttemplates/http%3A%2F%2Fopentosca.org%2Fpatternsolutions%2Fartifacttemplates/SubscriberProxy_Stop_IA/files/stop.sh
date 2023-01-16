#!/bin/bash
HOMEDIR=$HOME/javaapp
kill -9 $(ps aux | grep '[j]ava .*driver-manager.yml$' | awk '{print $2}')
sleep 5