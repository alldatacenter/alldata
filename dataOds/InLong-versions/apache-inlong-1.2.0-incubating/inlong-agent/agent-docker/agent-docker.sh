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
cat <<EOF > ${file_path}/conf/agent.properties
agent.fetcher.classname=org.apache.inlong.agent.plugin.fetcher.ManagerFetcher
agent.local.ip=$local_ip
agent.fetcher.interval=$AGENT_FETCH_INTERVAL
agent.heartbeat.interval=$AGENT_HEARTBEAT_INTERVAL
agent.manager.vip.http.host=$MANAGER_OPENAPI_IP
agent.manager.vip.http.port=$MANAGER_OPENAPI_PORT
agent.dataproxy.http.host=$DATAPROXY_IP
agent.dataproxy.http.port=$DATAPROXY_PORT
agent.http.port=8008
agent.http.enable=true
agent.prometheus.enable=true
agent.prometheus.exporter.port=8080
audit.proxys=$AUDIT_PROXY_URL
EOF
# start
bash +x ${file_path}/bin/agent.sh start
sleep 3
# keep alive
tail -F ${file_path}/logs/info.log
