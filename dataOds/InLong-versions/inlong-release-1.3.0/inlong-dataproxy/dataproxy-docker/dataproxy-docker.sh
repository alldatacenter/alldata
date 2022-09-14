#!/bin/sh
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
local_ip=$(ifconfig | grep inet | grep -v inet6 | grep -v "127.0.0.1" | awk '{print $2}' | grep -v "30.*")
# config
cd "${file_path}/"
common_conf_file=./conf/common.properties
sed -i "s/manager.hosts=.*$/manager.hosts=${MANAGER_OPENAPI_IP}:${MANAGER_OPENAPI_PORT}/g" "${common_conf_file}"
sed -i "s/audit.proxys=.*$/audit.proxys=${AUDIT_PROXY_URL}/g" "${common_conf_file}"
sed -i "s/proxy.report.ip=.*$/proxy.report.ip=${local_ip}/g" "${common_conf_file}"
sed -i "s/proxy.cluster.tag=.*$/proxy.cluster.tag=${CLUSTER_TAG}/g" "${common_conf_file}"
sed -i "s/proxy.cluster.name=.*$/proxy.cluster.name=${CLUSTER_NAME}/g" "${common_conf_file}"
sed -i "s/proxy.cluster.inCharges=.*$/proxy.cluster.inCharges=${CLUSTER_IN_CHARGES}/g" "${common_conf_file}"

# start
if [ "${MQ_TYPE}" = "pulsar" ]; then
  bash +x ./bin/dataproxy-start.sh pulsar
fi
if [ "${MQ_TYPE}" = "tubemq" ]; then
  bash +x ./bin/dataproxy-start.sh tube
fi

sleep 3
# keep alive
tail -F ./logs/info.log