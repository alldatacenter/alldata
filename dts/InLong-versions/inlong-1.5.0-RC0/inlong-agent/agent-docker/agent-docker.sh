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
local_ip=$(ifconfig $ETH_NETWORK | grep "inet" | grep -v "inet6" | awk '{print $2}')
# config

sed -i "s/agent.local.ip=.*$/agent.local.ip=$local_ip/g" "${file_path}/conf/agent.properties"
sed -i "s/agent.fetcher.interval=.*$/agent.fetcher.interval=$AGENT_FETCH_INTERVAL/g" "${file_path}/conf/agent.properties"
sed -i "s/agent.heartbeat.interval=.*$/agent.heartbeat.interval=$AGENT_HEARTBEAT_INTERVAL/g" "${file_path}/conf/agent.properties"
sed -i "s/agent.manager.vip.http.host=.*$/agent.manager.vip.http.host=$MANAGER_OPENAPI_IP/g" "${file_path}/conf/agent.properties"
sed -i "s/agent.manager.vip.http.port=.*$/agent.manager.vip.http.port=$MANAGER_OPENAPI_PORT/g" "${file_path}/conf/agent.properties"
sed -i "s/audit.enable=.*$/audit.enable=$AUDIT_ENABLE/g" "${file_path}/conf/agent.properties"
sed -i "s/audit.proxys=.*$/audit.proxys=$AUDIT_PROXY_URL/g" "${file_path}/conf/agent.properties"
sed -i "s/agent.cluster.tag=.*$/agent.cluster.tag=$CLUSTER_TAG/g" "${file_path}/conf/agent.properties"
sed -i "s/agent.cluster.name=.*$/agent.cluster.name=$CLUSTER_NAME/g" "${file_path}/conf/agent.properties"
sed -i "s/agent.cluster.inCharges=.*$/agent.cluster.inCharges=$CLUSTER_IN_CHARGES/g" "${file_path}/conf/agent.properties"
sed -i "s/agent.manager.auth.secretId=.*$/agent.manager.auth.secretId=$MANAGER_OPENAPI_AUTH_ID/g" "${file_path}/conf/agent.properties"
sed -i "s/agent.manager.auth.secretKey=.*$/agent.manager.auth.secretKey=$MANAGER_OPENAPI_AUTH_KEY/g" "${file_path}/conf/agent.properties"
sed -i "s/agent.custom.fixed.ip=.*$/agent.custom.fixed.ip=$CUSTOM_FIXED_IP/g" "${file_path}/conf/agent.properties"

# start
bash +x ${file_path}/bin/agent.sh start
sleep 3
# keep alive
tail -F ${file_path}/logs/info.log
