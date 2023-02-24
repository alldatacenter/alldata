#!/usr/bin/env bash
#
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
#
# The java implementation to use. If JAVA_HOME is not found we expect java and jar to be in path
#export JAVA_HOME=

# any additional java opts you want to set. This will apply to both client and server operations
#export ATLAS_OPTS=

# any additional java opts that you want to set for client only
#export ATLAS_CLIENT_OPTS=

# java heap size we want to set for the client. Default is 1024MB
#export ATLAS_CLIENT_HEAP=

# any additional opts you want to set for atlas service.
#export ATLAS_SERVER_OPTS=

# indicative values for large number of metadata entities (equal or more than 10,000s)
#export ATLAS_SERVER_OPTS="-server -XX:SoftRefLRUPolicyMSPerMB=0 -XX:+CMSClassUnloadingEnabled -XX:+UseConcMarkSweepGC -XX:+CMSParallelRemarkEnabled -XX:+PrintTenuringDistribution -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=dumps/atlas_server.hprof -Xloggc:logs/gc-worker.log -verbose:gc -XX:+UseGCLogFileRotation -XX:NumberOfGCLogFiles=10 -XX:GCLogFileSize=1m -XX:+PrintGCDetails -XX:+PrintHeapAtGC -XX:+PrintGCTimeStamps"

# java heap size we want to set for the atlas server. Default is 1024MB
#export ATLAS_SERVER_HEAP=

# indicative values for large number of metadata entities (equal or more than 10,000s) for JDK 8
#export ATLAS_SERVER_HEAP="-Xms15360m -Xmx15360m -XX:MaxNewSize=5120m -XX:MetaspaceSize=100M -XX:MaxMetaspaceSize=512m"

# What is is considered as atlas home dir. Default is the base locaion of the installed software
#export ATLAS_HOME_DIR=

# Where log files are stored. Defatult is logs directory under the base install location
#export ATLAS_LOG_DIR=

# Where pid files are stored. Defatult is logs directory under the base install location
#export ATLAS_PID_DIR=

# where the atlas titan db data is stored. Defatult is logs/data directory under the base install location
#export ATLAS_DATA_DIR=

# Where do you want to expand the war file. By Default it is in /server/webapp dir under the base install dir.
#export ATLAS_EXPANDED_WEBAPP_DIR=

# indicates whether or not a local instance of HBase should be started for Atlas
export MANAGE_LOCAL_HBASE=${hbase.embedded}

# indicates whether or not a local instance of Solr should be started for Atlas
export MANAGE_LOCAL_SOLR=${solr.embedded}

# indicates whether or not cassandra is the embedded backend for Atlas
export MANAGE_EMBEDDED_CASSANDRA=${cassandra.embedded}

# indicates whether or not a local instance of Elasticsearch should be started for Atlas
export MANAGE_LOCAL_ELASTICSEARCH=${elasticsearch.managed}
