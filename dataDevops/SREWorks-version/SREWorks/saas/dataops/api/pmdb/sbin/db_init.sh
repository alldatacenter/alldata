#!/bin/bash

set -e
set -x

mysql -h${DATA_DB_HOST} -u${DATA_DB_USER} -p${DATA_DB_PASSWORD} -P${DATA_DB_PORT} -e "CREATE DATABASE IF NOT EXISTS ${DATA_DB_PMDB_NAME} DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_general_ci"

ENV_ARG=$(awk 'BEGIN{for(v in ENVIRON) printf "${%s} ", v;}')

for file in /sql/*; do
    if [ "${file: -4}" == ".tpl" ]; then
        echo "Replace template SQL file: ${file} "
        new_basename=$(basename ${file} .tpl)
        envsubst "${ENV_ARG}" <${file} >/sql/${new_basename}
        rm -f ${file}
    fi
done

echo "Current SQL files:"
ls -1 /sql

echo "Running migrations..."
/migrate -source "file://sql" -database "mysql://${DATA_DB_USER}:${DATA_DB_PASSWORD}@tcp(${DATA_DB_HOST}:${DATA_DB_PORT})/${DATA_DB_PMDB_NAME}" up

if [ -d "/cron_sql" ]; then
    for file in /cron_sql/*; do
        if [ "${file: -4}" == ".tpl" ]; then
            echo "Replace cron template SQL file: ${file} "
            new_basename=$(basename ${file} .tpl)
            envsubst "${ENV_ARG}" <${file} >/cron_sql/${new_basename}
            rm -f ${file}
        fi
    done

    echo "Running cron sql files..."
    for file in /cron_sql/*; do
        echo "    --> Running cron SQL file: ${file} "
        mysql -h${DATA_DB_HOST} -u${DATA_DB_USER} -p${DATA_DB_PASSWORD} -P${DATA_DB_PORT} -D${DATA_DB_PMDB_NAME} <${file}
    done
    echo "Done."
fi