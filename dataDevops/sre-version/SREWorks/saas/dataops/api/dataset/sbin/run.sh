#!/bin/sh

# 显示当前 ENV 变量
DOCKER_COMMAND='docker run -it --entrypoint "sh" '
for item in $(env); do
  DOCKER_COMMAND+="-e ${item} "
done
DOCKER_COMMAND+="IMAGE_ID"
echo "Docker Command: ${DOCKER_COMMAND}"

export JVM_XMS="256m"

export JVM_XMX="256m"

# SkyWalking ENV 配置
export SW_AGENT_NAMESPACE=data
export SW_AGENT_NAME=dataset
export SW_AGENT_COLLECTOR_BACKEND_SERVICES=${DATA_SKYW_HOST}:${DATA_SKYW_PORT}
export JAVA_AGENT=-javaagent:/app/skywalking-agent/skywalking-agent.jar

# Log GRPC Export ENV
export SW_GRPC_LOG_SERVER_HOST=${DATA_SKYW_HOST}
export SW_GRPC_LOG_SERVER_PORT=${DATA_SKYW_PORT}

if [ "${DATA_SKYW_ENABLE}" == "true" ]
then
  exec java -Xmx${JVM_XMX} -Xms${JVM_XMS} -XX:ActiveProcessorCount=2 -Dloader.path=/app/ $JAVA_AGENT -jar /app/dataset.jar --spring.config.location=/app/
else
  exec java -Xmx${JVM_XMX} -Xms${JVM_XMS} -XX:ActiveProcessorCount=2 -Dloader.path=/app/ -jar /app/dataset.jar --spring.config.location=/app/
fi
