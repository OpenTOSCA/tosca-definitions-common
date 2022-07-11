export DEBIAN_FRONTEND=noninteractive
apt update
apt install -y libapache2-mod-php
service apache2 restart