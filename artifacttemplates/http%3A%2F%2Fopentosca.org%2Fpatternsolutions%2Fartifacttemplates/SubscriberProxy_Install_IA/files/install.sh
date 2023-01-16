#!/bin/bash

# Install dependencies
sudo apt-get update -qq
sudo apt-get install -qqy unzip

CSARROOT=$(find / -path "*.csar");
HOMEDIR=$HOME/javaapp

mkdir -p $HOMEDIR/jardriver

IFS=';' read -ra FILES <<< "$DAs"
for i in "${FILES[@]}"; do
  IFS=',' read -ra ENTRY <<< "$i"
  if [[ ( -f $CSARROOT${ENTRY[1]} ) && ( ${ENTRY[1]} == *.zip ) ]]; then
    ARCHIVE=$CSARROOT${ENTRY[1]}
    DIRNAME=$(basename "$ARCHIVE" ".zip")
    if [[ $DIRNAME == driver* ]]; then
      sudo unzip -o $ARCHIVE -d $HOMEDIR/jardriver
    else
      sudo unzip -o $ARCHIVE -d $HOMEDIR
    fi
  fi
done
sleep 5