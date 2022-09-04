#!/bin/sh

set -x
set -e 

databases=(sreworks_appmanager abm_paas_action abm_paas_authproxy abm_paas_nacos search_saas_tkgone sreworks_meta sreworks_saas_job)
for database in ${databases[@]}
do
  mysql -h${NEW_DB_HOST} -P${NEW_DB_PORT} -u${NEW_DB_USER} -p${NEW_DB_PASSWORD} -e "CREATE DATABASE IF NOT EXISTS ${database}"
  mysqldump -h${DB_HOST} -P${DB_PORT} -u${DB_USER} -p${DB_PASSWORD} --databases ${database} --add-drop-table | mysql ${database} -h${NEW_DB_HOST} -u${NEW_DB_USER} -p${NEW_DB_PASSWORD} -P${NEW_DB_PORT}
done
