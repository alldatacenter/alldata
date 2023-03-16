#!/bin/sh
source /etc/profile

pid=$(ps -ef | grep system.jar | grep -Ev 'color=auto' | awk '{print $2}')
echo "已启动system"

pid=$(ps -ef | grep data-market-service.jar | grep -Ev 'color=auto' | awk '{print $2}')
echo "已启动data-market-service"

pid=$(ps -ef | grep data-metadata-service.jar | grep -Ev 'color=auto' | awk '{print $2}')
echo "已启动metadata"

pid=$(ps -ef | grep service-data-integration.jar | grep -Ev 'color=auto' | awk '{print $2}')
echo "已启动service-data-integration"

pid=$(ps -ef | grep dts.jar | grep -Ev 'color=auto' | awk '{print $2}')
echo "已启动dts"