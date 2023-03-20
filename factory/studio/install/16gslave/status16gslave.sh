#!/bin/sh
source /etc/profile


pid=$(ps -ef | grep eureka.jar | grep -Ev 'color=auto' | awk '{print $2}')
echo "已启动eureka: $pid"


pid=$(ps -ef | grep data-market-service-mapping.jar | grep -Ev 'color=auto' | awk '{print $2}')
echo "已启动data-market-service-mapping: $pid"

pid=$(ps -ef | grep data-masterdata-service.jar | grep -Ev 'color=auto' | awk '{print $2}')
echo "已启动data-masterdata-service: $pid"

pid=$(ps -ef | grep data-metadata-service-console.jar | grep -Ev 'color=auto' | awk '{print $2}')
echo "已启动data-metadata-service-console: $pid"

pid=$(ps -ef | grep data-quality-service.jar | grep -Ev 'color=auto' | awk '{print $2}')
echo "已启动data-quality-service: $pid"


pid=$(ps -ef | grep workflow-service.jar | grep -Ev 'color=auto' | awk '{print $2}')
echo "已启动workflow-service: $pid"

echo "启动列表：data-market-service-mapping、data-masterdata-service、data-metadata-service-console、data-quality-service、eureka、gateway、workflow-service"