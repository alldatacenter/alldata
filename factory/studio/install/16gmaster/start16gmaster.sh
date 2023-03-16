#!/bin/sh
source /etc/profile

#!/bin/sh
source /etc/profile

echo "即将启动任务system"
sh system.sh

echo "即将启动任务market"
sh data-market-service.sh

echo "即将启动任务metadata"
sh data-metadata-service.sh


echo "即将启动任务market-service-integration"
sh data-market-service-integration.sh

echo "即将启动任务dts"
sh dts.sh
