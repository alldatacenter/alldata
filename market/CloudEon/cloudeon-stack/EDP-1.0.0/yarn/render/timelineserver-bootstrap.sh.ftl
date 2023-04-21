#!/usr/bin/env bash





echo "========================start timelineserver========================"
${r"${HADOOP_HOME}"}/sbin/yarn-daemon.sh --config /opt/edp/${service.serviceName}/conf start timelineserver

tail -f /dev/null