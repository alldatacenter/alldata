#!/bin/sh
source /etc/profile

# eureka单独启动
#echo "即将启动任务eureka"
#sh eureka.sh

#sleep 10s


echo "即将启动任务data-market-service-mapping"
sh data-market-service-mapping.sh

echo "即将启动任务master data service"
sh data-masterdata-service.sh

echo "即将启动任务metadata-console"
sh data-metadata-service-console.sh

echo "即将启动任务quality"
sh data-quality-service.sh

echo "即将启动任务workflow"
sh workflow-service.sh