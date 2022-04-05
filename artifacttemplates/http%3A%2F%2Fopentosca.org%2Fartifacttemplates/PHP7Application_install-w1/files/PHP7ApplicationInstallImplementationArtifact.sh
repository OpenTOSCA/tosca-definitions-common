export DEBIAN_FRONTEND=noninteractive
apt update
echo "<?php echo 'Hello World'; phpinfo() ?>" > var/www/html/index.php
service apache2 restart