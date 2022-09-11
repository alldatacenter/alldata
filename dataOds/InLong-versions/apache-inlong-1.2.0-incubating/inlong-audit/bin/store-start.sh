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
base_dir=$(
  cd $(dirname $0)
  cd ..
  pwd
)

LOG_PATH="${base_dir}/logs"

PID=$(ps -ef | grep "audit-store" | grep -v grep | awk '{ print $2}')
LOG_DIR="${base_dir}/logs"

if [ -n "$PID" ]; then
 echo "Application has already started."
 exit 0
fi

if [[ -z $JAVA_HOME ]]; then
    JAVA=$(which java)
    if [ $? != 0 ]; then
        echo "Error: JAVA_HOME not set, and no java executable found in $PATH." 1>&2
        exit 1
    fi
else
    JAVA=$JAVA_HOME/bin/java
fi

if [ ! -d "${LOG_DIR}" ]; then
  mkdir ${LOG_DIR}
fi

JAVA_OPTS="-server -XX:SurvivorRatio=2 -XX:+UseParallelGC"

if [ -z "$AUDIT_JVM_HEAP_OPTS" ]; then
  HEAP_OPTS="-Xms512m -Xmx1024m"
else
  HEAP_OPTS="$AUDIT_JVM_HEAP_OPTS"
fi
JAVA_OPTS="${JAVA_OPTS} ${HEAP_OPTS}"

SERVERJAR=`ls -lt ${base_dir}/lib |grep audit-store | head -2 | tail -1 | awk '{print $NF}'`

nohup $JAVA $JAVA_OPTS -Daudit.log.path=$LOG_PATH -Dloader.path="$base_dir/conf,$base_dir/lib/" -jar "$base_dir/lib/$SERVERJAR"  1>${LOG_DIR}/store.log 2>${LOG_DIR}/store-error.log &

PIDFILE="$base_dir/bin/PID"

PID=$(ps -ef | grep "$base_dir" | grep -v grep | awk '{ print $2}')

sleep 3

if [ -n "$PID" ]; then
  echo -n $PID > "$PIDFILE"
  echo "Application started."
  exit 0
else
  echo "Application start failed."
  exit 0
fi