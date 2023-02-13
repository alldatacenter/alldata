#!/usr/bin/env bash

if [ "${ENV_TYPE}" == "Standalone" ]
then
    mysql -h${DB_HOST} -P${DB_PORT} -u${DB_USER} -p${DB_PASSWORD} -D${DB_NAME_AUTHPROXY} -e 'delete from ta_user where id in (2,3,4)'
fi