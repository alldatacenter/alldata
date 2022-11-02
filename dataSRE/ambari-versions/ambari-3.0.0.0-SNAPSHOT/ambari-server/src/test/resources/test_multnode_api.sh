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

SERVER_HOST="ec2-107-20-75-170.compute-1.amazonaws.com"
declare -a AGENT_HOSTS=('test1' 'test2');

# All servers will be the first host on AGENT_HOSTS

echo curl -i -X POST -d '{"Clusters": {"version" : "HDP-1.2.0"}}' http://$SERVER_HOST:8080/api/v1/clusters/c1
echo curl -i -X POST http://$SERVER_HOST:8080/api/v1/clusters/c1/services/HDFS
echo curl -i -X POST http://$SERVER_HOST:8080/api/v1/clusters/c1/services/MAPREDUCE
echo curl -i -X POST http://$SERVER_HOST:8080/api/v1/clusters/c1/services/ZOOKEEPER
echo curl -i -X POST http://$SERVER_HOST:8080/api/v1/clusters/c1/services/HBASE
echo curl -i -X POST http://$SERVER_HOST:8080/api/v1/clusters/c1/services/GANGLIA
echo curl -i -X POST -d '{"type": "core-site", "tag": "version1", "properties" : { "fs.default.name" : "hdfs://'${AGENT_HOSTS[0]}':8020"}}' http://$SERVER_HOST:8080/api/v1/clusters/c1/configurations
echo curl -i -X POST -d '{"type": "hdfs-site", "tag": "version1", "properties" : { "dfs.datanode.data.dir.perm" : "750"}}' http://$SERVER_HOST:8080/api/v1/clusters/c1/configurations
echo curl -i -X POST -d '{"type": "global", "tag": "version1", "properties" : { "hbase_hdfs_root_dir" : "/apps/hbase/"}}' http://$SERVER_HOST:8080/api/v1/clusters/c1/configurations
echo curl -i -X POST -d '{"type": "mapred-site", "tag": "version1", "properties" : { "mapred.job.tracker" : "'${AGENT_HOSTS[0]}':50300", "mapreduce.history.server.embedded": "false", "mapreduce.history.server.http.address": "'${AGENT_HOSTS[0]}':51111"}}' http://$SERVER_HOST:8080/api/v1/clusters/c1/configurations
echo curl -i -X POST -d '{"type": "hbase-site", "tag": "version1", "properties" : { "hbase.rootdir" : "hdfs://'${AGENT_HOSTS[0]}':8020/apps/hbase/", "hbase.cluster.distributed" : "true", "hbase.zookeeper.quorum": "'${AGENT_HOSTS[0]}'", "zookeeper.session.timeout": "60000" }}' http://$SERVER_HOST:8080/api/v1/clusters/c1/configurations
echo curl -i -X POST -d '{"type": "hbase-env", "tag": "version1", "properties" : { "hbase_hdfs_root_dir" : "/apps/hbase/"}}' http://$SERVER_HOST:8080/api/v1/clusters/c1/configurations
echo curl -i -X PUT -d '{"config": {"core-site": "version1", "hdfs-site": "version1", "global" : "version1" }}'  http://$SERVER_HOST:8080/api/v1/clusters/c1/services/HDFS
echo curl -i -X PUT -d '{"config": {"core-site": "version1", "mapred-site": "version1"}}'  http://$SERVER_HOST:8080/api/v1/clusters/c1/services/MAPREDUCE
echo curl -i -X PUT -d '{"config": {"hbase-site": "version1", "hbase-env": "version1"}}'  http://$SERVER_HOST:8080/api/v1/clusters/c1/services/HBASE
echo curl -i -X POST http://$SERVER_HOST:8080/api/v1/clusters/c1/services/HDFS/components/NAMENODE
echo curl -i -X POST http://$SERVER_HOST:8080/api/v1/clusters/c1/services/HDFS/components/SECONDARY_NAMENODE
echo curl -i -X POST http://$SERVER_HOST:8080/api/v1/clusters/c1/services/HDFS/components/DATANODE
echo curl -i -X POST http://$SERVER_HOST:8080/api/v1/clusters/c1/services/HDFS/components/HDFS_CLIENT
echo curl -i -X POST http://$SERVER_HOST:8080/api/v1/clusters/c1/services/MAPREDUCE/components/JOBTRACKER
echo curl -i -X POST http://$SERVER_HOST:8080/api/v1/clusters/c1/services/MAPREDUCE/components/TASKTRACKER
echo curl -i -X POST http://$SERVER_HOST:8080/api/v1/clusters/c1/services/ZOOKEEPER/components/ZOOKEEPER_SERVER
echo curl -i -X POST http://$SERVER_HOST:8080/api/v1/clusters/c1/services/HBASE/components/HBASE_MASTER
echo curl -i -X POST http://$SERVER_HOST:8080/api/v1/clusters/c1/services/HBASE/components/HBASE_REGIONSERVER
echo curl -i -X POST http://$SERVER_HOST:8080/api/v1/clusters/c1/services/HBASE/components/HBASE_CLIENT
echo curl -i -X POST http://$SERVER_HOST:8080/api/v1/clusters/c1/services/GANGLIA/components/GANGLIA_SERVER
echo curl -i -X POST http://$SERVER_HOST:8080/api/v1/clusters/c1/services/GANGLIA/components/GANGLIA_MONITOR
echo curl -i -X POST http://$SERVER_HOST:8080/api/v1/clusters/c1/hosts/${AGENT_HOSTS[0]}
echo curl -i -X POST http://$SERVER_HOST:8080/api/v1/clusters/c1/hosts/${AGENT_HOSTS[0]}/host_components/NAMENODE
echo curl -i -X POST http://$SERVER_HOST:8080/api/v1/clusters/c1/hosts/${AGENT_HOSTS[0]}/host_components/SECONDARY_NAMENODE
echo curl -i -X POST http://$SERVER_HOST:8080/api/v1/clusters/c1/hosts/${AGENT_HOSTS[0]}/host_components/JOBTRACKER
echo curl -i -X POST http://$SERVER_HOST:8080/api/v1/clusters/c1/hosts/${AGENT_HOSTS[0]}/host_components/DATANODE
echo curl -i -X POST http://$SERVER_HOST:8080/api/v1/clusters/c1/hosts/${AGENT_HOSTS[0]}/host_components/TASKTRACKER
echo curl -i -X POST http://$SERVER_HOST:8080/api/v1/clusters/c1/hosts/${AGENT_HOSTS[0]}/host_components/ZOOKEEPER_SERVER
echo curl -i -X POST http://$SERVER_HOST:8080/api/v1/clusters/c1/hosts/${AGENT_HOSTS[0]}/host_components/GANGLIA_SERVER
echo curl -i -X POST http://$SERVER_HOST:8080/api/v1/clusters/c1/hosts/${AGENT_HOSTS[0]}/host_components/GANGLIA_MONITOR
echo curl -i -X POST http://$SERVER_HOST:8080/api/v1/clusters/c1/hosts/${AGENT_HOSTS[0]}/host_components/HDFS_CLIENT
echo curl -i -X POST http://$SERVER_HOST:8080/api/v1/clusters/c1/hosts/${AGENT_HOSTS[0]}/host_components/HBASE_MASTER
echo curl -i -X POST http://$SERVER_HOST:8080/api/v1/clusters/c1/hosts/${AGENT_HOSTS[0]}/host_components/HBASE_REGIONSERVER
echo curl -i -X POST http://$SERVER_HOST:8080/api/v1/clusters/c1/hosts/${AGENT_HOSTS[0]}/host_components/HBASE_CLIENT
echo 

len=${#AGENT_HOSTS[@]}

for (( i=1; i<$len; i++ ))
do
  curl -i -X POST http://$SERVER_HOST:8080/api/v1/clusters/c1/hosts/${AGENT_HOSTS[$i]}
  curl -i -X POST http://$SERVER_HOST:8080/api/v1/clusters/c1/hosts/${AGENT_HOSTS[$i]}/host_components/DATANODE
  curl -i -X POST http://$SERVER_HOST:8080/api/v1/clusters/c1/hosts/${AGENT_HOST[$i]}/host_components/TASKTRACKER
  curl -i -X POST http://$SERVER_HOST:8080/api/v1/clusters/c1/hosts/${AGENT_HOSTS[$i]}/host_components/GANGLIA_MONITOR
  curl -i -X POST http://$SERVER_HOST:8080/api/v1/clusters/c1/hosts/${AGENT_HOSTS[$i]}/host_components/HBASE_REGIONSERVER
done

curl -i -X PUT  -d '{"ServiceInfo": {"state" : "INSTALLED"}}'   http://$SERVER_HOST:8080/api/v1/clusters/c1/services?state=INIT
#curl -i -X PUT  -d '{"ServiceInfo": {"state" : "STARTED"}}'   http://$SERVER_HOST:8080/api/v1/clusters/c1/services?state=INSTALLED
# http://localhost:8080/api/v1/clusters/c1/requests/2
#curl -i -X PUT    http://localhost:8080/api/v1/clusters/c1/services?state="INSTALLED"
#curl -i -X POST  -d '{"ServiceInfo": {"state" : "STARTED"}}' http://localhost:8080/api/v1/clusters/c1/services/HDFS
