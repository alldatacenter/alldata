#!/bin/sh


source /etc/profile

pid=$(ps -ef | grep alldata-eladmin-system.jar | grep -Ev 'color=auto' | awk '{print $2}')

echo "即将杀死任务alldata-eladmin-system: $pid"

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

pid=$(ps -ef | grep datax-config.jar | grep -Ev 'color=auto' | awk '{print $2}')

echo "即将杀死任务datax-config: $pid"

kill -9 $pid

echo "已杀死任务: $pid"

pid=$(ps -ef | grep datax-service-data-integration.jar | grep -Ev 'color=auto' | awk '{print $2}')

echo "即将杀死任务datax-service-data-integration: $pid"

kill -9 $pid

echo "已杀死任务: $pid"