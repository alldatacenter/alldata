#!/bin/sh


source /etc/profile

pid=$(ps -ef | grep alldata.jar | grep -Ev 'color=auto' | awk '{print $2}')

echo "即将杀死任务: $pid"

kill -9 $pid

echo "已杀死任务: $pid"
