#!/usr/bin/env bash
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

# Set Hadoop-specific environment variables here.

# The only required environment variable is JAVA_HOME.  All others are
# optional.  When running a distributed configuration it is best to
# set JAVA_HOME in this file, so that it is correctly defined on
# remote nodes.

# The java implementation to use.
export JAVA_HOME=${r"${JAVA_HOME}"}

# The jsvc implementation to use. Jsvc is required to run secure datanodes.

export HADOOP_CONF_DIR=/opt/edp/${service.serviceName}/conf


# Extra Java CLASSPATH elements.  Automatically insert capacity-scheduler.
for f in $HADOOP_HOME/contrib/capacity-scheduler/*.jar; do
if [ "$HADOOP_CLASSPATH" ]; then
export HADOOP_CLASSPATH=$HADOOP_CLASSPATH:$f
else
export HADOOP_CLASSPATH=$f
fi
done


# The maximum amount of heap to use, in MB. Default is 1000.
#export HADOOP_HEAPSIZE=
#export HADOOP_NAMENODE_INIT_HEAPSIZE=""

# Extra Java runtime options.  Empty by default.
export HADOOP_OPTS="$HADOOP_OPTS -Djava.net.preferIPv4Stack=true $HADOOP_CLIENT_OPTS"

# Command specific options appended to HADOOP_OPTS when specified
# Export namenode memory
<#assign namenodeMemory=conf['hdfs.namenode.memory']?trim?number>

export NAMENODE_MEMORY=${namenodeMemory?floor?c}m
export HADOOP_NAMENODE_OPTS="-Xmx${namenodeMemory?floor?c}m -XX:+UseConcMarkSweepGC -XX:+ExplicitGCInvokesConcurrent -Dcom.sun.management.jmxremote.port=9912 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.local.only=false -javaagent:/opt/jmx_exporter/jmx_prometheus_javaagent-0.14.0.jar=5542:/opt/edp/${service.serviceName}/conf/jmx_prometheus.yaml $HADOOP_NAMENODE_OPTS"
export HADOOP_SECONDARYNAMENODE_OPTS="-Xmx${namenodeMemory?floor?c}m -Dcom.sun.management.jmxremote.port=9913 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.local.only=false -javaagent:/opt/jmx_exporter/jmx_prometheus_javaagent-0.14.0.jar=5543:/opt/edp/${service.serviceName}/conf/jmx_prometheus.yaml $HADOOP_SECONDARYNAMENODE_OPTS"

# Export zkfc memory
<#assign zkfcMemory=conf['hdfs.zkfc.memory']?trim?number>
export ZKFC_MEMORY=${zkfcMemory?floor?c}m
export HADOOP_ZKFC_OPTS="-Xmx${zkfcMemory?floor?c}m -Dcom.sun.management.jmxremote.port=9914 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.local.only=false -javaagent:/opt/jmx_exporter/jmx_prometheus_javaagent-0.14.0.jar=5544:/opt/edp/${service.serviceName}/conf/jmx_prometheus.yaml $HADOOP_ZKFC_OPTS"

# Export datanode memory
<#assign datanodeMemory=conf['hdfs.datanode.memory']?trim?number>
export DATANODE_MEMORY=${datanodeMemory?floor?c}m
export HADOOP_DATANODE_OPTS="-Xmx${datanodeMemory?floor?c}m -Dcom.sun.management.jmxremote.port=9915 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.local.only=false -javaagent:/opt/jmx_exporter/jmx_prometheus_javaagent-0.14.0.jar=5545:/opt/edp/${service.serviceName}/conf/jmx_prometheus.yaml $HADOOP_DATANODE_OPTS"

# Export journalnode memory
<#assign journalnodeMemory=conf['hdfs.journalnode.memory']?trim?number>export JOURNALNODE_MEMORY=${journalnodeMemory?floor?c}m
export HADOOP_JOURNALNODE_OPTS="-Xmx${journalnodeMemory?floor?c}m -Dcom.sun.management.jmxremote.port=9916 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.local.only=false -javaagent:/opt/jmx_exporter/jmx_prometheus_javaagent-0.14.0.jar=5546:/opt/edp/${service.serviceName}/conf/jmx_prometheus.yaml $HADOOP_JOURNALNODE_OPTS"

export HADOOP_BALANCER_OPTS="-Xmx4096m -Dcom.sun.management.jmxremote $HADOOP_BALANCER_OPTS"

# The following applies to multiple commands (fs, dfs, fsck, distcp etc)
#export HADOOP_CLIENT_OPTS="-Xmx128m $HADOOP_CLIENT_OPTS"
#HADOOP_JAVA_PLATFORM_OPTS="-XX:-UsePerfData $HADOOP_JAVA_PLATFORM_OPTS"

# On secure datanodes, user to run the datanode as after dropping privileges
export HADOOP_SECURE_DN_USER=${r"${HADOOP_SECURE_DN_USER}"}

# Where log files are stored.  $HADOOP_HOME/logs by default.
export HADOOP_LOG_DIR=/opt/edp/${service.serviceName}/log

# Where log files are stored in the secure data environment.
export HADOOP_SECURE_DN_LOG_DIR=${r"${HADOOP_LOG_DIR}"}

# The directory where pid files are stored. /tmp by default.
export HADOOP_PID_DIR=/opt/edp/${service.serviceName}/data
export HADOOP_SECURE_DN_PID_DIR=${r"${HADOOP_PID_DIR}"}

# A string representing this instance of hadoop. $USER by default.
#export HADOOP_IDENT_STRING=$USER


# Export journalnode config
<#if service['journalnode.http-port']??>
export JOURNALNODE_HTTP_PORT=${conf['journalnode.http-port']}
export JOURNALNODE_RPC_PORT=${conf['journalnode.rpc-port']}
# export HDFS_JOURNAL_NODE_COUNT=${serviceRoles.HDFS_JOURNALNODE?size}
</#if>

# Export dfs.datanode.data.dir
export DATANODE_DATA_DIRS=/opt/edp/${service.serviceName}/data/datanode

# Export dfs.namenode.name.dir
export NAMENODE_DATA_DIRS=/opt/edp/${service.serviceName}/data/namenode

