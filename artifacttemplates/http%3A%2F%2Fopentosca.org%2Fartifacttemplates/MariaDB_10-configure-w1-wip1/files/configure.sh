#!/bin/bash
databaseuser=$DBUser
databasepw=$DBPassword
databasename=$DBName

# iterate over map of DAs
IFS=';' read -ra NAMES <<< "$DAs";
for i in "${NAMES[@]}"; do
	echo "KeyValue-Pair: "
    echo $i
    IFS=',' read -ra entry <<< "$i";
    	echo "Key: "
    	echo ${entry[0]}
    	echo "Value: "
    	echo ${entry[1]}

	# is it an sql file ? at least the ending
	if [[ "${entry[1]}" == *.sql ]];
	then
		# connect to database and dump in the statements
		mysql -u$databaseuser -p$databasepw -D$databasename < $CSAR${entry[1]};
	fi
done
