#!/bin/bash
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# Environment Variables
#
#   DRILL_CONF_DIR   Alternate drill conf dir. Default is ${DRILL_HOME}/conf.
#   DRILL_LOG_DIR    Where log files are stored.  PWD by default.
#   DRILL_PID_DIR    The pid files are stored. /tmp by default.
#   DRILL_IDENT_STRING   A string representing this instance of drillbit. $USER by default
#   DRILL_NICENESS The scheduling priority for daemons. Defaults to 0.
#   DRILL_STOP_TIMEOUT  Time, in seconds, after which we kill -9 the server if it has not stopped.
#                        Default 1200 seconds.
#
# Modelled after $HADOOP_HOME/bin/hadoop-daemon.sh

usage="Usage: drillbit.sh [--config <conf-dir>]\
 (start|stop|restart|autorestart)"

# if no args specified, show usage
if [ $# -lt 1 ]; then
  echo $usage
  exit 1
fi

bin=`dirname "${BASH_SOURCE-$0}"`
bin=`cd "$bin">/dev/null; pwd`

. "$bin"/drill-config.sh

# get arguments
startStop=$1
shift

command=drillbit
shift

waitForProcessEnd() {
  pidKilled=$1
  commandName=$2
  processedAt=`date +%s`
  while kill -0 $pidKilled > /dev/null 2>&1;
   do
     echo -n "."
     sleep 1;
     # if process persists more than $DRILL_STOP_TIMEOUT (default 1200 sec) no mercy
     if [ $(( `date +%s` - $processedAt )) -gt ${DRILL_STOP_TIMEOUT:-1200} ]; then
       break;
     fi
   done
  # process still there : kill -9
  if kill -0 $pidKilled > /dev/null 2>&1; then
    echo -n force stopping $commandName with kill -9 $pidKilled
    $JAVA_HOME/bin/jstack -l $pidKilled > "$logout" 2>&1
    kill -9 $pidKilled > /dev/null 2>&1
  fi
  # Add a CR after we're done w/ dots.
  echo
}

drill_rotate_log ()
{
    log=$1;
    num=5;
    if [ -n "$2" ]; then
    num=$2
    fi
    if [ -f "$log" ]; then # rotate logs
    while [ $num -gt 1 ]; do
        prev=`expr $num - 1`
        [ -f "$log.$prev" ] && mv -f "$log.$prev" "$log.$num"
        num=$prev
    done
    mv -f "$log" "$log.$num";
    fi
}

check_before_start(){
    #ckeck if the process is not running
    mkdir -p "$DRILL_PID_DIR"
    if [ -f $pid ]; then
      if kill -0 `cat $pid` > /dev/null 2>&1; then
        echo $command running as process `cat $pid`.  Stop it first.
        exit 1
      fi
    fi
}

wait_until_done ()
{
    p=$1
    cnt=${DRILLBIT_TIMEOUT:-300}
    origcnt=$cnt
    while kill -0 $p > /dev/null 2>&1; do
      if [ $cnt -gt 1 ]; then
        cnt=`expr $cnt - 1`
        sleep 1
      else
        echo "Process did not complete after $origcnt seconds, killing."
        kill -9 $p
        exit 1
      fi
    done
    return 0
}

# get log directory
if [ "$DRILL_LOG_DIR" = "" ]; then
  export DRILL_LOG_DIR=/var/log/drill
fi
mkdir -p "$DRILL_LOG_DIR"

if [ "$DRILL_PID_DIR" = "" ]; then
  DRILL_PID_DIR=$DRILL_HOME
fi

# Some variables
# Work out java location so can print version into log.
if [ "$JAVA_HOME" != "" ]; then
  #echo "run java in $JAVA_HOME"
  JAVA_HOME=$JAVA_HOME
fi
if [ "$JAVA_HOME" = "" ]; then
  echo "Error: JAVA_HOME is not set."
  exit 1
fi

JAVA=$JAVA_HOME/bin/java
export DRILL_LOG_PREFIX=drillbit
export DRILL_LOGFILE=$DRILL_LOG_PREFIX.log
export DRILL_OUTFILE=$DRILL_LOG_PREFIX.out
loggc=$DRILL_LOG_DIR/$DRILL_LOG_PREFIX.gc
loglog="${DRILL_LOG_DIR}/${DRILL_LOGFILE}"
logout="${DRILL_LOG_DIR}/${DRILL_OUTFILE}"
pid=$DRILL_PID_DIR/drillbit.pid

DRILL_JAVA_OPTS="$DRILL_JAVA_OPTS -Dlog.path=$loglog"

if [ -n "$SERVER_GC_OPTS" ]; then
  export SERVER_GC_OPTS=${SERVER_GC_OPTS/"-Xloggc:<FILE-PATH>"/"-Xloggc:${loggc}"}
fi
if [ -n "$CLIENT_GC_OPTS" ]; then
  export CLIENT_GC_OPTS=${CLIENT_GC_OPTS/"-Xloggc:<FILE-PATH>"/"-Xloggc:${loggc}"}
fi

# Set default scheduling priority
if [ "$DRILL_NICENESS" = "" ]; then
    export DRILL_NICENESS=0
fi

thiscmd=$0
args=$@

case $startStop in

(start)
    check_before_start
    echo starting $command, logging to $logout
    nohup $thiscmd internal_start $command $args < /dev/null >> ${logout} 2>&1  &
    sleep 1;
  ;;

(internal_start)
    drill_rotate_log $loggc
    # Add to the command log file vital stats on our environment.
    echo "`date` Starting $command on `hostname`" >> $loglog
    echo "`ulimit -a`" >> $loglog 2>&1
    nice -n $DRILL_NICENESS "$DRILL_HOME"/bin/runbit \
        $command "$@" start >> "$logout" 2>&1 &
    echo $! > $pid
    wait
  ;;

(stop)
    rm -f "$DRILL_START_FILE"
    if [ -f $pid ]; then
      pidToKill=`cat $pid`
      # kill -0 == see if the PID exists
      if kill -0 $pidToKill > /dev/null 2>&1; then
        echo stopping $command
        echo "`date` Terminating $command" pid $pidToKill>> $loglog
        kill $pidToKill > /dev/null 2>&1
        waitForProcessEnd $pidToKill $command
        rm $pid
      else
        retval=$?
        echo no $command to stop because kill -0 of pid $pidToKill failed with status $retval
      fi
    else
      echo no $command to stop because no pid file $pid
    fi
  ;;

(restart)
    # stop the command
    $thiscmd --config "${DRILL_CONF_DIR}" stop $command $args &
    wait_until_done $!
    # wait a user-specified sleep period
    sp=${DRILL_RESTART_SLEEP:-3}
    if [ $sp -gt 0 ]; then
      sleep $sp
    fi
    # start the command
    $thiscmd --config "${DRILL_CONF_DIR}" start $command $args &
    wait_until_done $!
  ;;

(*)
  echo $usage
  exit 1
  ;;
esac
