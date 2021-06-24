#!/bin/bash

if [ -z "$CATALINA_HOME" ]; then
  $CATALINA_HOME=/opt/tomcat
fi

IFS=';' read -ra NAMES <<< "$DAs";
for i in "${NAMES[@]}"; do
  echo "KeyValue-Pair: "
  echo $i
  IFS=',' read -ra entry <<< "$i";
      echo "Key: "
      echo ${entry[0]}
      echo "Value: "
      echo ${entry[1]}

  # find the .war file
  if [[ "${entry[1]}" == *.war ]];
  then
    if [[ -z "$context_path" || "${context_path^^}" == ROOT ]]; then
      sudo rm -rf $CATALINA_HOME/webapps/ROOT
      sudo cp $CSAR${entry[1]} $CATALINA_HOME/webapps/ROOT.war
    else
      sudo cp $CSAR${entry[1]} $CATALINA_HOME/webapps/${context_path}.war
    fi
  fi
done

exit 0
