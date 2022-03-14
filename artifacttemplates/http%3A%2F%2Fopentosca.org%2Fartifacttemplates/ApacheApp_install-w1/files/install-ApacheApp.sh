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

  # find the .zip file
  if [[ "${entry[1]}" == *.zip ]];
  then
	sudo unzip -o $CSAR${entry[1]} -d $Rootpath
  fi
done

echo "URL="$IP":"$Port"/"
exit 0