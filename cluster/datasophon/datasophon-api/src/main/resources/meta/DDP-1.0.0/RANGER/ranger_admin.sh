#!/bin/bash
usage="Usage: start.sh (start|stop|restart) <command> "

# if no args specified, show usage
startStop=$1

start(){
	echo "ranger admin start"
	ranger-admin start
	if [ $? -eq 0 ]
    then
		echo "ranger admin start success"
	else
		echo "ranger admin start failed"
		exit 1
	fi
}
stop(){
	echo "ranger admin stop"
	ranger-admin stop
	if [ $? -eq 0 ]
    then
		echo "ranger admin stop success"
	else
		echo "ranger admin stop failed"
		exit 1
	fi
}
status(){
  echo "ranger admin status"
  pid=`jps | grep -iw EmbeddedServer | grep -v grep | awk '{print $1}'`
	echo "pid is : $pid"
	kill -0 $pid
	if [ $? -eq 0 ]
	then
		echo "$command is  running "
	else
		echo "$command  is not running"
		exit 1
	fi
}
restart(){
	echo "ranger admin restart"
	ranger-admin restart
	if [ $? -eq 0 ]
    then
		echo "ranger admin restart success"
	else
		echo "ranger admin restart failed"
		exit 1
	fi
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


echo "End $startStop ranger"