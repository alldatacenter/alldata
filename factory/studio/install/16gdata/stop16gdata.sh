#!/bin/sh
source /etc/profile

pid=$(ps -ef | grep data-standard-service.jar | grep -Ev 'color=auto' | awk '{print $2}')

echo "即将杀死任务standard: $pid"

kill -9 $pid

echo "已杀死任务: $pid"


pid=$(ps -ef | grep data-visual-service.jar | grep -Ev 'color=auto' | awk '{print $2}')

echo "即将杀死任务visual: $pid"

kill -9 $pid

echo "已杀死任务: $pid"

pid=$(ps -ef | grep email-service.jar | grep -Ev 'color=auto' | awk '{print $2}')

echo "即将杀死任务email: $pid"

kill -9 $pid

echo "已杀死任务: $pid"

pid=$(ps -ef | grep file-service.jar | grep -Ev 'color=auto' | awk '{print $2}')

echo "即将杀死任务file: $pid"

kill -9 $pid

echo "已杀死任务: $pid"

pid=$(ps -ef | grep quartz-service.jar | grep -Ev 'color=auto' | awk '{print $2}')

echo "即将杀死任务quartz: $pid"

kill -9 $pid

echo "已杀死任务: $pid"

pid=$(ps -ef | grep gateway.jar | grep -Ev 'color=auto' | awk '{print $2}')

echo "即将杀死任务gateway: $pid"

kill -9 $pid

echo "已杀死任务: $pid"
