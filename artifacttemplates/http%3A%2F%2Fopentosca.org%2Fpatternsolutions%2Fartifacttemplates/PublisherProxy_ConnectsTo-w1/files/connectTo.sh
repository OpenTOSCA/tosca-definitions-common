#!/bin/bash

HOMEDIR=$HOME/pythonapp
cd $HOMEDIR
kill -9 $(cat $HOMEDIR/publisherProxy.pid)

cat << EOF >> $HOMEDIR/driver-manager.yml
  name: $TopicName
  driver: $Driver
  connection: $IP:$TARGET_Port
EOF

cd $HOMEDIR/publisherProxy
nohup python3 main.py $SOURCE_Port "$HOMEDIR/driver-manager.yml" > log.log 2>&1 &
echo $! > publisherProxy.pid

sleep 5
