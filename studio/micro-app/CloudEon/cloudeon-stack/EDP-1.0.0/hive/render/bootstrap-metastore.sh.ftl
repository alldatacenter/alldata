#!/bin/bash

export HIVE_CONF_DIR=/opt/edp/${service.serviceName}/conf

nohup hive --service metastore >> /opt/edp/${service.serviceName}/log/metastore_log_`date '+%Y-%m-%d'` 2>&1 &

tail -f /dev/null