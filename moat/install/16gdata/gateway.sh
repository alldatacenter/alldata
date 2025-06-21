#!/bin/sh
source /etc/profile

cd $(dirname $0)


nohup java -jar -Xms1024m -Xmx2048m -XX:PermSize=128M -XX:MaxPermSize=256M -XX:+UseG1GC -XX:MaxGCPauseMillis=20 -XX:InitiatingHeapOccupancyPercent=35 -XX:+ExplicitGCInvokesConcurrent -XX:MaxInlineLevel=15 ./gateway/gateway.jar --server.port=9538  > gateway.log 2>&1 &

