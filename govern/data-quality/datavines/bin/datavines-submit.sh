#!/bin/sh
#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

usage="Usage: datavines-submit.sh <command> "

# if no args specified, show usage
if [ $# -le 1 ]; then
  echo $usage
  exit 1
fi

filePath=$1

echo "Begin Execute......"

BIN_DIR=`dirname $0`
BIN_DIR=`cd "$BIN_DIR"; pwd`
DATAVINES_HOME=$BIN_DIR/..

source /etc/profile

export JAVA_HOME=$JAVA_HOME
export DATAVINES_LIB_JARS=$DATAVINES_HOME/libs/
export DATAVINES_OPTS="-server -Xmx16g -Xms1g -Xss512k -XX:+DisableExplicitGC -XX:+UseConcMarkSweepGC -XX:+CMSParallelRemarkEnabled -XX:LargePageSizeInBytes=128m -XX:+UseFastAccessorMethods -XX:+UseCMSInitiatingOccupancyOnly -XX:CMSInitiatingOccupancyFraction=70"

cd $DATAVINES_HOME

if [ $filePath ]; then
  $JAVA_HOME/bin/java $DATAVINES_OPTS -Djava.ext.dirs=$JAVA_HOME/jre/lib/ext:$DATAVINES_LIB_JARS -classpath $DATAVINES_LIB_JARS io.datavines.runner.JobExecuteBootstrap $filePath
else
  echo "Error: No filePath was found."
  exit 1
fi