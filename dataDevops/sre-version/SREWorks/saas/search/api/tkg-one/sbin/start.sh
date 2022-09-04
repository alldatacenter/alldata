#!/bin/sh
set -x

# 显示当前 ENV 变量
DOCKER_COMMAND='docker run -it --entrypoint "bash" '
for item in $(env); do
  DOCKER_COMMAND+="-e ${item} "
done
DOCKER_COMMAND+="IMAGE_ID"
echo "Docker Comm

and: ${DOCKER_COMMAND}"

export APP_NAME=tkg-one
export HTTP_SERVER_PORT=${HTTP_SERVER_PORT-"7001"}

## ilogtail
echo ${APP_NAME}_${ENV} > /etc/ilogtail/user_defined_id
if [ ! -f "/etc/ilogtail/users/1270632786127642" ]; then
  touch /etc/ilogtail/users/1270632786127642
fi


SERVICE_OPTS="${SERVICE_OPTS} -Xmx320m -Xms320m"
SERVICE_OPTS="${SERVICE_OPTS} -XX:ActiveProcessorCount=2"
## SERVICE_OPTS="${SERVICE_OPTS} -Dspring.profiles.active=oxs"
SERVICE_OPTS="${SERVICE_OPTS} -Dproject.name=${APP_NAME}"

exec java ${SERVICE_OPTS} -jar $(cd "$(dirname "$0")";pwd)/tkg-one.jar --spring.config.location=/app/