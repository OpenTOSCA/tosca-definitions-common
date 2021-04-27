#!/bin/bash
cd  /etc/nginx/sites-enabled

sudo echo "
server {
    listen 80 default_server;
    listen [::]:80 default_server;

    root /var/www/html/${AppName};

    location / {
      index index.html;
    }
}
" > default

sudo service nginx restart
