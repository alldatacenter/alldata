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

usage() {
  echo "usage: run-filesource-usergroupsync.sh
 -h                  show help.
 -i <arg>            Input file name ( csv or json file )
			  JSON FILE FORMAT
				{
				"user1":["group-1", "group-2", "group-3"],
				"user2":["group-x","group-y","group-z"]
				}

		      CSV FILE FORMAT
                    user-1,group-1,group-2,group-3
                    user-2,group-x,group-y,group-z"
  exit 1
}

logdir="/var/log/ranger/usersync"
scriptPath=$(cd "$(dirname "$0")"; pwd)
ugsync_home="${scriptPath}/.."
cp="${ugsync_home}/dist/*:${ugsync_home}/lib/*:${ugsync_home}/conf"

JAVA_CMD="java -Dlogdir=${logdir} -cp ${cp} org.apache.ranger.unixusersync.process.FileSourceUserGroupBuilder"

while getopts "i:h" opt; do
  case $opt in
    i) JAVA_CMD="$JAVA_CMD $OPTARG"
       fileName=$OPTARG
       ;;
    h) usage
       ;;
   \?) echo -e \\n"Option -$OPTARG not allowed."
        usage
        ;;
  esac
done

if [ $OPTIND -eq 1 ];
then
  usage;
fi

echo "JAVA commnad = $JAVA_CMD"

if [ "${JAVA_HOME}" != "" ]
then
	export JAVA_HOME
	PATH="${JAVA_HOME}/bin:${PATH}"
	export PATH
fi
$JAVA_CMD
errorCode=$?
if [ ${errorCode} -eq 0 ]; then
    echo "Successfully loaded users/groups from file ${fileName}"
else
    echo "Failed to load users/groups from file ${fileName}: error code=${errorCode}"
fi
