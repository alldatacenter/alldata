#!/bin/sh
source /etc/profile

# gateway单独启动
#echo "即将启动任务gateway"
#sh gateway.sh
#sleep 15s

echo "即将启动任务standard"
sh data-standard-service.sh

echo "即将启动任务visual"
sh data-visual-service.sh

echo "即将启动任务email"
sh email-service.sh

echo "即将启动任务file"
sh file-service.sh

echo "即将启动任务quartz"
sh quartz-service.sh


