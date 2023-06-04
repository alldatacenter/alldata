#! /bin/bash
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements. See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership. The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License. You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#

#======================================================================
# Stop the project service shell script
# Find PID by project name
# Then kill -9 pid
#======================================================================

# Project name
APPLICATION="InlongManagerMain"
echo stop ${APPLICATION} Application...

# Project startup jar package name
APPLICATION_JAR="manager-web.jar"
INLONG_STOP_TIMEOUT=30
PID=$(ps -ef | grep "${APPLICATION_JAR}" | grep -v grep | awk '{ print $2 }')

if [[ -z "$PID" ]]; then
  echo ${APPLICATION} was already stopped
fi
echo "${APPLICATION} stopping"
kill -15 ${PID}
count=0
while ps -p ${PID} > /dev/null;
do
    echo "Shutdown is in progress... Please wait..."
    sleep 1
    count=$((count+1))
    if [ ${count} -eq ${INLONG_STOP_TIMEOUT} ]; then
        break
    fi
done
if ps -p ${PID} > /dev/null; then
    echo "${APPLICATION} did not stop gracefully, killing with SIGKILL..."
    kill -9 ${PID}
else
    echo "${APPLICATION} stopped successfully"
fi
