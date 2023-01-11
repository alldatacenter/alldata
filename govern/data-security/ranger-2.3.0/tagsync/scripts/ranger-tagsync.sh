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

### BEGIN INIT INFO
# Provides:        ranger-tagsync
# Required-Start:  $local_fs $remote_fs $network $named $syslog $time
# Required-Stop:   $local_fs $remote_fs $network $named $syslog $time
# Default-Start:   2 3 4 5
# Default-Stop:
# Short-Description: Start/Stop Ranger tagsync
### END INIT INFO

LINUX_USER=ranger
BIN_PATH=/usr/bin
MOD_NAME=ranger-tagsync-services.sh
pidf=/var/run/ranger/tagsync.pid
pid=""
if [ -f ${pidf} ]
then
    pid=`cat $pidf`
fi

case $1 in
	start)
	    if [ "${pid}" != "" ]
	    then
	        echo "Ranger tagsync Service is already running"
		    exit 1
		 else
		 	echo "Starting Ranger tagsync."
		    /bin/su --login  $LINUX_USER -c "${BIN_PATH}/${MOD_NAME} start"
	    fi
		;;
	stop)
	    if [ "${pid}" != "" ]
        then
            echo "Stopping Ranger tagsync."
            /bin/su --login  $LINUX_USER -c "${BIN_PATH}/${MOD_NAME} stop"
        else
            echo "Ranger tagsync Service is NOT running"
            exit 1
        fi
		;;
	restart)
        if [ "${pid}" != "" ]
        then
            echo "Stopping Ranger tagsync."
            /bin/su --login  $LINUX_USER -c "${BIN_PATH}/${MOD_NAME} stop"
            sleep 10
        fi
        echo "Starting Ranger tagsync."
        /bin/su --login  $LINUX_USER -c "${BIN_PATH}/${MOD_NAME} start"
		;;
	status)
        if [ "${pid}" != "" ]
        then
            echo "Ranger tagsync Service is running [pid={$pid}]"
        else
            echo "Ranger tagsync Service is NOT running."
        fi
	 ;;
	*)
		echo "Invalid argument [$1]; Only start | stop | restart | status, are supported."
		exit 1
	esac
