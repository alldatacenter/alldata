#!/bin/bash
#
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#
cd "$(dirname "$0")"/../conf || exit
basedir="$(pwd)"

error() {
  local msg=$1
  local exit_code=$2

  echo "Error: $msg" >&2

  if [ -n "$exit_code" ] ; then
    exit $exit_code
  fi
}

for i in {mx.properties,transfer.properties,weight.properties,common.properties,blacklist.properties,groupid_mapping.properties,dc_mapping.properties,topics.properties,tube_switch.properties,mq_cluster.properties}
do
  if [ ! -f "$i" ]; then
    touch "$i"
  fi
done

cd .. || exit

MQ_TYPE=pulsar
if [ -n "$1" ]; then
  MQ_TYPE=$1
fi

CONFIG_FILE="dataproxy-${MQ_TYPE}.conf"
if [ "${MQ_TYPE}" == "pulsar" ] || [ "${MQ_TYPE}" == "kafka" ]; then
  CONFIG_FILE="dataproxy.conf"
fi

CONFIG_FILE_WITH_COFING_PATH="conf/${CONFIG_FILE}"
CONFIG_FILE_WITH_PATH="${basedir}/${CONFIG_FILE}"

if [ -f "$CONFIG_FILE_WITH_PATH" ]; then
  nohup bash +x bin/dataproxy-ng agent --conf conf/ -f "${CONFIG_FILE_WITH_COFING_PATH}" -n agent1 --no-reload-conf  > /dev/null 2>&1 &
else
   error "${CONFIG_FILE_WITH_PATH} is not exist! start failed!" 1
fi

