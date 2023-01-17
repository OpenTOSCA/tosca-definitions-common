#!/bin/bash
HOMEDIR=$HOME/pythonapp
cd $HOMEDIR/publisherProxy
nohup python3 main.py 9993 "$HOMEDIR/driver-manager.yml" > log.log 2>&1 &
echo $! > publisherProxy.pid
sleep 5