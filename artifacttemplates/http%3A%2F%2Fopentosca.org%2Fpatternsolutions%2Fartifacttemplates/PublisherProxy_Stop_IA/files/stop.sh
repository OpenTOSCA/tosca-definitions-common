#!/bin/bash
HOMEDIR=$HOME/pythonapp
kill -9 $(ps aux | grep '[p]ython3 .*driver-manager.yml$' | awk '{print $2}')
sleep 5