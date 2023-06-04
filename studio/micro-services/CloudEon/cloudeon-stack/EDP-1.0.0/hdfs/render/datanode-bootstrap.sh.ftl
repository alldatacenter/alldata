#!/usr/bin/env bash

source /opt/edp/${service.serviceName}/conf/hadoop-hdfs-env.sh

echo "========================starting datanode========================"
${r"${HADOOP_HOME}"}/sbin/hadoop-daemon.sh --config /opt/edp/${service.serviceName}/conf start datanode

tail -f /dev/null