#!/bin/sh
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

PRG="$0"
PRGDIR=`dirname "$PRG"`
PRG_DIR=`dirname "$PRG"`
GRIFFIN_HOME=`cd "$PRG_DIR/.." >/dev/null; pwd`

mkdir -p $GRIFFIN_HOME/logs
GRIFFIN_LOG_DIR=$GRIFFIN_HOME/logs

#server memory
export JAVA_MEM_OPTS="$JAVA_MEM_OPTS -server -Xms1G -Xmx1G -Xmn512M -XX:PermSize=128M -XX:MaxPermSize=256M -Xss512k -XX:SurvivorRatio=8"

#gc
export JAVA_MEM_OPTS="$JAVA_MEM_OPTS -XX:MaxTenuringThreshold=4 -XX:+UseConcMarkSweepGC -XX:CMSInitiatingOccupancyFraction=70 -XX:+UseCMSCompactAtFullCollection -XX:CMSFullGCsBeforeCompaction=2 -XX:+ExplicitGCInvokesConcurrent  -XX:+CMSScavengeBeforeRemark"

#gc log
export JAVA_MEM_OPTS="$JAVA_MEM_OPTS -Xloggc:$GRIFFIN_LOG_DIR/gc.log.`date +%Y-%m-%d_%H_%M_%S` \
 -XX:+PrintGCDateStamps -XX:+PrintTenuringDistribution -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+PrintGCApplicationStoppedTime -XX:+PrintGCApplicationConcurrentTime -XX:PrintFLSStatistics=1"

#oom
export JAVA_MEM_OPTS="$JAVA_MEM_OPTS -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=$GRIFFIN_LOG_DIR/heapdump.hprof"
