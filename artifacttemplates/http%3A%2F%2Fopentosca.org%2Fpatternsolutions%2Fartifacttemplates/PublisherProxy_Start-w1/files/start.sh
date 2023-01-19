#!/bin/bash

HOMEDIR=$HOME/pythonapp
cd $HOMEDIR/publisherProxy

nohup python3 main.py $Port "$HOMEDIR/driver-manager.yml" > log.log 2>&1 &
echo $! > publisherProxy.pid

sleep 5
