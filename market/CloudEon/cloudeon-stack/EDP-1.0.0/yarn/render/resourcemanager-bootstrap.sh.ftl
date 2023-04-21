#!/usr/bin/env bash





echo "========================start resourcemanager========================"
${r"${HADOOP_HOME}"}/sbin/yarn-daemon.sh --config /opt/edp/${service.serviceName}/conf start resourcemanager
tail -f /dev/null