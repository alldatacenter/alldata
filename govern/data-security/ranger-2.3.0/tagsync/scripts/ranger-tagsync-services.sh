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
        echo "No argument provided.."
        echo "Usage: $0 {start | stop | restart | version}"
        exit;
fi
action=$1
action=`echo $action | tr '[:lower:]' '[:upper:]'`
realScriptPath=`readlink -f $0`
realScriptDir=`dirname $realScriptPath`
cd $realScriptDir
cdir=`pwd`
ranger_tagsync_max_heap_size=1g

for custom_env_script in `find ${cdir}/conf/ -name "ranger-tagsync-env*"`; do
        if [ -f $custom_env_script ]; then
                . $custom_env_script
        fi
done

if [ -z "${TAGSYNC_PID_DIR_PATH}" ]; then
        TAGSYNC_PID_DIR_PATH=/var/run/ranger
fi

if [ -z "${TAGSYNC_PID_NAME}" ]
then
        TAGSYNC_PID_NAME=tagsync.pid
fi

pidf=${TAGSYNC_PID_DIR_PATH}/${TAGSYNC_PID_NAME}

if [ -z "${UNIX_TAGSYNC_USER}" ]; then
        UNIX_TAGSYNC_USER=ranger
fi

JAVA_OPTS=" ${JAVA_OPTS} -XX:MetaspaceSize=100m -XX:MaxMetaspaceSize=200m -Xmx${ranger_tagsync_max_heap_size} -Xms1g "

if [ "${action}" == "START" ]; then

	#Export JAVA_HOME
	if [ -f ${cdir}/conf/java_home.sh ]; then
		. ${cdir}/conf/java_home.sh
	fi

	for custom_env_script in `find ${cdir}/conf.dist/ -name "ranger-tagsync-env*"`; do
        	if [ -f $custom_env_script ]; then
                	. $custom_env_script
	        fi
	done

	if [ "$JAVA_HOME" != "" ]; then
        	export PATH=$JAVA_HOME/bin:$PATH
	fi

	if [ -z "${RANGER_TAGSYNC_LOG_DIR}" ]; then
	    RANGER_TAGSYNC_LOG_DIR=/var/log/ranger/tagsync
	fi

	if [ ! -d $RANGER_TAGSYNC_LOG_DIR ]; then
		mkdir -p $RANGER_TAGSYNC_LOG_DIR
		chmod 777 $RANGER_TAGSYNC_LOG_DIR
	fi

	cp="${cdir}/conf:${cdir}/dist/*:${cdir}/lib/*:${RANGER_TAGSYNC_HADOOP_CONF_DIR}/*"

	if [ -f "$pidf" ] ; then
		pid=`cat $pidf`
		if  ps -p $pid > /dev/null
		then
			echo "Apache Ranger Tagsync Service is already running [pid={$pid}]"
			exit ;
		else
			rm -rf $pidf
		fi
	fi

	cd ${cdir}

	SLEEP_TIME_AFTER_START=5
	nohup java -Dproc_rangertagsync ${JAVA_OPTS} -Dlogdir="${RANGER_TAGSYNC_LOG_DIR}" -Dlogback.configurationFile=file:/etc/ranger/tagsync/conf/logback.xml -cp "${cp}" org.apache.ranger.tagsync.process.TagSynchronizer  > ${RANGER_TAGSYNC_LOG_DIR}/tagsync.out 2>&1 &
	VALUE_OF_PID=$!
	echo "Starting Apache Ranger Tagsync Service"
	sleep $SLEEP_TIME_AFTER_START
	if ps -p $VALUE_OF_PID > /dev/null
	then
		echo $VALUE_OF_PID > ${pidf}
                chown ${UNIX_TAGSYNC_USER} ${pidf}
		chmod 660 ${pidf}
		pid=`cat $pidf`
		echo "Apache Ranger Tagsync Service with pid ${pid} has started."
	else
		echo "Apache Ranger Tagsync Service failed to start!"
	fi
	exit;

elif [ "${action}" == "STOP" ]; then
	WAIT_TIME_FOR_SHUTDOWN=2
	NR_ITER_FOR_SHUTDOWN_CHECK=15
	if [ -f "$pidf" ] ; then
		pid=`cat $pidf` > /dev/null 2>&1
		echo "Getting pid from $pidf .."
	else
		pid=`ps -ef | grep java | grep -- '-Dproc_rangertagsync' | grep -v grep | awk '{ print $2 }'`
		if [ "$pid" != "" ];then
			echo "pid file($pidf) not present, taking pid from \'ps\' command.."
		else
			echo "Apache Ranger Tagsync Service is not running"
			return	
		fi
	fi
	echo "Found Apache Ranger Tagsync Service with pid $pid, Stopping it..."
	kill -15 $pid
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
		echo "Apache Ranger Tagsync Service with pid ${pid} has been stopped."
	fi
	exit;
	
elif [ "${action}" == "RESTART" ]; then
	echo "Restarting Apache Ranger Tagsync"
	${cdir}/ranger-tagsync-services.sh stop
	${cdir}/ranger-tagsync-services.sh start
	exit;
elif [ "${action}" == "VERSION" ]; then
	cd ${cdir}/lib
	java -cp ranger-util-*.jar org.apache.ranger.common.RangerVersionInfo
	exit
else 
	echo "Invalid argument [$1];"
	echo "Usage: $0 {start | stop | restart | version}"
    exit;
fi

