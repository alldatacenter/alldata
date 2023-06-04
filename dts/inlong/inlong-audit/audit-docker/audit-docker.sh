#!/bin/bash
#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

file_path=$(cd "$(dirname "$0")"/../;pwd)
# store config
store_conf_file=${file_path}/conf/application.properties
# proxy config
proxy_conf_file=${file_path}/conf/audit-proxy-${MQ_TYPE}.conf
sql_file="${file_path}"/sql/apache_inlong_audit.sql
sql_ck_file="${file_path}"/sql/apache_inlong_audit_clickhouse.sql

# replace the configuration for audit proxy
sed -i "s/manager.hosts=.*$/manager.hosts=${MANAGER_OPENAPI_IP}:${MANAGER_OPENAPI_PORT}/g" "${store_conf_file}"
sed -i "s/proxy.cluster.tag=.*$/proxy.cluster.tag=${CLUSTER_TAG}/g" "${store_conf_file}"
if [ "${MQ_TYPE}" = "pulsar" ]; then
  sed -i "s/audit.config.proxy.type=.*$/audit.config.proxy.type=pulsar"/g "${store_conf_file}"
  sed -i "s/audit.pulsar.topic = .*$/audit.pulsar.topic = ${PULSAR_AUDIT_TOPIC}/g" "${store_conf_file}"
  sed -i "s/agent1.sinks.pulsar-sink-msg1.topic = .*$/agent1.sinks.pulsar-sink-msg1.topic = ${PULSAR_AUDIT_TOPIC}/g" "${proxy_conf_file}"
  sed -i "s/agent1.sinks.pulsar-sink-msg2.topic = .*$/agent1.sinks.pulsar-sink-msg2.topic = ${PULSAR_AUDIT_TOPIC}/g" "${proxy_conf_file}"
fi
if [ "${MQ_TYPE}" = "kafka" ]; then
  sed -i "s/audit.config.proxy.type=.*$/audit.config.proxy.type=kafka"/g "${store_conf_file}"
fi
if [ "${MQ_TYPE}" = "tubemq" ]; then
  sed -i "s/audit.config.proxy.type=.*$/audit.config.proxy.type=tube"/g "${store_conf_file}"
  sed -i "s/audit.tube.topic = .*$/audit.tube.topic = ${TUBE_AUDIT_TOPIC}/g" "${store_conf_file}"
  sed -i "s/agent1.sinks.tube-sink-msg1.topic = .*$/agent1.sinks.tube-sink-msg1.topic = ${TUBE_AUDIT_TOPIC}/g" "${proxy_conf_file}"
  sed -i "s/agent1.sinks.tube-sink-msg2.topic = .*$/agent1.sinks.tube-sink-msg2.topic = ${TUBE_AUDIT_TOPIC}/g" "${proxy_conf_file}"
fi

# replace the configuration for audit store
if [ -n "${STORE_MODE}" ]; then
  sed -i "s/audit.config.store.mode=.*$/audit.config.store.mode=${STORE_MODE}/g" "${store_conf_file}"
fi
# DB
sed -i "s/127.0.0.1:3306\/apache_inlong_audit/${JDBC_URL}\/${AUDIT_DBNAME}/g" "${store_conf_file}"
sed -i "s/spring.datasource.druid.username=.*$/spring.datasource.druid.username=${USERNAME}/g" "${store_conf_file}"
sed -i "s/spring.datasource.druid.password=.*$/spring.datasource.druid.password=${PASSWORD}/g" "${store_conf_file}"
# mysql file for audit
sed -i "s/apache_inlong_audit/${AUDIT_DBNAME}/g" "${sql_file}"
# clickhouse
sed -i "s/clickhouse.url=.*$/clickhouse.url=jdbc:clickhouse:\/\/${STORE_CK_URL}\/${STORE_CK_DBNAME}/g" "${store_conf_file}"
sed -i "s/clickhouse.username=.*$/clickhouse.username=${STORE_CK_USERNAME}/g" "${store_conf_file}"
sed -i "s/clickhouse.password=.*$/clickhouse.password=${STORE_CK_PASSWD}/g" "${store_conf_file}"
# mysql file for clickhouse
sed -i "s/apache_inlong_audit/${STORE_CK_DBNAME}/g" "${sql_ck_file}"
# elasticsearch
sed -i "s/elasticsearch.host=.*$/elasticsearch.host=${STORE_ES_HOST}/g" "${store_conf_file}"
sed -i "s/elasticsearch.port=.*$/elasticsearch.port=${STORE_ES_PORT}/g" "${store_conf_file}"
sed -i "s/elasticsearch.authEnable=.*$/elasticsearch.authEnable=${STORE_ES_AUTHENABLE}/g" "${store_conf_file}"
sed -i "s/elasticsearch.username=.*$/elasticsearch.username=${STORE_ES_USERNAME}/g" "${store_conf_file}"
sed -i "s/elasticsearch.password=.*$/elasticsearch.password=${STORE_ES_PASSWD}/g" "${store_conf_file}"

# Whether the database table exists. If it does not exist, initialize the database and skip if it exists.
if [[ "${JDBC_URL}" =~ (.+):([0-9]+) ]]; then
  datasource_hostname=${BASH_REMATCH[1]}
  datasource_port=${BASH_REMATCH[2]}

  select_db_sql="SELECT COUNT(*) FROM information_schema.TABLES WHERE table_schema = 'apache_inlong_audit'"
  inlong_audit_count=$(mysql -h${datasource_hostname} -P${datasource_port} -u${USERNAME} -p${PASSWORD} -e "${select_db_sql}")
  inlong_num=$(echo "$inlong_audit_count" | tr -cd "[0-9]")
  if [ "${inlong_num}" = 0 ]; then
    mysql -h${datasource_hostname} -P${datasource_port} -u${USERNAME} -p${PASSWORD} < sql/apache_inlong_audit.sql
  fi
fi

# start proxy
cd "${file_path}/"
if [ "${START_MODE}" = "all" ] || [ "${START_MODE}" = "proxy" ]; then
  if [ "${MQ_TYPE}" = "pulsar" ]; then
    bash +x ./bin/proxy-start.sh pulsar
  fi
  if [ "${MQ_TYPE}" = "kafka" ]; then
    bash +x ./bin/proxy-start.sh kafka
  fi
  if [ "${MQ_TYPE}" = "tubemq" ]; then
    bash +x ./bin/proxy-start.sh tubemq
  fi
fi
# start store
if [ "${START_MODE}" = "all" ] || [ "${START_MODE}" = "store" ]; then
  bash +x ./bin/store-start.sh
fi
sleep 3
# keep alive
tail -F ./logs/info.log
