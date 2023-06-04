#!/bin/sh
source /etc/profile

pid=$(ps -ef | grep gateway.jar | grep -Ev 'color=auto' | awk '{print $2}')

echo "已启动gateway: $pid"

pid=$(ps -ef | grep data-standard-service.jar | grep -Ev 'color=auto' | awk '{print $2}')

echo "已启动standard: $pid"

pid=$(ps -ef | grep data-visual-service.jar | grep -Ev 'color=auto' | awk '{print $2}')

echo "已启动visual: $pid"


pid=$(ps -ef | grep email-service.jar | grep -Ev 'color=auto' | awk '{print $2}')

echo "已启动email: $pid"

pid=$(ps -ef | grep file-service.jar | grep -Ev 'color=auto' | awk '{print $2}')

echo "已启动file: $pid"

pid=$(ps -ef | grep quartz-service.jar | grep -Ev 'color=auto' | awk '{print $2}')

echo "已启动quartz: $pid"
