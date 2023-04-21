#!/bin/bash

export SPARK_PID_DIR=/opt/edp/${service.serviceName}/data
export SPARK_LOG_DIR=/opt/edp/${service.serviceName}/log
export SPARK_LOCAL_DIRS=/opt/edp/${service.serviceName}/data/local
export SPARK_HISTORY_OPTS="$SPARK_HISTORY_OPTS  -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.port=9924 -javaagent:/opt/jmx_exporter/jmx_prometheus_javaagent-0.14.0.jar=5554:/opt/edp/${service.serviceName}/conf/jmx_prometheus.yaml"

${r"${SPARK_HOME}"}/sbin/start-history-server.sh --properties-file /opt/edp/${service.serviceName}/conf/spark-defaults.conf

tail -f /dev/null