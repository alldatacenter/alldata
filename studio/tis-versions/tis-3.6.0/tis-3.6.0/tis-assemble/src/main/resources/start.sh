#!/bin/sh

WORK_HOME=/home/admin/full-dump-assemble
cd $WORK_HOME
CLASSPATH=./dependency/*:

export JAVA_HOME=/usr/lib/jvm/java-1.7.0-openjdk.x86_64

echo $CLASSPATH

start(){ 

./commons-daemon-1.0.15-src/src/native/unix/jsvc \
  -cwd $WORK_HOME \
  -debug \
  -home $JAVA_HOME \
  -keepstdin \
  -Xmx2014m -Xms2014m -Xmn469m -Xss256k \
  -Dlog.dir=/opt/logs/tis \
  -Ddir-hdfs20=./dir-hdfs20 ${sys.argument} \
  -pidfile ./pidfile.pid \
  -cp $CLASSPATH \
   com.qlangtech.tis.order.center.IndexSwapTaskflowLauncher



  echo "start successful"
}


stop(){ 

./commons-daemon-1.0.15-src/src/native/unix/jsvc  -cwd $WORK_HOME  -stop -pidfile ./pidfile.pid -cp $CLASSPATH com.qlangtech.tis.order.center.IndexSwapTaskflowLauncher

echo "dfire-order-full-dump stopped"

}


case "$1" in  
    start)  
        start  
    ;;  
    stop)  
        stop  
    ;;  
    restart)  
        stop  
        start  
    ;;  
    *)  
        usage  
    ;;  
esac 





