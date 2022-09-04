#!/bin/bash

set -e
set -x

IP_LIST=$(echo "${ELASTICSEARCH_HOST}" | sed -n 1'p' | tr ',' '\n')
for ip in ${IP_LIST}; do
    export ELASTICSEARCH_HOST="${ip}"
done


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

# 渲染变量 (for bigdatak)
# shellcheck disable=SC2044
ENV_ARG=$(awk 'BEGIN{for(v in ENVIRON) printf "${%s} ", v;}')
for file in $(find '/usr/local/bigdatak'); do
    if [ "${file: -4}" == ".tpl" ]; then
        echo "Replace template file: ${file} "
        new_basename=$(basename ${file} .tpl)
        dir=$(dirname "$file")
        envsubst "${ENV_ARG}" <${file} >${dir}/${new_basename}
    fi
done

# 添加ES配置到数据库
echo "Import staragent sql files..."
MYSQL_CMD="mysql -h${DB_HOST} -u${DB_USER} -p${DB_PASSWORD} -P${DB_PORT} -D${DB_NAME} -e"
${MYSQL_CMD} "delete from config where name = 'backendStores'"
${MYSQL_CMD} "INSERT IGNORE INTO config (gmt_create,gmt_modified,category,nr_type,nr_id,name,modifier,content) VALUES ('2020-03-24 22:46:53','2020-04-03 00:57:40','__category','__nrType','__nrId','backendStores','83242','[{\"schema\":\"http\",\"password\":\"${ELASTICSEARCH_PASSWORD}\",\"default_store\":true,\"port\":${ELASTICSEARCH_PORT},\"name\":\"backend_store_basic\",\"host\":\"${ELASTICSEARCH_HOST}\",\"index_patterns\":{},\"type\":\"elasticsearch\",\"user\":\"${ELASTICSEARCH_USER}\",\"backup_store\":\"\"}]');"

export JVM_XMX="320m"

exec java -Xmx${JVM_XMX} -Xms${JVM_XMX} -XX:ActiveProcessorCount=2 -cp 'app:app/lib/*' com.alibaba.tesla.ApplicationPrivate
