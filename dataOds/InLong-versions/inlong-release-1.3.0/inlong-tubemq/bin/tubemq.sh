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

# project directory
if [ -z "$BASE_DIR" ] ; then
  PRG="$0"

  # need this for relative symlinks
  while [ -h "$PRG" ] ; do
    ls=`ls -ld "$PRG"`
    link=`expr "$ls" : '.*-> \(.*\)$'`
    if expr "$link" : '/.*' > /dev/null; then
      PRG="$link"
    else
      PRG="`dirname "$PRG"`/$link"
    fi
  done
  BASE_DIR=`dirname "$PRG"`/..

  # make it fully qualified
  BASE_DIR=`cd "$BASE_DIR" && pwd`
fi

# load environmental variables
source $BASE_DIR/bin/env.sh

AS_USER=`whoami`
LOG_DIR="$BASE_DIR/logs"
PID_DIR="$BASE_DIR/logs"

# check service running status
function running(){
  if [ -f "$PID_FILE" ]; then
    pid=$(cat "$PID_FILE")
    process=`ps aux | grep " $pid "|grep "\-Dtubemq\.home=$BASE_DIR" | grep -v grep`;
    if [ "$process" == "" ]; then
      return 1;
    else
      return 0;
    fi
  else
    return 1
  fi
}

# start the specified service
function start_server() {
  if running; then
    echo "TubeMQ $SERVICE is running."
    exit 1
  fi

  mkdir -p $PID_DIR
  touch $LOG_FILE
  mkdir -p $LOG_DIR
  chown -R $AS_USER $PID_DIR
  chown -R $AS_USER $LOG_DIR

  config_files="-f $BASE_DIR/conf/$SERVICE.ini"

  echo "Starting TubeMQ $SERVICE server..."
  pushd .
    cd $BASE_DIR
    #echo "$JAVA $SERVICE_ARGS $SERVICE_CLASS $config_files"
    sleep 1
    nohup $JAVA $SERVICE_ARGS $SERVICE_CLASS $config_files 2>&1 >>$LOG_FILE &
    echo $! > $PID_FILE
    chmod 755 $PID_FILE
  popd
}

# status of the specified service
function status_server() {
  if running; then
    echo "TubeMQ $SERVICE is running."
    exit 0
  else
    echo "TubeMQ $SERVICE is not running."
    exit 1
  fi
}

# stop the specified service
function stop_server() {
  if ! running; then
    echo "TubeMQ $SERVICE is not running."
    exit 1
  fi
  count=0
  pid=$(cat $PID_FILE)
  while running;
  do
    let count=$count+1
    echo "Stopping TubeMQ $SERVICE $count times"
    if [ $count -gt 10 ]; then
      echo "kill -9 $pid"
      kill -9 $pid
    else
      kill $pid
    fi
    sleep 6;
  done
  echo "Stop TubeMQ $SERVICE successfully."
  rm $PID_FILE
}

# display usage
function help() {
  echo "Usage: tubemq {master|broker} {status|start|stop|restart}" >&2
  echo "       status:      start the master/broker server"
  echo "       start:      start the master/broker server"
  echo "       stop:       stop the master/broker server"
  echo "       restart:    restart the master/broker server"
}

# if less than two arguments supplied
if [ $# -lt 2 ]; then
  help;
  exit 1;
fi

SERVICE=$1
COMMAND=$2
shift 2

case $SERVICE in
  master)
    SERVICE_CLASS="org.apache.inlong.tubemq.server.tools.MasterStartup"
    SERVICE_ARGS=$MASTER_ARGS
    ;;
  broker)
    SERVICE_CLASS="org.apache.inlong.tubemq.server.tools.BrokerStartup"
    SERVICE_ARGS=$BROKER_ARGS
    ;;
  *)
    help;
    exit 1;
    ;;
esac

LOG_FILE="$LOG_DIR/$SERVICE.log"
PID_FILE="$PID_DIR/.$SERVICE.run.pid"

case $COMMAND in
  status)
    status_server $@;
    ;;
  start)
    start_server $@;
    ;;
  stop)
    stop_server $@;
    ;;
  restart)
    $0 $SERVICE stop $@
    $0 $SERVICE start $@
    ;;
  *)
    help;
    exit 1;
    ;;
esac
