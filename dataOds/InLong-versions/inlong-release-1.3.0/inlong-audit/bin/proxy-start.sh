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

BASE_DIR=$(
  # shellcheck disable=SC2164
  cd "$(dirname "$0")"
  cd ../
  pwd
)

# Enter the root directory path
# shellcheck disable=SC2164
cd "$BASE_DIR"

error() {
  local msg=$1
  local exit_code=$2

  echo "Error: $msg" >&2

  if [ -n "$exit_code" ]; then
    exit "$exit_code"
  fi
}

PREPARE_FILE=${BASE_DIR}/conf/server.properties
if [ ! -f "$PREPARE_FILE" ]; then
  touch "$PREPARE_FILE"
fi

MQ_TYPE=pulsar
if [ -n "$1" ]; then
  MQ_TYPE=$1
fi

CONFIG_FILE="audit-proxy-${MQ_TYPE}.conf"
CONFIG_FILE_WITH_PATH="${BASE_DIR}/conf/${CONFIG_FILE}"
LOG_DIR="${BASE_DIR}/logs"

if [ ! -d "${LOG_DIR}" ]; then
  mkdir "${LOG_DIR}"
fi
if [ -f "$CONFIG_FILE_WITH_PATH" ]; then
  nohup bash +x "${BASE_DIR}"/bin/audit-proxy agent --conf "${BASE_DIR}"/conf/ -f "${CONFIG_FILE_WITH_PATH}" -n agent1 --no-reload-conf 1>"${LOG_DIR}"/proxy.log 2>"${LOG_DIR}"/proxy-error.log &
else
  error "${CONFIG_FILE_WITH_PATH} is not exist, start failed!" 1
fi
