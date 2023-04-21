#!/bin/bash


export LOG_DIR="/opt/edp/${service.serviceName}/log"

export KAFKA_OPTS="$KAFKA_OPTS -Xmx${conf['kafka.server.memory']?number?floor?c}m -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.port=9921 -javaagent:/opt/jmx_exporter/jmx_prometheus_javaagent-0.14.0.jar=5551:/opt/edp/${service.serviceName}/conf/jmx_prometheus.yaml"

kafka-server-start.sh  /opt/edp/${service.serviceName}/conf/server.properties

