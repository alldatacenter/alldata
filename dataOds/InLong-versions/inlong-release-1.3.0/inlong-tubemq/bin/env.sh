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

#Config your java home
#JAVA_HOME=/opt/jdk/

if [ -z "$JAVA_HOME" ]; then
  export JAVA=`which java`
else
  export JAVA="$JAVA_HOME/bin/java"
fi

tubemq_home=$BASE_DIR
export CLASSPATH=$CLASSPATH:$BASE_DIR/conf:$(ls $BASE_DIR/lib/*.jar | tr '\n' :)

#Master jvm args
if [ -z "$MASTER_JVM_SIZE" ]; then
  MASTER_JVM_SIZE="-Xms15g -Xmx15g -Xmn5g"
fi
MASTER_JVM_ARGS="$MASTER_JVM_SIZE -XX:SurvivorRatio=6 -XX:+UseMembar -XX:+UseConcMarkSweepGC -XX:+CMSParallelRemarkEnabled -XX:+CMSScavengeBeforeRemark -XX:ParallelCMSThreads=12 -XX:+UseCMSCompactAtFullCollection -verbose:gc -Xloggc:$BASE_DIR/logs/gc.log.`date +%Y-%m-%d-%H-%M-%S` -XX:+PrintGCDetails -XX:+PrintGCDateStamps -XX:+CMSClassUnloadingEnabled -XX:CMSInitiatingOccupancyFraction=70 -XX:+UseGCLogFileRotation -XX:NumberOfGCLogFiles=3 -XX:GCLogFileSize=512M -Dsun.net.inetaddr.ttl=3 -Dsun.net.inetaddr.negative.ttl=1 -server -Dtubemq.home=$tubemq_home -cp $CLASSPATH "
#Broker jvm args
if [ -z "$BROKER_JVM_SIZE" ]; then
  BROKER_JVM_SIZE="-Xms15g -Xmx15g -Xmn5g"
fi
BROKER_JVM_ARGS="$BROKER_JVM_SIZE -XX:SurvivorRatio=6 -XX:MaxDirectMemorySize=10g -XX:+UseMembar -XX:+UseConcMarkSweepGC -XX:+CMSParallelRemarkEnabled -XX:+CMSScavengeBeforeRemark -XX:ParallelCMSThreads=12 -XX:+UseCMSCompactAtFullCollection -verbose:gc -Xloggc:$BASE_DIR/logs/gc.log.`date +%Y-%m-%d-%H-%M-%S` -XX:+PrintGCDetails -XX:+PrintGCDateStamps -XX:+CMSClassUnloadingEnabled -XX:CMSInitiatingOccupancyFraction=70 -XX:+UseGCLogFileRotation -XX:NumberOfGCLogFiles=3 -XX:GCLogFileSize=512M -Dsun.net.inetaddr.ttl=3 -Dsun.net.inetaddr.negative.ttl=1 -Dtube.fast_boot=false -server -Dtubemq.home=$tubemq_home -cp $CLASSPATH "
#Tools jvm args,you don't have to modify this at all.
TOOLS_JVM_ARGS="-Xmx512m -Xms512m -Dtubemq.home=$tubemq_home -cp $CLASSPATH "
#Tool repair jvm args
TOOL_REPAIR_JVM_ARGS="-Xmx24g -Xms8g -Dtubemq.home=$tubemq_home -cp $CLASSPATH "

if [ -z "$MASTER_ARGS" ]; then
  export MASTER_ARGS="$MASTER_JVM_ARGS -Dtubemq.log.prefix=master -Dtubemq.log.path=$LOG_DIR -Dlog4j.configurationFile=${BASE_DIR}/conf/log4j2.xml"
fi

if [ -z "$BROKER_ARGS" ]; then
  export BROKER_ARGS="$BROKER_JVM_ARGS -Dtubemq.log.prefix=broker -Dtubemq.log.path=$LOG_DIR -Dlog4j.configurationFile=${BASE_DIR}/conf/log4j2.xml"
fi

if [ -z "$TOOLS_ARGS" ]; then
  export TOOLS_ARGS="$TOOLS_JVM_ARGS -Dtubemq.log.prefix=tools -Dtubemq.log.path=$LOG_DIR -Dlog4j.configurationFile=${BASE_DIR}/conf/log4j2.xml"
fi

if [ -z "$TOOL_REPAIR_ARGS" ]; then
  export TOOL_REPAIR_ARGS="$TOOL_REPAIR_JVM_ARGS -Dtubemq.log.prefix=tools -Dtubemq.log.path=$LOG_DIR -Dlog4j.configurationFile=${BASE_DIR}/conf/log4j2.xml"
fi





