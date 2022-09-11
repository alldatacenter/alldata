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

# replace the configuration for audit store
sed -i "s/127.0.0.1:3306/${JDBC_URL}/g" "${store_conf_file}"
sed -i "s/spring.datasource.druid.username=.*$/spring.datasource.druid.username=${USERNAME}/g" "${store_conf_file}"
sed -i "s/spring.datasource.druid.password=.*$/spring.datasource.druid.password=${PASSWORD}/g" "${store_conf_file}"

# replace the configuration for audit proxy
if [[ "${MQ_TYPE}" == "pulsar" ]]; then
  sed -i "s/audit.pulsar.server.url=.*$/audit.pulsar.server.url=pulsar:\/\/${PULSAR_BROKER_LIST}/g" "${store_conf_file}"
  sed -i "s/agent1.sinks.pulsar-sink-msg1.pulsar_server_url = .*$/agent1.sinks.pulsar-sink-msg1.pulsar_server_url = pulsar:\/\/${PULSAR_BROKER_LIST}/g" "${proxy_conf_file}"
  sed -i "s/agent1.sinks.pulsar-sink-msg2.pulsar_server_url = .*$/agent1.sinks.pulsar-sink-msg2.pulsar_server_url = pulsar:\/\/${PULSAR_BROKER_LIST}/g" "${proxy_conf_file}"
fi
if [[ "${MQ_TYPE}" == "tube" ]]; then
  sed -i "s/audit.tube.masterlist=.*$/audit.tube.masterlist=${TUBE_MASTER_LIST}/g" "${store_conf_file}"
  sed -i "s/agent1.sinks.tube-sink-msg1.master-host-port-list = .*$/agent1.sinks.tube-sink-msg1.master-host-port-list = ${TUBE_MASTER_LIST}/g" "${proxy_conf_file}"
  sed -i "s/agent1.sinks.tube-sink-msg2.master-host-port-list = .*$/agent1.sinks.tube-sink-msg2.master-host-port-list = ${TUBE_MASTER_LIST}/g" "${proxy_conf_file}"
fi

# Whether the database table exists. If it does not exist, initialize the database and skip if it exists.
if [[ "${JDBC_URL}" =~ (.+):([0-9]+) ]]; then
  datasource_hostname=${BASH_REMATCH[1]}
  datasource_port=${BASH_REMATCH[2]}

  select_db_sql="SELECT COUNT(*) FROM information_schema.TABLES WHERE table_schema = 'apache_inlong_audit'"
  inlong_audit_count=$(mysql -h${datasource_hostname} -P${datasource_port} -u${USERNAME} -p${PASSWORD} -e "${select_db_sql}")
  inlong_num=$(echo "$inlong_audit_count" | tr -cd "[0-9]")
  if [[ $inlong_num == 0 ]]; then
    mysql -h${datasource_hostname} -P${datasource_port} -u${USERNAME} -p${PASSWORD} < sql/apache_inlong_audit.sql
  fi
fi

# start
bash +x ${file_path}/bin/proxy-start.sh
bash +x ${file_path}/bin/store-start.sh
sleep 3
# keep alive
tail -F ${file_path}/logs/info.log
