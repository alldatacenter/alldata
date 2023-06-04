#!/bin/bash



hbase-daemon.sh --config /opt/edp/${service.serviceName}/conf start regionserver


tail -f /dev/null