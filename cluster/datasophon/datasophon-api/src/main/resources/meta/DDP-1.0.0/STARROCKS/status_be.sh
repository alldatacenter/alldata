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

curdir=`dirname "$0"`
curdir=`cd "$curdir"; pwd`
PID_DIR=`cd "$curdir"; pwd`
pid=$PID_DIR/be.pid


status(){
  if [ -f $pid ]; then
    ARGET_PID=`cat $pid`
    echo "pid is $ARGET_PID"
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
case $startStop in
  (status)
	  status
	;;
  (*)
    echo $usage
    exit 1
    ;;
esac


echo "End $startStop $command."
