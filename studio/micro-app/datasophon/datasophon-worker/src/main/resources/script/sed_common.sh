#!/bin/bash
masterHost=$1
masterWebPort=$2
frameCode=$3
clusterId=$4
installPath=$5
sed -i -e "s:masterHost=.*:masterHost=${masterHost}:g" ${installPath}/datasophon-worker/conf/common.properties
sed -i -e "s:masterWebPort=.*:masterWebPort=${masterWebPort}:g" ${installPath}/datasophon-worker/conf/common.properties
sed -i -e "s:frameCode=.*:frameCode=${frameCode}:g" ${installPath}/datasophon-worker/conf/common.properties
sed -i -e "s:clusterId=.*:clusterId=${clusterId}:g" ${installPath}/datasophon-worker/conf/common.properties
echo "success"