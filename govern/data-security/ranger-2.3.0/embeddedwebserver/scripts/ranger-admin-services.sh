#!/bin/bash

# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

if [[ -z $1 ]]; then
	echo "Invalid argument [$1];"
	echo "Usage: Only start | stop | restart | metric | version , are supported."
	echo "For Metric Usage: metric -type policies | audits | usergroup | services | database | contextenrichers | denyconditions"
	exit;
fi
action=$1
arg2=$2
arg3=$3
action=`echo $action | tr '[:lower:]' '[:upper:]'`
realScriptPath=`readlink -f $0`
realScriptDir=`dirname $realScriptPath`
XAPOLICYMGR_DIR=`(cd $realScriptDir/..; pwd)`

XAPOLICYMGR_EWS_DIR=${XAPOLICYMGR_DIR}/ews
RANGER_JAAS_LIB_DIR="${XAPOLICYMGR_EWS_DIR}/ranger_jaas"
RANGER_JAAS_CONF_DIR="${XAPOLICYMGR_EWS_DIR}/webapp/WEB-INF/classes/conf/ranger_jaas"
ranger_admin_max_heap_size=1g

if [ -f ${XAPOLICYMGR_DIR}/ews/webapp/WEB-INF/classes/conf/java_home.sh ]; then
        . ${XAPOLICYMGR_DIR}/ews/webapp/WEB-INF/classes/conf/java_home.sh
fi

for custom_env_script in `find ${XAPOLICYMGR_DIR}/ews/webapp/WEB-INF/classes/conf/ -name "ranger-admin-env*"`; do
        if [ -f $custom_env_script ]; then
                . $custom_env_script
        fi
done

JAVA_OPTS=" ${JAVA_OPTS} -XX:MetaspaceSize=100m -XX:MaxMetaspaceSize=200m -Xmx${ranger_admin_max_heap_size} -Xms1g -Xloggc:${XAPOLICYMGR_EWS_DIR}/logs/gc-worker.log -verbose:gc -XX:+PrintGCDetails"
if [[ ${JAVA_OPTS} != *"-Duser.timezone"* ]] ;then  export JAVA_OPTS=" ${JAVA_OPTS} -Duser.timezone=UTC" ;fi

if [ "$JAVA_HOME" != "" ]; then
        export PATH=$JAVA_HOME/bin:$PATH
fi

cd ${XAPOLICYMGR_EWS_DIR}
if [ -z "${RANGER_ADMIN_LOG_DIR}" ]
then
	RANGER_ADMIN_LOG_DIR=${XAPOLICYMGR_EWS_DIR}/logs
fi

if [ -z "${RANGER_ADMIN_LOGBACK_CONF_FILE}" ]
then
	RANGER_ADMIN_LOGBACK_CONF_FILE=${XAPOLICYMGR_EWS_DIR}/webapp/WEB-INF/classes/conf/logback.xml
fi

if [ -z "${RANGER_PID_DIR_PATH}" ]
then
        RANGER_PID_DIR_PATH=/var/run/ranger
fi

if [ -z "${RANGER_ADMIN_PID_NAME}" ]
then
        RANGER_ADMIN_PID_NAME=rangeradmin.pid
fi

if [ ! -d "${RANGER_PID_DIR_PATH}" ]
then  
	mkdir -p  $RANGER_PID_DIR_PATH
	chmod 660 $RANGER_PID_DIR_PATH
fi

# User can set their own pid path using RANGER_PID_DIR_PATH and
# RANGER_ADMIN_PID_NAME variable before calling the script. The user can modify
# the value of the RANGER_PID_DIR_PATH in ranger-admin-env-piddir.sh to change
# pid path and set the value of RANGER_ADMIN_PID_NAME to change the pid file.
pidf=${RANGER_PID_DIR_PATH}/${RANGER_ADMIN_PID_NAME}

if [ -z "${RANGER_USER}" ]
then
        RANGER_USER=ranger
fi
SERVER_NAME=rangeradmin
start() {
	SLEEP_TIME_AFTER_START=5
	nohup  java -Dproc_rangeradmin ${JAVA_OPTS} -Dlogback.configurationFile=file:${RANGER_ADMIN_LOGBACK_CONF_FILE}  -Duser=${USER} -Dhostname=${HOSTNAME} ${DB_SSL_PARAM} -Dservername=${SERVER_NAME} -Dlogdir=${RANGER_ADMIN_LOG_DIR} -Dcatalina.base=${XAPOLICYMGR_EWS_DIR} -cp "${XAPOLICYMGR_EWS_DIR}/webapp/WEB-INF/classes/conf:${XAPOLICYMGR_EWS_DIR}/lib/*:${XAPOLICYMGR_EWS_DIR}/webapp/WEB-INF/lib/*:${RANGER_JAAS_LIB_DIR}/*:${RANGER_JAAS_CONF_DIR}:${JAVA_HOME}/lib/*:${RANGER_HADOOP_CONF_DIR}/*:$CLASSPATH" org.apache.ranger.server.tomcat.EmbeddedServer > ${RANGER_ADMIN_LOG_DIR}/catalina.out 2>&1 &
	VALUE_OF_PID=$!
	echo "Starting Apache Ranger Admin Service"
	sleep $SLEEP_TIME_AFTER_START
	if ps -p $VALUE_OF_PID > /dev/null
	then
		echo $VALUE_OF_PID > ${pidf}
                chown ${RANGER_USER} ${pidf}
		chmod 660 ${pidf}
		pid=`cat $pidf`
		echo "Apache Ranger Admin Service with pid ${pid} has started."
	else
		echo "Apache Ranger Admin Service failed to start!"
	fi
	exit;
}

stop(){
	WAIT_TIME_FOR_SHUTDOWN=2
	NR_ITER_FOR_SHUTDOWN_CHECK=15
	if [ -f "$pidf" ] ; then
		pid=`cat $pidf` > /dev/null 2>&1
		echo "Getting pid from $pidf .."
	else
		pid=`ps -ef | grep java | grep -- '-Dproc_rangeradmin' | grep -v grep | awk '{ print $2 }'`
		if [ "$pid" != "" ];then
			echo "pid file($pidf) not present, taking pid from \'ps\' command.."
		else
			echo "Apache Ranger Admin Service is not running"
			return
		fi
	fi

	echo "Found Apache Ranger Admin Service with pid $pid, Stopping it..."
	nohup java ${JAVA_OPTS} -Duser=${USER} -Dhostname=${HOSTNAME} -Dlogdir=${RANGER_ADMIN_LOG_DIR} -Dcatalina.base=${XAPOLICYMGR_EWS_DIR} -cp "${XAPOLICYMGR_EWS_DIR}/webapp/WEB-INF/classes/conf:${XAPOLICYMGR_EWS_DIR}/lib/*:${RANGER_JAAS_LIB_DIR}/*:${RANGER_JAAS_CONF_DIR}:${RANGER_HADOOP_CONF_DIR}/*:$CLASSPATH" org.apache.ranger.server.tomcat.StopEmbeddedServer > ${RANGER_ADMIN_LOG_DIR}/catalina.out 2>&1
	for ((i=0; i<$NR_ITER_FOR_SHUTDOWN_CHECK; i++))
	do
		sleep $WAIT_TIME_FOR_SHUTDOWN
		if ps -p $pid > /dev/null ; then
			echo "Shutdown in progress. Will check after $WAIT_TIME_FOR_SHUTDOWN secs again.."
			continue;
		else
			break;
		fi
	done
	# if process is still around, use kill -9
	if ps -p $pid > /dev/null ; then
		echo "Initial kill failed, getting serious now..."
		kill -9 $pid
	fi
	sleep 1 #give kill -9  sometime to "kill"
	if ps -p $pid > /dev/null ; then
		echo "Wow, even kill -9 failed, giving up! Sorry.."
		exit 1

	else
		rm -rf $pidf
		echo "Apache Ranger Admin Service with pid ${pid} has been stopped."
	fi

}

metric(){
 if [ "$JAVA_HOME" == "" ]; then
  echo "[E] JAVA_HOME environment variable not defined, aborting Apache Ranger Admin metric collection"
  exit 1;
 fi
 java ${JAVA_OPTS} -Dlogdir=${RANGER_ADMIN_LOG_DIR} -cp "${XAPOLICYMGR_EWS_DIR}/webapp/WEB-INF/classes/conf:${XAPOLICYMGR_EWS_DIR}/webapp/WEB-INF/classes/:${XAPOLICYMGR_EWS_DIR}/webapp/WEB-INF/classes/lib/*:${XAPOLICYMGR_EWS_DIR}/webapp/WEB-INF/classes/META-INF:${XAPOLICYMGR_EWS_DIR}/webapp/WEB-INF/lib/*:${XAPOLICYMGR_EWS_DIR}/webapp/META-INF:${XAPOLICYMGR_EWS_DIR}/lib/*:${RANGER_JAAS_LIB_DIR}/*:${RANGER_JAAS_CONF_DIR}:${JAVA_HOME}/lib/*:${RANGER_HADOOP_CONF_DIR}/*:$CLASSPATH" org.apache.ranger.patch.cliutil.MetricUtil ${arg2} ${arg3} 2>/dev/null
}

if [ "${action}" == "START" ]; then
	if [ -f "$pidf" ] ; then
		pid=`cat $pidf`
		if  ps -p $pid > /dev/null
		then
			echo "Apache Ranger Admin Service is already running [pid={$pid}]"
			exit 1
		else
			rm -rf $pidf
			start;
		fi
    else
		start;
	fi
elif [ "${action}" == "STOP" ]; then
	stop;
	exit;
elif [ "${action}" == "RESTART" ]; then
	echo "Restarting Apache Ranger Admin"
	stop;
	start;
elif [ "${action}" == "METRIC" ]; then
	metric;
elif [ "${action}" == "VERSION" ]; then
	cd ${XAPOLICYMGR_EWS_DIR}/webapp/WEB-INF/lib
	java -cp ranger-util-*.jar org.apache.ranger.common.RangerVersionInfo
	exit;
else
    echo "Invalid argument [$1];"
    echo "Usage: Only start | stop | restart | metric | version, are supported."
    echo "For metric Usage: metric -type policies | audits | usergroup | services | database | contextenrichers | denyconditions"
    exit;
fi
