#!/bin/sh
source /etc/profile

cd $(dirname $0)


nohup java -jar -Xms1024m -Xmx2048m -XX:PermSize=128M -XX:MaxPermSize=256M -XX:+UseG1GC -XX:MaxGCPauseMillis=20 -XX:InitiatingHeapOccupancyPercent=35 -XX:+ExplicitGCInvokesConcurrent -XX:MaxInlineLevel=15 ./eureka/eureka.jar --server.port=8610  > eureka.log 2>&1 &

