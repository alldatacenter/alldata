#!/bin/bash
usage="Usage: start.sh (start|stop|restart) <command> "

# if no args specified, show usage
if [ $# -le 1 ]; then
  echo $usage
  exit 1
fi
startStop=$1
shift
command=$1
SH_DIR=`dirname $0`
export LOG_DIR=$SH_DIR/logs
export PID_DIR=$SH_DIR/pid


log=$LOG_DIR/$command.out
pid=$PID_DIR/$command.pid
ident=$SH_DIR/ident.id

if [ ! -d "$LOG_DIR" ]; then
  mkdir $LOG_DIR
fi
if [ $command = "hiveserver2" ]
  then
	  cmd="$SH_DIR/bin/hiveserver2"
fi
if [ $command = "hivemetastore" ]
  then
	  cmd="$SH_DIR/bin/hive --service metastore"
fi


start(){
	[ -w "$PID_DIR" ] ||  mkdir -p "$PID_DIR"
  if [ -f $pid ]; then
    if kill -0 `cat $pid` > /dev/null 2>&1; then
      echo $command running as process `cat $pid`.  Stop it first.
      exit 1
    fi
  fi
  if [ $command = "hiveserver2" ]
  then
      /opt/datasophon/hadoop-3.3.3/bin/hdfs dfs -mkdir -p /user/hive/warehouse
      /opt/datasophon/hadoop-3.3.3/bin/hdfs dfs -chmod g+w /user/hive/warehouse
  fi
  echo starting $command, logging to $log
  echo "nohup $cmd > $log 2>&1 &"
  nohup $cmd > $log 2>&1 &
  echo $! > $pid
}
stop(){
	if [ -f $pid ]; then
        TARGET_PID=`cat $pid`
        if kill -0 $TARGET_PID > /dev/null 2>&1; then
          echo stopping $command
          kill $TARGET_PID
          sleep 3s
          if kill -0 $TARGET_PID > /dev/null 2>&1; then
            echo "$command did not stop gracefully after 3 seconds: killing with kill -9"
            kill -9 $TARGET_PID
          fi
        else
          echo no $command to stop
        fi
        rm -f $pid
      else
        echo no $command to stop
      fi
}
status(){
  if [ -f $pid ]; then
    ARGET_PID=`cat $pid`
    kill -0 $ARGET_PID
    if [ $? -eq 0 ]
    then
      echo "$command is  running "
    else
      echo "$command  is not running"
      exit 1
    fi
  else
    echo "$command  pid file is not exists"
    exit 1
	fi
}
restart(){
	stop
	sleep 10
	start
}
case $startStop in
  (start)
    start
    ;;
  (stop)
    stop
      ;;
  (status)
	  status
	;;
  (restart)
	  restart
      ;;
  (*)
    echo $usage
    exit 1
    ;;
esac


echo "End $startStop $command."