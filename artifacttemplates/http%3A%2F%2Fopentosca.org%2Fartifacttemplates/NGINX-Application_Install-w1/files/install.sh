#!/bin/bash
sudo apt update -qq
sudo apt -qqy install unzip;

IFS=';' read -ra NAMES <<< "$DAs";
for i in "${NAMES[@]}"; do
    IFS=',' read -ra entry <<< "$i";
  # find the zip file
    if [[ "${entry[1]}" == *.zip ]];
    then
        # unzip the application to
        sudo mkdir -p /var/www/html/${AppName}
        sudo unzip $CSAR${entry[1]} -d /var/www/html/${AppName}
    fi
done

echo "installed website at /var/www/html/${AppName}"
