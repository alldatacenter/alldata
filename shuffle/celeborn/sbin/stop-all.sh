#!/usr/bin/env bash
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

# Starts the celeborn master and workers on the machine this script is executed on.

if [ -z "${CELEBORN_HOME}" ]; then
  export CELEBORN_HOME="$(cd "`dirname "$0"`"/..; pwd)"
fi

. "${CELEBORN_HOME}/sbin/load-celeborn-env.sh"

if [ -f "${CELEBORN_CONF_DIR}/hosts" ]; then
  HOST_LIST=$(awk '/\[/{prefix=$0; next} $1{print prefix,$0}' "${CELEBORN_CONF_DIR}/hosts")
else
  HOST_LIST="[master] localhost\n[worker] localhost"
fi

# By default disable strict host key checking
if [ "$CELEBORN_SSH_OPTS" = "" ]; then
  CELEBORN_SSH_OPTS="-o StrictHostKeyChecking=no"
fi

# stop masters
for host in `echo "$HOST_LIST" | sed  "s/#.*$//;/^$/d" | grep '\[master\]' | awk '{print $NF}'`
do
  if [ -n "${CELEBORN_SSH_FOREGROUND}" ]; then
    ssh $CELEBORN_SSH_OPTS "$host" "${CELEBORN_HOME}/sbin/stop-master.sh"
  else
    ssh $CELEBORN_SSH_OPTS "$host" "${CELEBORN_HOME}/sbin/stop-master.sh" &
  fi
  if [ "$CELEBORN_SLEEP" != "" ]; then
    sleep $CELEBORN_SLEEP
  fi
done

# stop workers
for host in `echo "$HOST_LIST"| sed  "s/#.*$//;/^$/d" | grep '\[worker\]' | awk '{print $NF}'`
do
  if [ -n "${CELEBORN_SSH_FOREGROUND}" ]; then
    ssh $CELEBORN_SSH_OPTS "$host" "${CELEBORN_HOME}/sbin/stop-worker.sh"
  else
    ssh $CELEBORN_SSH_OPTS "$host" "${CELEBORN_HOME}/sbin/stop-worker.sh" &
  fi
  if [ "$CELEBORN_SLEEP" != "" ]; then
    sleep $CELEBORN_SLEEP
  fi
done

wait
