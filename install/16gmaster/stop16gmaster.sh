#!/bin/sh
source /etc/profile

pid=$(ps -ef | grep system.jar | grep -Ev 'color=auto' | awk '{print $2}')

echo "即将杀死任务system: $pid"

kill -9 $pid

echo "已杀死任务: $pid"

pid=$(ps -ef | grep data-market-service.jar | grep -Ev 'color=auto' | awk '{print $2}')

echo "即将杀死任务data-market-service: $pid"

kill -9 $pid

echo "已杀死任务: $pid"

pid=$(ps -ef | grep data-metadata-service.jar | grep -Ev 'color=auto' | awk '{print $2}')

echo "即将杀死任务data-metadata-service: $pid"

kill -9 $pid

echo "已杀死任务: $pid"

pid=$(ps -ef | grep service-data-integration.jar | grep -Ev 'color=auto' | awk '{print $2}')

echo "即将杀死任务service-data-integration: $pid"

kill -9 $pid

echo "已杀死任务: $pid"

pid=$(ps -ef | grep service-data-integration.jar | grep -Ev 'color=auto' | awk '{print $2}')

echo "即将杀死任务service-data-integration: $pid"

kill -9 $pid

echo "已杀死任务: $pid"


pid=$(ps -ef | grep config.jar | grep -Ev 'color=auto' | awk '{print $2}')

echo "即将杀死任务config: $pid"

kill -9 $pid

echo "已杀死任务: $pid"