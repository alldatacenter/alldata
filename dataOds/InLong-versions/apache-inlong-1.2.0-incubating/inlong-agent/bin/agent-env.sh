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



#project directory
BASE_DIR=$(cd "$(dirname "$0")"/../;pwd)

AS_USER=`whoami`
export LOG_DIR="$BASE_DIR/logs"

mkdir -p $LOG_DIR
chown -R $AS_USER $LOG_DIR

# find java home
if [ -z "$JAVA_HOME" ]; then
  export JAVA=$(which java)
  export JPS=$(which jps)
else
  export JAVA="$JAVA_HOME/bin/java"
  export JPS="$JAVA_HOME/bin/jps"
fi

if [ -z "$AGENT_JVM_HEAP_OPTS" ]; then
  HEAP_OPTS="-Xms512m -Xmx6656m"
else
  HEAP_OPTS="$AGENT_JVM_HEAP_OPTS"
fi
GC_OPTS="-XX:SurvivorRatio=6 -XX:+UseMembar -XX:+UseConcMarkSweepGC -XX:+CMSParallelRemarkEnabled -XX:+CMSScavengeBeforeRemark -XX:ParallelCMSThreads=3 -XX:+TieredCompilation -XX:+UseCMSCompactAtFullCollection -verbose:gc -Xloggc:$BASE_DIR/logs/gc.log.`date +%Y-%m-%d-%H-%M-%S` -XX:+PrintGCDetails -XX:+PrintGCDateStamps -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=$BASE_DIR/logs/ -XX:+CMSClassUnloadingEnabled -XX:CMSInitiatingOccupancyFraction=60 -XX:CMSFullGCsBeforeCompaction=1 -Dsun.net.inetaddr.ttl=3 -Dsun.net.inetaddr.negative.ttl=1 -Djava.net.preferIPv4Stack=true"
AGENT_JVM_ARGS="$HEAP_OPTS $GC_OPTS"

# Add Agent Rmi Args when necessary
AGENT_RMI_ARGS="-Dcom.sun.management.jmxremote \
-Dcom.sun.management.jmxremote.port=18080 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false"
CONFIG_DIR=${BASE_DIR}"/conf/"
JAR_LIBS=${BASE_DIR}"/lib/*"
CLASSPATH=${CONFIG_DIR}:${JAR_LIBS}

JMX_ENABLED=$(grep -c "agent.prometheus.enable=false" $BASE_DIR/conf/agent.properties)
if [[ $JMX_ENABLED == 1 ]]; then
  export AGENT_ARGS="$AGENT_JVM_ARGS $AGENT_RMI_ARGS -cp $CLASSPATH -Dagent.home=$BASE_DIR"
else
  export AGENT_ARGS="$AGENT_JVM_ARGS -cp $CLASSPATH -Dagent.home=$BASE_DIR"
fi