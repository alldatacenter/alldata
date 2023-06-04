#!/bin/bash


DOLPHINSCHEDULER_API_HOME=$DOLPHINSCHEDULER_HOME/api-server

pid=/opt/edp/${service.serviceName}/data/api-server-pid
log=/opt/edp/${service.serviceName}/log/api-server-$HOSTNAME.out
DS_CONF=/opt/edp/${service.serviceName}/conf
source "$DS_CONF/dolphinscheduler_env.sh"

JAVA_OPTS="-server -Duser.timezone=$SPRING_JACKSON_TIME_ZONE -Xms1g -Xmx1g -Xmn512m -XX:+PrintGCDetails -Xloggc:gc.log -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=dump.hprof"

cd $DOLPHINSCHEDULER_API_HOME

# 启动后访问地址  http://localhost:12345/dolphinscheduler/doc.html
#               http://localhost:12345/dolphinscheduler/ui
nohup java $JAVA_OPTS \
  -cp "$DS_CONF/common.properties":"$DOLPHINSCHEDULER_API_HOME/libs/*" \
 -Dlogging.config=$DS_CONF/api-logback.xml  -Dspring.config.location=$DS_CONF/api-application.yaml org.apache.dolphinscheduler.api.ApiApplicationServer     > $log 2>&1 &

echo $! > $pid
tail -f /dev/null