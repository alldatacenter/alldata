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

BASE_DIR=$(dirname $0)/..

source "${BASE_DIR}"/bin/agent-env.sh
CONSOLE_OUTPUT_FILE="${LOG_DIR}/agent-out.log"

function help() {
  echo "Usage: agent.sh {status|start|stop|restart|clean}" >&2
  echo "       status:     the status of inlong agent"
  echo "       start:      start the inlong agent"
  echo "       stop:       stop the inlong agent"
  echo "       restart:    restart the inlong agent"
  echo "       clean:      unregister this node in manager"
  echo "       help:       get help from inlong agent"
}

function running() {
  process=$("$JPS" | grep "AgentMain" | grep -v grep)
  if [ "${process}" = "" ]; then
    return 1;
  else
    return 0;
  fi
}

# start agent
function start_agent() {
  if running; then
    echo "agent is running."
    exit 1
  fi
  nohup ${JAVA} ${AGENT_ARGS} org.apache.inlong.agent.core.AgentMain > /dev/null 2>&1 &
}

# stop agent
function stop_agent() {
  if ! running; then
    echo "agent is not running."
    exit 1
  fi
  count=0
  pid=$("$JPS" | grep "AgentMain" | grep -v grep | awk '{print $1}')
  while running;
  do
    (( count++ ))
    echo "Stopping agent $count times"
    if [ "${count}" -gt 10 ]; then
        echo "kill -9 $pid"
        kill -9 "${pid}"
    else
        kill "${pid}"
    fi
    sleep 6;
  done
  echo "Stop agent successfully."
}

# get status of agent
function status_agent() {
  if running; then
    echo "agent is running."
    exit 0
  else
    echo "agent is not running."
    exit 1
  fi
}

# clean agent
function clean_agent() {
  if running; then
    echo "agent is running. please stop it first."
    exit 0
  fi
  ${JAVA} ${AGENT_ARGS} org.apache.inlong.agent.core.HeartbeatManager
}

function help_agent() {
  ${JAVA} ${AGENT_ARGS} org.apache.inlong.agent.core.AgentMain -h
}

command=$1
shift 1
case $command in
  status)
    status_agent $@;
    ;;
  start)
    start_agent $@;
    ;;
  stop)
    stop_agent $@;
    ;;
  restart)
    $0 stop $@
    $0 start $@
    ;;
  clean)
    clean_agent $@;
    ;;
  help)
    help_agent;
    ;;
  *)
    help;
    exit 1;
    ;;
esac
