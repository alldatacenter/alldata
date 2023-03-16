#!/bin/sh
source /etc/profile

echo "即将启动任务standard"
sh data-standard-service.sh

echo "即将启动任务visual"
sh data-visual-service.sh

echo "即将启动任务data-tool-monitor"
sh tool-monitor.sh

echo "即将启动任务email"
sh email-service.sh

echo "即将启动任务file"
sh file-service.sh

echo "即将启动任务quartz"
sh quartz-service.sh


