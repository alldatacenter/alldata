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

service ssh start

if [ ! -e ${KNOX_HOME}/.setupDone ]
then
  su -c "ssh-keygen -t rsa -P '' -f ~/.ssh/id_rsa" knox
  su -c "cat ~/.ssh/id_rsa.pub >> ~/.ssh/authorized_keys" knox
  su -c "chmod 0600 ~/.ssh/authorized_keys" knox

  echo "ssh" > /etc/pdsh/rcmd_default

  ${RANGER_SCRIPTS}/ranger-knox-setup.sh

  touch ${KNOX_HOME}/.setupDone
fi

su -c "${KNOX_HOME}/bin/ldap.sh start" knox

su -c "${KNOX_HOME}/bin/gateway.sh start" knox

KNOX_GATEWAY_PID=`ps -ef  | grep -v grep | grep -i "gateway.jar" | awk '{print $2}'`

# prevent the container from exiting
tail --pid=$KNOX_GATEWAY_PID -f /dev/null
