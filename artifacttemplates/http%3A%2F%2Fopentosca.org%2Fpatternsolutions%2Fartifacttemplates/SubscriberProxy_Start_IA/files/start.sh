#!/bin/bash
HOMEDIR=$HOME/javaapp
cd $HOMEDIR
nohup java  -Dloader.path="$HOMEDIR/jardriver" -jar $HOMEDIR/subscriber-1.0.jar "$HOMEDIR/driver-manager.yml" > log.log 2>&1 &
echo $! > subscriberProxy.pid
sleep 5