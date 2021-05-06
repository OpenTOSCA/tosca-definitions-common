#!/bin/bash

#find csar root
csarRoot=$(find ~ -maxdepth 1 -path "*.csar");
#set JAVA_HOME
echo "JAVA_HOME=$JAVA_HOME" >> /etc/default/tomcat7;

echo "Got the following DAs:";
IFS=';' read -ra NAMES <<< "$DAs";
for i in "${NAMES[@]}"; do
        #echo "KeyValue-Pair: "
        #echo $i
        IFS=',' read -ra entry <<< "$i";    
        #echo "Key: "
        #echo ${entry[0]}
        #echo "Value: "
        #echo ${entry[1]}
	echo ${entry[1]}
        if [[ "${entry[1]}" == *tomcat-users.xml ]];
        then    
		echo "Copy of tomcat-users.xml"
	        sudo cp $csarRoot${entry[1]} /var/lib/tomcat7/conf
        fi
	if [[ "${entry[1]}" == *server.xml ]];
        then    
		echo "Copy of server.xml"
	        sudo cp $csarRoot${entry[1]} /var/lib/tomcat7/conf
        fi
done

sudo service tomcat7 restart
