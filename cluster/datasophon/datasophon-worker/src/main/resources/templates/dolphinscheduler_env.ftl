#
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
#

# JAVA_HOME, will use it to start DolphinScheduler server
export JAVA_HOME=${r"${JAVA_HOME"}:-/usr/local/jdk1.8.0_333}

# Database related configuration, set database type, username and password
export DATABASE=${r"${DATABASE"}:-mysql}
export SPRING_PROFILES_ACTIVE=${r"${DATABASE}"}
export SPRING_DATASOURCE_URL="${databaseUrl}"
export SPRING_DATASOURCE_USERNAME=${username}
export SPRING_DATASOURCE_PASSWORD=${password}

# DolphinScheduler server related configuration
export SPRING_CACHE_TYPE=${r"${SPRING_CACHE_TYPE:-none}"}
export SPRING_JACKSON_TIME_ZONE=${r"${SPRING_JACKSON_TIME_ZONE:-UTC}"}
export MASTER_FETCH_COMMAND_NUM=${r"${MASTER_FETCH_COMMAND_NUM:-10}"}

# Registry center configuration, determines the type and link of the registry center
export REGISTRY_TYPE=${r"${REGISTRY_TYPE"}:-zookeeper}
export REGISTRY_ZOOKEEPER_CONNECT_STRING=${r"${REGISTRY_ZOOKEEPER_CONNECT_STRING"}:-${zkUrls}}

# Tasks related configurations, need to change the configuration if you use the related tasks.
export HADOOP_HOME=${r"${HADOOP_HOME"}:-/opt/datasophon/hadoop-3.3.3}
export HADOOP_CONF_DIR=${r"${HADOOP_CONF_DIR"}:-/opt/datasophon/hadoop-3.3.3/etc/hadoop}
export SPARK_HOME1=${r"${SPARK_HOME1"}:-/opt/soft/spark1}
export SPARK_HOME2=${r"${SPARK_HOME2"}:-/opt/soft/spark2}
export PYTHON_HOME=${r"${PYTHON_HOME"}:-/opt/soft/python}
export HIVE_HOME=${r"${HIVE_HOME"}:-/opt/datasophon/hive-3.1.0}
export FLINK_HOME=${r"${FLINK_HOME"}:-/opt/datasophon/flink-1.15.2}
export DATAX_HOME=${r"${DATAX_HOME"}:-/opt/soft/datax}
export SEATUNNEL_HOME=${r"${SEATUNNEL_HOME"}:-/opt/soft/seatunnel}
export CHUNJUN_HOME=${r"${CHUNJUN_HOME"}:-/opt/soft/chunjun}

export PATH=$HADOOP_HOME/bin:$SPARK_HOME1/bin:$SPARK_HOME2/bin:$PYTHON_HOME/bin:$JAVA_HOME/bin:$HIVE_HOME/bin:$FLINK_HOME/bin:$DATAX_HOME/bin:$SEATUNNEL_HOME/bin:$CHUNJUN_HOME/bin:$PATH
