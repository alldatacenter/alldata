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
#   DRILL_CONF_DIR      Alternate drill conf dir. Default is ${DRILL_HOME}/conf.
#   DRILL_LOG_DIR       Where log files are stored. Default is /var/log/drill if
#                       that exists, else $DRILL_HOME/log
#   DRILL_PID_DIR       The pid files are stored. $DRILL_HOME by default.
#   DRILL_IDENT_STRING  A string representing this instance of drillbit.
#                       $USER by default
#   DRILL_NICENESS      The scheduling priority for daemons. Defaults to 0.
#   DRILL_STOP_TIMEOUT  Time, in seconds, after which we kill -9 the server if
#                       it has not stopped.
#                       Default 120 seconds.
#   SERVER_LOG_GC       Set to "1" to enable Java garbage collector logging.
#
# See also the environment variables defined in drill-config.sh
# and runbit. Most of the above can be set in drill-env.sh for
# each site.
#
# Modeled after $HADOOP_HOME/bin/hadoop-daemon.sh
#
# Usage:
#
# drillbit.sh [--config conf-dir] cmd [arg1 arg2 ...]
#
# The configuration directory, if provided, must exist and contain a Drill
# configuration file. The option takes precedence over the
# DRILL_CONF_DIR environment variable.
#
# The command is one of: start|stop|status|restart|run|graceful_stop
#
# Additional arguments are passed as JVM options to the Drill-bit.
# They typically are of the form:
#
# -Dconfig-var=value
#
# Where config-var is a fully expanded form of a configuration variable.
# The value overrides any value in the user or Drill configuration files.

usage="Usage: drillbit.sh [--config|--site <site-dir>]\
 (start|stop|status|restart|run|graceful_stop) [args]"

bin=`dirname "${BASH_SOURCE-$0}"`
bin=`cd -P "$bin">/dev/null; pwd`

base=`basename "${BASH_SOURCE-$0}"`
command=${base/.*/}

# Environment variable to indicate Drillbit is being setup. Later drill-env.sh
# and distrib-env.sh can consume this to set common environment variable differently
# for Drillbit and Sqlline.
export DRILLBIT_CONTEXT=1

# Setup environment. This parses, and removes, the
# options --config conf-dir parameters.

. "$bin/drill-config.sh"

# if no args specified, show usage
if [ ${#args[@]} = 0 ]; then
  echo $usage
  exit 1
fi

# Get command. all other args are JVM args, typically properties.
startStopStatus="${args[0]}"
args[0]=''
export args

# Set default scheduling priority
DRILL_NICENESS=${DRILL_NICENESS:-0}
GRACEFUL_FILE=$DRILL_HOME/$GRACEFUL_SIGFILE

waitForProcessEnd()
{
  pidKilled=$1
  commandName=$2
  kill_drillbit=$3
  processedAt=`date +%s`
  triggered_shutdown=false
  origcnt=${DRILL_STOP_TIMEOUT:-120}
  while kill -0 $pidKilled > /dev/null 2>&1;
   do
     echo -n "."
     sleep 1;
     #Incase of graceful shutdown, create graceful file and wait till the process ends.
     if [ "$kill_drillbit" = false ]; then
       if [ "$triggered_shutdown" = false ]; then
         touch $GRACEFUL_FILE
         triggered_shutdown=true
       fi
     fi
     if [ "$kill_drillbit" = true ] ; then
        # if process persists more than $DRILL_STOP_TIMEOUT (default 120 sec) no mercy
        if [ $(( `date +%s` - $processedAt )) -gt $origcnt ]; then
          break;
        fi
     fi
  done
  echo
  # process still there : kill -9
  if kill -0 $pidKilled > /dev/null 2>&1; then
    echo "$commandName did not complete after $origcnt seconds, killing with kill -9 $pidKilled"
    `dirname $JAVA`/jstack -l $pidKilled > "$logout" 2>&1
    kill -9 $pidKilled > /dev/null 2>&1
  fi
}

check_before_start()
{
  #check that the process is not running
  mkdir -p "$DRILL_PID_DIR"
  if [ -f $pidFile ]; then
    if kill -0 `cat $pidFile` > /dev/null 2>&1; then
      echo "$command is already running as process `cat $pidFile`.  Stop it first."
      exit 1
    fi
  fi
   #remove any previous uncleaned graceful file
  if [ -f "$GRACEFUL_FILE" ]; then
    rm $GRACEFUL_FILE
    rm_status=$?
    if [ $rm_status -ne 0 ];then
        echo "Error: Failed to remove $GRACEFUL_FILE!"
        exit $rm_status
    fi
  fi
}

check_after_start(){
    dbitPid=$1;
    # Check and enforce for CGroup
    if [ -n "$DRILLBIT_CGROUP" ]; then
      check_and_enforce_cgroup $dbitPid
    fi
}

check_and_enforce_cgroup(){
    dbitPid=$1;
    kill -0 $dbitPid
    if [ $? -gt 0 ]; then 
      echo "ERROR: Failed to add Drillbit to CGroup ( $DRILLBIT_CGROUP ) for 'cpu'. Ensure that the Drillbit ( pid=$dbitPid ) started up." >&2
      exit 1
    fi
    SYS_CGROUP_DIR=${SYS_CGROUP_DIR:-"/sys/fs/cgroup"}
    if [ -f $SYS_CGROUP_DIR/cpu/$DRILLBIT_CGROUP/cgroup.procs ]; then
      echo $dbitPid > $SYS_CGROUP_DIR/cpu/$DRILLBIT_CGROUP/cgroup.procs
      # Verify Enforcement
      cgroupStatus=`grep -w $dbitPid $SYS_CGROUP_DIR/cpu/${DRILLBIT_CGROUP}/cgroup.procs`
      if [ -n "$cgroupStatus" ]; then
        #Ref: https://www.kernel.org/doc/Documentation/scheduler/sched-bwc.txt
        cpu_quota=`cat ${SYS_CGROUP_DIR}/cpu/${DRILLBIT_CGROUP}/cpu.cfs_quota_us`
        cpu_period=`cat ${SYS_CGROUP_DIR}/cpu/${DRILLBIT_CGROUP}/cpu.cfs_period_us`
        if [ $cpu_period -gt 0 ] && [ $cpu_quota -gt 0 ]; then
          coresAllowed=`echo $(( 100 * $cpu_quota / $cpu_period )) | sed 's/..$/.&/'`
          echo "INFO: CGroup (drillcpu) will limit Drill to $coresAllowed cpu(s)"
        fi
      else
        echo "ERROR: Failed to add Drillbit to CGroup ( $DRILLBIT_CGROUP ) for 'cpu'. Ensure that the cgroup manages 'cpu'" >&2
      fi
    else
      echo "ERROR: CGroup $DRILLBIT_CGROUP not found. Ensure that daemon is running, SYS_CGROUP_DIR is correctly set (currently, $SYS_CGROUP_DIR ), and that the CGroup exists" >&2
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

start_bit ( )
{
  check_before_start
  echo "Starting $command, logging to $logout"
  echo "`date` Starting $command on `hostname`" >> "$DRILLBIT_LOG_PATH"
  echo "`ulimit -a`" >> "$DRILLBIT_LOG_PATH" 2>&1
  nohup nice -n $DRILL_NICENESS "$DRILL_HOME/bin/runbit" exec ${args[@]} >> "$logout" 2>&1 &
  procId=$!
  echo $procId > $pidFile # Yeah, $pidFile is a file, $procId is the pid...
  echo $! > $pidFile
  sleep 1
  check_after_start $procId
}

stop_bit ( )
{
  kill_drillbit=$1
  if [ -f $pidFile ]; then
    pidToKill=`cat $pidFile`
    # kill -0 == see if the PID exists
    if kill -0 $pidToKill > /dev/null 2>&1; then
      echo "Stopping $command"
      echo "`date` Terminating $command pid $pidToKill" >> "$DRILLBIT_LOG_PATH"
      if [ $kill_drillbit = true ]; then
        kill $pidToKill > /dev/null 2>&1
      fi
      waitForProcessEnd $pidToKill $command $kill_drillbit
      retval=0
    else
      retval=$?
      echo "No $command to stop because kill -0 of pid $pidToKill failed with status $retval"
    fi
    rm $pidFile > /dev/null 2>&1
  else
    echo "No $command to stop because no pid file $pidFile"
    retval=1
  fi
  return $retval
}

pidFile=$DRILL_PID_DIR/drillbit.pid
logout="${DRILL_LOG_PREFIX}.out"

thiscmd=$0

case $startStopStatus in

(start)
  start_bit
  ;;

(run)
  # Launch Drill as a child process. Does not redirect stderr or stdout.
  # Does not capture the Drillbit pid.
  # Use this when launching Drill from your own script that manages the
  # process, such as (roll-your-own) YARN, Mesos, supervisord, etc.

  echo "`date` Starting $command on `hostname`"
  echo "`ulimit -a`"
  $DRILL_HOME/bin/runbit exec ${args[@]}
  ;;

(stop)
  kill_drillbit=true
  stop_bit $kill_drillbit
  exit $?
  ;;

# Shutdown the drillbit gracefully without disrupting the in-flight queries.
# In this case, if there are any long running queries the drillbit will take a
# little longer to shutdown. Incase if the user wishes to shutdown immediately
# they can issue stop instead of graceful_stop.
(graceful_stop)
  kill_drillbit=false
  stop_bit $kill_drillbit
  exit $?
  ;;

(restart)
  # stop the command
  kill_drillbit=true
  stop_bit $kill_drillbit
  # wait a user-specified sleep period
  sp=${DRILL_RESTART_SLEEP:-3}
  if [ $sp -gt 0 ]; then
    sleep $sp
  fi
  # start the command
  start_bit
  ;;

(status)
  if [ -f $pidFile ]; then
    TARGET_PID=`cat $pidFile`
    if kill -0 $TARGET_PID > /dev/null 2>&1; then
      echo "$command is running."
    else
      echo "$pidFile file is present but $command is not running."
      exit 1
    fi
  else
    echo "$command is not running."
    exit 1
  fi
  ;;

(debug)
  # Undocumented command to print out environment and Drillbit
  # command line after all adjustments.

  echo "command: $command"
  echo "args: ${args[@]}"
  echo "cwd:" `pwd`
  # Print Drill command line
  "$DRILL_HOME/bin/runbit" debug ${args[@]}
  ;;

(*)
  echo $usage
  exit 1
  ;;
esac
