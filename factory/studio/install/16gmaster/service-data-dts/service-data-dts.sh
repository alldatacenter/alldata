#!/bin/sh
source /etc/profile

cd $(dirname $0)

nohup java -jar -Xms128m -Xmx2048m -XX:PermSize=128M -XX:MaxPermSize=256M -XX:+UseG1GC -XX:MaxGCPauseMillis=20 -XX:InitiatingHeapOccupancyPercent=35 -XX:+ExplicitGCInvokesConcurrent -XX:MaxInlineLevel=15 ./service-data-dts.jar --server.port=8810 --spring.profiles.active=dev > service-data-dts.log 2>&1 &

