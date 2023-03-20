#!/bin/sh
source /etc/profile

#!/bin/sh
source /etc/profile


echo "即将启动任务config"
sh config/config.sh

sleep 10s


echo "即将启动任务system"
sh system-service/system-service.sh

echo "即将启动任务market"
sh data-market-service/data-market-service.sh

echo "即将启动任务metadata"
sh data-metadata-service/data-metadata-service.sh


echo "即将启动任务market-service-integration"
sh data-market-service-integration/data-market-service-integration.sh

