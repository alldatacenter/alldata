#!/usr/bin/env sh
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

base_dir=$(dirname $0)

DAEMON_NAME=${DAEMON_NAME:-"tubemq-manager"}
LOG_DIR=${LOG_DIR:-"$base_dir/../logs"}
CONF_DIR=${CONF_DIR:-"$base_dir/../conf"}
LIB_DIR=${LIB_DIR:-"$base_dir/../lib"}
CONSOLE_OUTPUT_FILE=$LOG_DIR/$DAEMON_NAME.out

if [ -z "$TUBE_MANAGER_JVM_HEAP_OPTS" ]; then
  MANAGER_HEAP_OPTS="-Xms512m -Xmx1024m"
else
  MANAGER_HEAP_OPTS="$TUBE_MANAGER_JVM_HEAP_OPTS"
fi
MANAGER_GC_OPTS="-XX:+UseG1GC -verbose:gc -verbose:sizes -Xloggc:${LOG_DIR}/gc.log.`date +%Y-%m-%d-%H-%M-%S` -XX:+PrintGCDetails -XX:+PrintGCDateStamps -XX:+PrintTenuringDistribution"

# create logs directory
if [ ! -d "$LOG_DIR" ]; then
  mkdir -p "$LOG_DIR"
fi

# Exclude jars not necessary for running commands.
regex="(-(test|test-sources|src|scaladoc|javadoc)\.jar|jar.asc)$"
should_include_file() {
  if [ "$INCLUDE_TEST_JARS" = true ]; then
    return 0
  fi
  file=$1
  if [ -z "$(echo "$file" | egrep "$regex")" ] ; then
    return 0
  else
    return 1
  fi
}

for file in ${LIB_DIR}/*.jar;
do
  if should_include_file "$file"; then
    CLASSPATH="$CLASSPATH":"$file"
  fi
done

CLASSPATH="${CONF_DIR}":$CLASSPATH
export MANAGER_JVM_OPTS="${MANAGER_HEAP_OPTS} ${MANAGER_GC_OPTS} -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=${LOG_DIR}"

# Which java to use
if [ -z "$JAVA_HOME" ]; then
  JAVA="java"
else
  JAVA="$JAVA_HOME/bin/java"
fi

nohup "$JAVA" -Dmanager.log.path=${LOG_DIR} $MANAGER_JVM_OPTS -cp "$CLASSPATH" org.apache.inlong.tubemq.manager.TubeMQManager "$@" > "$CONSOLE_OUTPUT_FILE" 2>&1 < /dev/null &