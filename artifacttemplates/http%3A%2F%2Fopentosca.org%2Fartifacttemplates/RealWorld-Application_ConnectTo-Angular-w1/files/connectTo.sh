#!/bin/bash
# Parameters:
# $Port
# $VMIP
# $SOURCE_AppName

sed -i "s#api_url:\"https://conduit.productionready.io/api\"#api_url:\"http://$VMIP:$Port\"#" /var/www/html/$SOURCE_AppName/main.*.js
