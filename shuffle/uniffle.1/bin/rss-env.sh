#!/usr/bin/env bash

#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

set -o pipefail
set -o nounset   # exit the script if you try to use an uninitialised variable
set -o errexit   # exit the script if any statement returns a non-true return value

JAVA_HOME=<java_home_dir>
HADOOP_HOME=<hadoop_home_dir>
XMX_SIZE="80g" # Shuffle Server JVM XMX size

# RSS_HOME, RSS home directory (Default: parent directory of the script)
# RSS_CONF_DIR, RSS configuration directory (Default: ${RSS_HOME}/conf)
# HADOOP_CONF_DIR, Hadoop configuration directory (Default: ${HADOOP_HOME}/etc/hadoop)
# RSS_PID_DIR, Where the pid file is stored (Default: ${RSS_HOME})
# RSS_LOG_DIR, Where log files are stored (Default: ${RSS_HOME}/logs)
# RSS_IP, IP address Shuffle Server binds to on this node (Default: first non-loopback ipv4)
