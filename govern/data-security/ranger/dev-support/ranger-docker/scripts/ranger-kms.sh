#!/bin/bash

# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.


if [ ! -e ${RANGER_HOME}/.setupDone ]
then
  SETUP_RANGER=true
else
  SETUP_RANGER=false
fi

if [ "${SETUP_RANGER}" == "true" ]
then
  cd "${RANGER_HOME}"/kms || exit
  if ./setup.sh;
  then
    touch "${RANGER_HOME}"/.setupDone
  else
    echo "Ranger KMS Setup Script didn't complete proper execution."
  fi
fi

# delete PID file if exists
rm -f /var/run/ranger_kms/rangerkms.pid

cd ${RANGER_HOME}/kms && ./ranger-kms-services.sh start

RANGER_KMS_PID=`ps -ef  | grep -v grep | grep "Dproc_rangerkms" | awk '{print $2}'`

# prevent the container from exiting
if [ -z "$RANGER_KMS_PID" ]
then
  echo "Ranger KMS process probably exited, no process id found!"
else
  tail --pid=$RANGER_KMS_PID -f /dev/null
fi
