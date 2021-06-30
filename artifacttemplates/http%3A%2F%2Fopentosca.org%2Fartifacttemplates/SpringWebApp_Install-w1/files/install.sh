#!/bin/bash

IFS=';' read -ra NAMES <<< "$DAs";
for i in "${NAMES[@]}"; do
  echo "KeyValue-Pair: "
  echo $i
  IFS=',' read -ra entry <<< "$i";
    echo "Key: "
    echo ${entry[0]}
    echo "Value: "
    echo ${entry[1]}

  # find the executable jar file
  if [[ "${entry[1]}" == *.jar ]];
  then
    # copy the executable to /var/www/
    sudo mkdir -p /var/www/java/${AppName}
    sudo cp $CSAR${entry[1]} /var/www/java/${AppName}/${AppName}.jar
  # find the application.properties file
  elif [[ "${entry[1]}" == *.properties ]];
  then
    # copy the config file also to /var/www/
    sudo mkdir -p /var/www/java/${AppName}
    sudo cp $CSAR${entry[1]} /var/www/java/${AppName}/application.properties
  fi
done
