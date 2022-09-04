#!/bin/bash

set -e
set -x

# 显示当前 ENV 变量
DOCKER_COMMAND='docker run -it --entrypoint "bash" '
for item in $(env); do
  DOCKER_COMMAND+="-e ${item} "
done
DOCKER_COMMAND+="IMAGE_ID"
echo "Docker Command: ${DOCKER_COMMAND}"

export JVM_XMX="200m"

exec java -Xmx${JVM_XMX} -Xms${JVM_XMX} -XX:NewRatio=3 -XX:ActiveProcessorCount=4 -Dloader.path=/app/ ${EXTRA_JVM_PARAMETERS} -jar /app/aisp-process-strategy.jar --spring.config.location=/app/
