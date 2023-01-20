#!/bin/bash

HOMEDIR=$HOME/javaapp

cd $HOMEDIR
kill -9 $(cat $HOMEDIR/subscriberProxy.pid)

cat << EOF >> driver-manager.yml
  proxyFor:
    protocol: $ChannelType
    location: $IP
    port: $Port
EOF

nohup java  -Dloader.path="$HOMEDIR/jardriver" -jar $HOMEDIR/subscriber-1.0.jar "$HOMEDIR/driver-manager.yml" > log.log 2>&1 &
echo $! > subscriberProxy.pid

sleep 5
