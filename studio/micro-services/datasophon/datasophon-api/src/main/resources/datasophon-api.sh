#!/bin/sh
usage="Usage: start.sh (start|stop|restart) <command> "

# if no args specified, show usage
if [ $# -le 1 ]; then
  echo $usage
  exit 1
fi

startStop=$1
shift
command=$1
shift

echo "Begin $startStop $command......"

BIN_DIR=`dirname $0`
BIN_DIR=`cd "$BIN_DIR"; pwd`
DDH_HOME=$BIN_DIR/..

source /etc/profile

export JAVA_HOME=$JAVA_HOME
#export JAVA_HOME=/opt/soft/jdk
export HOSTNAME=`hostname`

export DDH_PID_DIR=$DDH_HOME/pid
export DDH_LOG_DIR=$DDH_HOME/logs
export DDH_CONF_DIR=$DDH_HOME/conf
export DDH_LIB_JARS=$DDH_HOME/lib/*

export DDH_OPTS="-server -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=128m -Xss512k -XX:+UseParNewGC -XX:+UseConcMarkSweepGC -XX:+CMSParallelRemarkEnabled -XX:LargePageSizeInBytes=128m -XX:+UseCMSInitiatingOccupancyOnly -XX:CMSInitiatingOccupancyFraction=70 -XX:+PrintGCDetails -Xloggc:$DOLPHINSCHEDULER_LOG_DIR/gc.log -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=dump.hprof -XshowSettings:vm $DDH_OPTS"
export STOP_TIMEOUT=5

if [ ! -d "$DDH_LOG_DIR" ]; then
  mkdir $DDH_LOG_DIR
fi

log=$DDH_LOG_DIR/$command-$HOSTNAME.out
pid=$DDH_PID_DIR/$command.pid

cd $DDH_HOME

if [ "$command" = "api" ]; then
  LOG_FILE="-Dlogging.config=classpath:logback-api.xml -Dspring.profiles.active=api"
  CLASS=com.datasophon.api.DDHApplicationServer
  JMX="-javaagent:$DDH_HOME/jmx/jmx_prometheus_javaagent-0.16.1.jar=8586:$DDH_HOME/jmx/jmx_exporter_config.yaml"
  HEAP_OPTS="-Xms1g -Xmx1g -Xmn512m"
  export DDH_OPTS="$HEAP_OPTS $DDH_OPTS $JMX"
elif [ "$command" = "all" ]; then
  CLASS=com.datasophon.data.service.DataServiceApplication
else
  echo "Error: No command named \`$command' was found."
  exit 1
fi

case $startStop in
  (start)
    [ -w "$DDH_PID_DIR" ] ||  mkdir -p "$DDH_PID_DIR"

    if [ -f $pid ]; then
      if kill -0 `cat $pid` > /dev/null 2>&1; then
        echo $command running as process `cat $pid`.  Stop it first.
        exit 1
      fi
    fi

    echo starting $command, logging to $log

    exec_command="$LOG_FILE $DDH_OPTS -classpath $DDH_CONF_DIR:$DDH_LIB_JARS $CLASS"

    echo "nohup $JAVA_HOME/bin/java $exec_command > $log 2>&1 &"
    nohup $JAVA_HOME/bin/java $exec_command > $log 2>&1 &
    echo $! > $pid
    ;;

  (stop)
      if [ -f $pid ]; then
        TARGET_PID=`cat $pid`
        if kill -0 $TARGET_PID > /dev/null 2>&1; then
          echo stopping $command
          kill $TARGET_PID
          sleep $STOP_TIMEOUT
          if kill -0 $TARGET_PID > /dev/null 2>&1; then
            echo "$command did not stop gracefully after $STOP_TIMEOUT seconds: killing with kill -9"
            kill -9 $TARGET_PID
          fi
        else
          echo no $command to stop
        fi
        rm -f $pid
      else
        echo no $command to stop
      fi
      ;;
  (restart)
      if [ -f $pid ]; then
        TARGET_PID=`cat $pid`
        if kill -0 $TARGET_PID > /dev/null 2>&1; then
          echo stopping $command
          kill $TARGET_PID
          sleep $STOP_TIMEOUT
          if kill -0 $TARGET_PID > /dev/null 2>&1; then
            echo "$command did not stop gracefully after $STOP_TIMEOUT seconds: killing with kill -9"
            kill -9 $TARGET_PID
          fi
        else
          echo no $command to stop
        fi
        rm -f $pid
      else
        echo no $command to stop
      fi
      sleep 2s
      [ -w "$DDH_PID_DIR" ] ||  mkdir -p "$DDH_PID_DIR"
      if [ -f $pid ]; then
          if kill -0 `cat $pid` > /dev/null 2>&1; then
            echo $command running as process `cat $pid`.  Stop it first.
            exit 1
          fi
      fi
      echo starting $command, logging to $log

      exec_command="$LOG_FILE $DDH_OPTS -classpath $DDH_CONF_DIR:$DDH_LIB_JARS $CLASS"

      echo "nohup $JAVA_HOME/bin/java $exec_command > $log 2>&1 &"
      nohup $JAVA_HOME/bin/java $exec_command > $log 2>&1 &
      echo $! > $pid
      ;;
  (*)
    echo $usage
    exit 1
    ;;

esac

echo "End $startStop $command."