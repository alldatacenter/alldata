#!/bin/bash

export SPARK_PID_DIR=/opt/edp/${service.serviceName}/data
export SPARK_LOG_DIR=/opt/edp/${service.serviceName}/log
export SPARK_LOCAL_DIRS=/opt/edp/${service.serviceName}/data/local

${r"${SPARK_HOME}"}/sbin/start-thriftserver.sh  --properties-file /opt/edp/${service.serviceName}/conf/spark-defaults.conf --master yarn --deploy-mode client  --executor-memory 3g --total-executor-cores 3

tail -f /dev/null