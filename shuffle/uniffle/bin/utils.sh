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

# Common utils
set -o nounset   # exit the script if you try to use an uninitialised variable
set -o errexit   # exit the script if any statement returns a non-true return value

#---
# is_process_running: Checks if a process is running
# args:               Process ID of running proccess
# returns:            returns 0 if process is running, 1 if not found
#---
function is_process_running {
  local  pid=$1
  kill -0 $pid > /dev/null 2>&1 #exit code ($?) is 0 if pid is running, 1 if not running
  local  status=$?              #because we are returning exit code, can use with if & no [ bracket
  return $status
}

#---
# args:               Process name of a running process to shutdown, install directory
# returns:            returns 0 if success, 1 otherwise
#---
function common_shutdown {
  process_name="$1"
  install_dir="$2"
  max_attempt=30
  get_pid_file_name ${process_name}
  pid_file_name="${pid_file}"
  pid=`cat ${install_dir}/${pid_file_name}`

  kill_process_with_retry "${pid}" "${process_name}" "${max_attempt}"

  if [[ $? == 0 ]]; then
    rm -f ${install_dir}/${pid_file_name}
    return 0
  else
    return 1
  fi
}

#---
# args:               Process name, coordinator or shuffle-server
#---
function get_pid_file_name {
  process_name="$1"
  if [[ $process_name == "coordinator" ]]; then
    pid_file="coordinator.pid"
  elif [[ $process_name == "shuffle-server" ]]; then
    pid_file="shuffle-server.pid"
  else
    echo "Invalid process name: $process_name"
    exit 1
  fi
}

#---
# kill_process_with_retry: Checks and attempts to kill the running process
# args:                    PID, process name, number of kill attempts
# returns:                 returns 0 if kill succeds or nothing to kill, 1 if kill fails
# exception:               If passed a non-existant pid, function will forcefully exit
#---
function kill_process_with_retry {
   local pid="$1"
   local pname="$2"
   local maxattempt="$3"
   local sleeptime=3

   if ! is_process_running $pid ; then
     echo "ERROR: process name ${pname} with pid: ${pid} not found"
     exit 1
   fi

   for try in $(seq 1 $maxattempt); do
      echo "Killing $pname. [pid: $pid], attempt: $try"
      if is_process_running $pid; then
        kill ${pid}
      fi
      sleep $sleeptime
      if is_process_running $pid; then
        echo "$pname is not dead [pid: $pid]"
        echo "sleeping for $sleeptime seconds before retry"
        sleep $sleeptime
      else
        echo "shutdown succeeded"
        return 0
      fi
   done

   echo "Error: unable to kill process for $maxattempt attempt(s), killing the process with -9"
   kill -9 $pid
   sleep $sleeptime

   if is_process_running $pid; then
      echo "$pname is not dead even after kill -9 [pid: $pid]"
      return 1
   else
    echo "shutdown succeeded"
    return 0
   fi
}

#---
# is_jvm_process_running: Checks if a jvm process is running
# args: jps path, main class of the jvm process
# exit 1 if process is running
#---
function is_jvm_process_running {
  local cmd=$1
  local main_class=$2
  local tmp=$($cmd -l 2>/dev/null | grep "${main_class}" | cut -d' ' -f2)

  if [[ "$tmp" == "${main_class}" ]]; then
    echo "$main_class already exist"
    exit 1
  fi

}

function is_port_in_use {
  local port=$1
  local tmp=$(lsof -i:$port | grep LISTEN)
  if [[ "$tmp" != "" ]]; then
    echo "port[$port] is already in use"
    exit 1
  fi
}
#---
# load_rss_env: Export RSS environment variables
#---
function load_rss_env {
  set -o allexport

  # find rss-env.sh
  set +o nounset
  if [ -f "${RSS_CONF_DIR}/rss-env.sh" ]; then
     RSS_ENV_SH="${RSS_CONF_DIR}/rss-env.sh"
  elif [ -f "${RSS_HOME}/bin/rss-env.sh" ]; then
     RSS_ENV_SH="${RSS_HOME}/bin/rss-env.sh"
  else
     RSS_ENV_SH="$(cd "$(dirname "$0")"; pwd)/rss-env.sh"
  fi
  set -o nounset
  echo "Using rss_env.sh: ${RSS_ENV_SH}"

  # export rss-env.sh
  source "${RSS_ENV_SH}"

  # check
  if [ -z "$JAVA_HOME" ]; then
    echo "No env JAVA_HOME."
    exit 1
  fi
  if [ -z "$HADOOP_HOME" ]; then
    echo "No env HADOOP_HOME."
    exit 1
  fi

  # export default value
  set +o nounset
  if [ -z "$RSS_HOME" ]; then
    RSS_HOME="$(cd "$(dirname "$0")/.."; pwd)"
  fi
  if [ -z "$RSS_CONF_DIR" ]; then
    RSS_CONF_DIR="${RSS_HOME}/conf"
  fi
  if [ -z "$HADOOP_CONF_DIR" ]; then
    HADOOP_CONF_DIR="${HADOOP_HOME}/etc/hadoop"
  fi
  if [ -z "$RSS_LOG_DIR" ]; then
    RSS_LOG_DIR="${RSS_HOME}/logs"
  fi
  if [ -z "$RSS_PID_DIR" ]; then
    RSS_PID_DIR="${RSS_HOME}"
  fi
  set -o nounset

  RUNNER="${JAVA_HOME}/bin/java"
  JPS="${JAVA_HOME}/bin/jps"

  # print
  echo "Using Java from ${JAVA_HOME}"
  echo "Using Hadoop from ${HADOOP_HOME}"
  echo "Using RSS from ${RSS_HOME}"
  echo "Using RSS conf from ${RSS_CONF_DIR}"
  echo "Using Hadoop conf from ${HADOOP_CONF_DIR}"
  echo "Write log file to ${RSS_LOG_DIR}"
  echo "Write pid file to ${RSS_PID_DIR}"

  set +o allexport
}

