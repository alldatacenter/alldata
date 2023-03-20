#!/bin/sh
source /etc/profile

cd $(dirname $0)

nohup java -jar -Xms1024m -Xmx2048m -XX:PermSize=128M -XX:MaxPermSize=256M -XX:+UseG1GC -XX:MaxGCPauseMillis=20 -XX:InitiatingHeapOccupancyPercent=35 -XX:+ExplicitGCInvokesConcurrent -XX:MaxInlineLevel=15 ./config/config.jar --server.port=8611 > config.log 2>&1 &

