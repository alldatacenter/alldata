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
# Obtain the local ip by specifying the network card name according to the environment variable and
# it should be noted that this is a single network card IP acquisition method.
local_ip=$(ifconfig $ETH_NAME | grep inet | grep -v inet6 | grep -v "127.0.0.1" | awk '{print $2}')
# config
cd "${file_path}/"
common_conf_file=./conf/common.properties
if [ "${MQ_TYPE}" == "pulsar" ] || [ "${MQ_TYPE}" == "kafka" ]; then
  mq_conf_file=./conf/dataproxy.conf
elif [ "${MQ_TYPE}" == "tubemq" ]; then
  mq_conf_file=./conf/dataproxy-${MQ_TYPE}.conf
else
  echo "MQ_TYPE must be one of pulsar/kafka/tubemq !"
  exit 1
fi

sed -i "s/manager.hosts=.*$/manager.hosts=${MANAGER_OPENAPI_IP}:${MANAGER_OPENAPI_PORT}/g" "${common_conf_file}"
sed -i "s/audit.enable=.*$/audit.enable=${AUDIT_ENABLE}/g" "${common_conf_file}"
sed -i "s/audit.proxys=.*$/audit.proxys=${AUDIT_PROXY_URL}/g" "${common_conf_file}"
sed -i "s/localhost.*$/${local_ip}/g" "${mq_conf_file}"
sed -i "s/proxy.cluster.tag=.*$/proxy.cluster.tag=${CLUSTER_TAG}/g" "${common_conf_file}"
sed -i "s/proxy.cluster.name=.*$/proxy.cluster.name=${CLUSTER_NAME}/g" "${common_conf_file}"
sed -i "s/proxy.cluster.inCharges=.*$/proxy.cluster.inCharges=${CLUSTER_IN_CHARGES}/g" "${common_conf_file}"

bash +x ./bin/dataproxy-start.sh "${MQ_TYPE}"

sleep 3
# keep alive
tail -F ./logs/info.log