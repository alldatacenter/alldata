#!/bin/bash


DOLPHINSCHEDULER_MASTER_HOME=$DOLPHINSCHEDULER_HOME/master-server

pid=/opt/edp/${service.serviceName}/data/master-server-pid
log=/opt/edp/${service.serviceName}/log/master-server-$HOSTNAME.out
DS_CONF=/opt/edp/${service.serviceName}/conf

source "$DS_CONF/dolphinscheduler_env.sh"

JAVA_OPTS="-server -Duser.timezone=$SPRING_JACKSON_TIME_ZONE -Xms1g -Xmx1g -Xmn512m -XX:+PrintGCDetails -Xloggc:gc.log -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=dump.hprof"




nohup java $JAVA_OPTS \
  -cp "$DS_CONF/common.properties":"$DOLPHINSCHEDULER_MASTER_HOME/libs/*" \
-Dlogging.config=$DS_CONF/master-logback.xml  -Dspring.config.location=$DS_CONF/master-application.yaml  org.apache.dolphinscheduler.server.master.MasterServer    > $log 2>&1 &

echo $! > $pid
tail -f /dev/null