#!/bin/bash
IFS=';' read -ra NAMES <<<"$DAs"
for i in "${NAMES[@]}"; do
  echo "KeyValue-Pair: "
  echo $i
  IFS=',' read -ra entry <<<"$i"
  echo "Key: "
  echo ${entry[0]}
  echo "Value: "
  echo ${entry[1]}

  # find the .properties file
  if [[ "${entry[1]}" == *.properties ]]; then
    # copy the config file to
    sudo cp $CSAR${entry[1]} /var/www/java/${AppName}/application.properties
    sudo echo "server.port=${Port}" >>/var/www/java/${AppName}/application.properties
  # else if a application .yml is provided, copy it
  elif [[ "${entry[1]}" == *.yml ]]; then
    sudo cp $CSAR${entry[1]} /var/www/java/${AppName}/application.yml
    sudo echo "server"
    sudo echo "  port=${Port}" >>/var/www/java/${AppName}/application.yml
  fi
done

if [[ ! -f /var/www/java/${AppName}/application.properties || ! -f /var/www/java/${AppName}/application.yml ]]; then
  sudo echo "server.port=${Port}" >>/var/www/java/${AppName}/application.properties
fi

sudo iptables -A INPUT -p tcp -m tcp --dport $Port -j ACCEPT
