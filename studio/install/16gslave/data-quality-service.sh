#!/bin/sh
source /etc/profile

cd $(dirname $0)


nohup java -jar -Xms128m -Xmx2048m -XX:PermSize=128M -XX:MaxPermSize=256M -XX:+UseG1GC -XX:MaxGCPauseMillis=20 -XX:InitiatingHeapOccupancyPercent=35 -XX:+ExplicitGCInvokesConcurrent -XX:MaxInlineLevel=15 ./data-quality-service/data-quality-service.jar --server.port=8826  > data-quality-service.log 2>&1 &

