#!/bin/bash

HOMEDIR=$HOME/pythonapp
cd $HOMEDIR
kill -9 $(cat $HOMEDIR/publisherProxy.pid)

echo "  name: $Name
  driver: $Driver
  connection: $VMIP:$DefaultPort" >> $HOMEDIR/driver-manager.yml

cd $HOMEDIR/publisherProxy
nohup python3 main.py 9993 "$HOMEDIR/driver-manager.yml" > log.log 2>&1 &
echo $! > publisherProxy.pid

sleep 5