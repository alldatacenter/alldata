
mysql -h${DB_HOST} -P${DB_PORT} -u${DB_USER} -p${DB_PASSWORD} -D${DB_NAME} -e 'UPDATE ta_user SET avatar = "${DEFAULT_AMDIN_AVATOR}"'
