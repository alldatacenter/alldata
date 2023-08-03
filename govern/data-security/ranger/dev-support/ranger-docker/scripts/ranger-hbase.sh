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

if [ ! -e ${HBASE_HOME}/.setupDone ]
then
  su -c "ssh-keygen -t rsa -P '' -f ~/.ssh/id_rsa" hbase
  su -c "cat ~/.ssh/id_rsa.pub >> ~/.ssh/authorized_keys" hbase
  su -c "chmod 0600 ~/.ssh/authorized_keys" hbase

  echo "ssh" > /etc/pdsh/rcmd_default

  ${RANGER_SCRIPTS}/ranger-hbase-setup.sh

  touch ${HBASE_HOME}/.setupDone
fi

su -c "${HBASE_HOME}/bin/start-hbase.sh" hbase

HBASE_MASTER_PID=`ps -ef  | grep -v grep | grep -i "org.apache.hadoop.hbase.master.HMaster" | awk '{print $2}'`

# prevent the container from exiting
tail --pid=$HBASE_MASTER_PID -f /dev/null
