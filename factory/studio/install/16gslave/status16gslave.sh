#!/bin/sh
source /etc/profile


pid=$(ps -ef | grep eureka.jar | grep -Ev 'color=auto' | awk '{print $2}')
echo "已启动eureka"

pid=$(ps -ef | grep config.jar | grep -Ev 'color=auto' | awk '{print $2}')
echo "已启动config"

pid=$(ps -ef | grep gateway.jar | grep -Ev 'color=auto' | awk '{print $2}')
echo "已启动gateway"

pid=$(ps -ef | grep data-market-service-mapping.jar | grep -Ev 'color=auto' | awk '{print $2}')
echo "已启动data-market-service-mapping"

pid=$(ps -ef | grep data-masterdata-service.jar | grep -Ev 'color=auto' | awk '{print $2}')
echo "已启动data-masterdata-service"

pid=$(ps -ef | grep data-metadata-service-console.jar | grep -Ev 'color=auto' | awk '{print $2}')
echo "已启动data-metadata-service-console"

pid=$(ps -ef | grep data-quality-service.jar | grep -Ev 'color=auto' | awk '{print $2}')
echo "已启动data-quality-service"


pid=$(ps -ef | grep workflow-service.jar | grep -Ev 'color=auto' | awk '{print $2}')
echo "已启动workflow-service"

echo "启动列表：data-market-service-mapping、data-masterdata-service、data-metadata-service-console、data-quality-service、eureka、gateway、workflow-service"