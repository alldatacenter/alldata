#!/bin/sh

set -x

# 显示当前 ENV 变量
DOCKER_COMMAND='docker run -it --entrypoint "bash" '
for item in $(env); do
  DOCKER_COMMAND+="-e ${item} "
done
DOCKER_COMMAND+="IMAGE_ID"
echo "Docker Command: ${DOCKER_COMMAND}"

# 更新 ENV_TYPE
if [ "${CLOUD_TYPE}" == "ApsaraStackInsight" ]; then
    export ENV_TYPE="DXZ"
elif [ "${CLOUD_TYPE}" == "ApsaraStackAgility" ]; then
    export ENV_TYPE="RQY"
else
    export ENV_TYPE=${CLOUD_TYPE}
fi

export JVM_XMX="256m"

exec java -Xmx${JVM_XMX} -Xms${JVM_XMX} -XX:ActiveProcessorCount=2 -Dloader.path=/app/ -jar /app/tesla-authproxy.jar --spring.config.location=/app/
