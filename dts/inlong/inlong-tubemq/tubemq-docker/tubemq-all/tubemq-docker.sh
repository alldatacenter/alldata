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
# wait the service to start
wait_port_to_listen() {
  service_name=$1
  service_port=$2
  result=$(netstat -anp | grep ${service_port})
  while [[ -z "$result" ]]; do
    echo "waiting $service_name to start, sleep 3s ..."
    sleep 3
    result=$(netstat -anp | grep ${service_port})
  done
}

cd /opt/tubemq-server/bin/
# configure useWebProxy
if [[ $USE_WEB_PROXY ]]; then
  sed -i 's/useWebProxy=.*/useWebProxy=true/g' /opt/tubemq-server/conf/master.ini
else
  sed -i 's/useWebProxy=.*/useWebProxy=false/g' /opt/tubemq-server/conf/master.ini
fi
# for standalone, start all services
if [ "${TARGET}" = "standalone" ]; then
  # zookeeper start
  /docker-entrypoint.sh zkServer.sh start
  wait_port_to_listen zookeeper 2181
  # master start
  ./tubemq.sh master start
  wait_port_to_listen master 8080
  # add broker
  curl -d "type=op_modify&method=admin_add_broker_configure&brokerId=1\
    &brokerIp=127.0.0.1&brokerPort=8123&deletePolicy=delete,168h&numPartitions=3\
    &unflushThreshold=1000&acceptPublish=true&acceptSubscribe=true&unflushInterval=10000\
    &createUser=docker&confModAuthToken=abc" http://127.0.0.1:8080/webapi.htm
  # online
  curl -d "type=op_modify&method=admin_online_broker_configure&brokerId=1\
    &modifyUser=docker&confModAuthToken=abc" http://127.0.0.1:8080/webapi.htm
  # broker start
  ./tubemq.sh broker start
  tail -F /opt/tubemq-server/logs/*
fi
# for master
if [ "${TARGET}" = "master" ]; then
  ./tubemq.sh master start
  tail -F /opt/tubemq-server/logs/master.log
fi
# for broker
if [ "${TARGET}" = "broker" ]; then
  ./tubemq.sh broker start
  tail -F /opt/tubemq-server/logs/broker.log
fi