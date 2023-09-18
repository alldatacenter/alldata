#!/bin/bash


DOLPHINSCHEDULER_ALERT_HOME=$DOLPHINSCHEDULER_HOME/alert-server

pid=/opt/edp/${service.serviceName}/data/alert-server-pid
log=/opt/edp/${service.serviceName}/log/alert-server-$HOSTNAME.out
DS_CONF=/opt/edp/${service.serviceName}/conf
source "$DS_CONF/dolphinscheduler_env.sh"

JAVA_OPTS="-server -Duser.timezone=$SPRING_JACKSON_TIME_ZONE -Xms1g -Xmx1g -Xmn512m -XX:+PrintGCDetails -Xloggc:gc.log -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=dump.hprof"




nohup java $JAVA_OPTS \
  -cp "$DS_CONF/common.properties":"$DOLPHINSCHEDULER_ALERT_HOME/libs/*" \
 -Dlogging.config=$DS_CONF/alert-logback.xml -Dspring.config.location=$DS_CONF/alert-application.yaml  org.apache.dolphinscheduler.alert.AlertServer      > $log 2>&1 &

echo $! > $pid
tail -f /dev/null