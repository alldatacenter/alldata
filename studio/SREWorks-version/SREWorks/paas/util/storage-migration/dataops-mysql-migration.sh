#!/bin/sh

set -x
set -e 

force="false"
while getopts 'f' OPT; do
    case $OPT in
        f) force="true";;
        ?) ;;
    esac
done

dataops_databases=(aiops_aisp pmdb sw_saas_dataset sw_saas_datasource sw_saas_health sw_saas_warehouse)
for database in ${dataops_databases[@]}
do
  mysql -h${NEW_DATA_DB_HOST} -u${NEW_DATA_DB_USER} -p${NEW_DATA_DB_PASSWORD} -P${NEW_DATA_DB_PORT} -e "CREATE DATABASE IF NOT EXISTS ${database}"
  if [[ $force == "true" ]]; then
    mysqldump -h${DATAOPS_DB_HOST} -P${DATAOPS_DB_PORT} -u${DATAOPS_DB_USER} -p${DATAOPS_DB_PASSWORD} --databases ${database} --add-drop-table | mysql ${database} -h${NEW_DATA_DB_HOST} -u${NEW_DATA_DB_USER} -p${NEW_DATA_DB_PASSWORD} -P${NEW_DATA_DB_PORT}
  else
    mysqldump -h${DATAOPS_DB_HOST} -P${DATAOPS_DB_PORT} -u${DATAOPS_DB_USER} -p${DATAOPS_DB_PASSWORD} --databases ${database} | mysql ${database} -h${NEW_DATA_DB_HOST} -u${NEW_DATA_DB_USER} -p${NEW_DATA_DB_PASSWORD} -P${NEW_DATA_DB_PORT}
  fi
done


mysql -h${NEW_DATA_DB_HOST} -u${NEW_DATA_DB_USER} -p${NEW_DATA_DB_PASSWORD} -P${NEW_DATA_DB_PORT} pmdb -e "REPLACE INTO datasource(id, name, type, connect_config, build_in, app_id, creator, last_modifier, description) VALUES (1, 'sreworks_es', 'es', '{\"schema\":\"http\",\"port\":${DATA_DEST_ES_PORT},\"host\":\"${DATA_DEST_ES_HOST}\",\"username\":\"${DATA_DEST_ES_USER}\",\"password\":\"${DATA_DEST_ES_PASSWORD}\"}', true, 0, 'sreworks', 'sreworks', 'sreworks_es数据源');"

mysql -h${NEW_DATA_DB_HOST} -u${NEW_DATA_DB_USER} -p${NEW_DATA_DB_PASSWORD} -P${NEW_DATA_DB_PORT} pmdb -e "REPLACE INTO datasource(id, name, type, connect_config, build_in, app_id, creator, last_modifier, description) VALUES (2, 'sreworks_meta_mysql', 'mysql', '{\"password\":\"${NEW_DB_PASSWORD}\",\"port\":${NEW_DB_PORT},\"host\":\"${NEW_DB_HOST}\",\"db\":\"sreworks_meta\",\"username\":\"${NEW_DB_USER}\"}', true, 0, 'sreworks', 'sreworks', 'sreworks_meta数据源');"
