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

set -o pipefail
set -o nounset   # exit the script if you try to use an uninitialised variable
set -o errexit   # exit the script if any statement returns a non-true return value

source "$(dirname "$0")/utils.sh"
load_rss_env

cd "$RSS_HOME"

COORDINATOR_CONF_FILE="${RSS_CONF_DIR}/coordinator.conf"
JAR_DIR="${RSS_HOME}/jars"
LOG_CONF_FILE="${RSS_CONF_DIR}/log4j.properties"
LOG_PATH="${RSS_LOG_DIR}/coordinator.log"
OUT_PATH="${RSS_LOG_DIR}/coordinator.out"

MAIN_CLASS="org.apache.uniffle.coordinator.CoordinatorServer"

HADOOP_DEPENDENCY="$("$HADOOP_HOME/bin/hadoop" classpath --glob)"

echo "Check process existence"
is_jvm_process_running "$JPS" $MAIN_CLASS

CLASSPATH=""

for file in $(ls ${JAR_DIR}/coordinator/*.jar 2>/dev/null); do
  CLASSPATH=$CLASSPATH:$file
done

mkdir -p "${RSS_LOG_DIR}"
mkdir -p "${RSS_PID_DIR}"

CLASSPATH=$CLASSPATH:$HADOOP_CONF_DIR:$HADOOP_DEPENDENCY
JAVA_LIB_PATH="-Djava.library.path=$HADOOP_HOME/lib/native"

echo "class path is $CLASSPATH"

JVM_ARGS=" -server \
          -Xmx8g \
          -XX:+UseG1GC \
          -XX:MaxGCPauseMillis=200 \
          -XX:ParallelGCThreads=20 \
          -XX:ConcGCThreads=5 \
          -XX:InitiatingHeapOccupancyPercent=45 "

ARGS=""

if [ -f ${LOG_CONF_FILE} ]; then
  ARGS="$ARGS -Dlog4j.configuration=file:${LOG_CONF_FILE} -Dlog.path=${LOG_PATH}"
else
  echo "Exit with error: ${LOG_CONF_FILE} file doesn't exist."
  exit 1
fi

$RUNNER $ARGS $JVM_ARGS -cp $CLASSPATH $MAIN_CLASS --conf "$COORDINATOR_CONF_FILE" $@ &> $OUT_PATH &

get_pid_file_name coordinator
echo $! >${RSS_PID_DIR}/${pid_file}
