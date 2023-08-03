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

CREATE_HDFS_DIR=false

if [ ! -e ${HADOOP_HOME}/.setupDone ]
then
  su -c "ssh-keygen -t rsa -P '' -f ~/.ssh/id_rsa" hdfs
  su -c "cat ~/.ssh/id_rsa.pub >> ~/.ssh/authorized_keys" hdfs
  su -c "chmod 0600 ~/.ssh/authorized_keys" hdfs

  su -c "ssh-keygen -t rsa -P '' -f ~/.ssh/id_rsa" yarn
  su -c "cat ~/.ssh/id_rsa.pub >> ~/.ssh/authorized_keys" yarn
  su -c "chmod 0600 ~/.ssh/authorized_keys" yarn

  echo "ssh" > /etc/pdsh/rcmd_default

  ${RANGER_SCRIPTS}/ranger-hadoop-setup.sh

  su -c "${HADOOP_HOME}/bin/hdfs namenode -format" hdfs

  CREATE_HDFS_DIR=true
  touch ${HADOOP_HOME}/.setupDone
fi

su -c "${HADOOP_HOME}/sbin/start-dfs.sh" hdfs
su -c "${HADOOP_HOME}/sbin/start-yarn.sh" yarn

if [ "${CREATE_HDFS_DIR}" == "true" ]
then
  su -c "${RANGER_SCRIPTS}/ranger-hadoop-mkdir.sh" hdfs
fi

NAMENODE_PID=`ps -ef  | grep -v grep | grep -i "org.apache.hadoop.hdfs.server.namenode.NameNode" | awk '{print $2}'`

# prevent the container from exiting
tail --pid=$NAMENODE_PID -f /dev/null
