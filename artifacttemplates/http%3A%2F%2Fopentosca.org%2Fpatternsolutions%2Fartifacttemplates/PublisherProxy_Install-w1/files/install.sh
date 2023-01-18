#!/bin/bash

# Install dependencies
sudo apt-get update -qq
sudo apt-get install -qqy unzip
sudo pip3 install pyyaml

CSARROOT=$(find / -path "*.csar");
echo $CSARROOT
HOMEDIR=$HOME/pythonapp

mkdir -p $HOMEDIR

# Extract DAs
IFS=';' read -ra FILES <<< "$DAs"
for i in "${FILES[@]}"; do
  IFS=',' read -ra ENTRY <<< "$i"
  if [[ ( -f $CSARROOT${ENTRY[1]} ) && ( ${ENTRY[1]} == *.zip ) ]]; then
    ARCHIVE=$CSARROOT${ENTRY[1]}
    echo $ARCHIVE
    sudo unzip -o $ARCHIVE -d $HOMEDIR
  fi
done

# Install supplied drivers
cd $HOMEDIR
for DIRNAME in */; do
  if [[ $DIRNAME == driver* ]]; then
    sudo pip3 install $DIRNAME
  fi
done

# Install publisher
cd $HOMEDIR/publisherProxy
sudo pip3 install -r requirements.txt

echo "requestReplyTopic:" > $HOMEDIR/driver-manager.yml

sleep 5
