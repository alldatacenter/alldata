#!/bin/sh
source /etc/profile

pid=$(ps -ef | grep config.jar | grep -Ev 'color=auto' | awk '{print $2}')
echo "已启动config:$pid"

pid=$(ps -ef | grep service-system.jar | grep -Ev 'color=auto' | awk '{print $2}')
echo "已启动service-system:$pid"

pid=$(ps -ef | grep data-market-service.jar | grep -Ev 'color=auto' | awk '{print $2}')
echo "已启动data-market-service:$pid"

pid=$(ps -ef | grep data-metadata-service.jar | grep -Ev 'color=auto' | awk '{print $2}')
echo "已启动metadata:$pid"

pid=$(ps -ef | grep service-data-integration.jar | grep -Ev 'color=auto' | awk '{print $2}')
echo "已启动service-data-integration:$pid"
