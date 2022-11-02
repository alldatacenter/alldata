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

AGENT_HOST="localhost.localdomain"
curl -i -X POST -d '{"Clusters": {"version" : "HDP-1.2.0"}}' http://localhost:8080/api/v1/clusters/c1
curl -i -X POST http://localhost:8080/api/v1/clusters/c1/services/HDFS
curl -i -X POST http://localhost:8080/api/v1/clusters/c1/services/MAPREDUCE
curl -i -X POST http://localhost:8080/api/v1/clusters/c1/services/ZOOKEEPER
curl -i -X POST http://localhost:8080/api/v1/clusters/c1/services/HBASE
curl -i -X POST http://localhost:8080/api/v1/clusters/c1/services/GANGLIA

curl -i -X PUT -d '{"Clusters": {"desired_config": {"type": "core-site", "tag": "version1", "properties" : { "fs.default.name" : "localhost:8020"}}}}' http://localhost:8080/api/v1/clusters/c1
curl -i -X PUT -d '{"Clusters": {"desired_config": {"type": "core-site", "tag": "version2", "properties" : { "fs.default.name" : "localhost:8020"}}}}' http://localhost:8080/api/v1/clusters/c1
curl -i -X PUT -d '{"Clusters": {"desired_config": {"type": "core-site", "tag": "version1"}}}' http://localhost:8080/api/v1/clusters/c1
curl -i -X PUT -d '{"Clusters": {"desired_config": {"type": "hdfs-site", "tag": "version1", "properties" : { "dfs.datanode.data.dir.perm" : "750"}}}}' http://localhost:8080/api/v1/clusters/c1
curl -i -X PUT -d '{"Clusters": {"desired_config": {"type": "global", "tag": "version1", "properties" : { "hbase_hdfs_root_dir" : "/apps/hbase/"}}}}' http://localhost:8080/api/v1/clusters/c1
curl -i -X PUT -d '{"Clusters": {"desired_config": {"type": "mapred-site", "tag": "version1", "properties" : { "mapred.job.tracker" : "localhost:50300", "mapreduce.history.server.embedded": "false", "mapreduce.history.server.http.address": "localhost:51111"}}}}' http://localhost:8080/api/v1/clusters/c1
curl -i -X PUT -d '{"Clusters": {"desired_config": {"type": "hbase-site", "tag": "version1", "properties" : { "hbase.rootdir" : "hdfs://localhost:8020/apps/hbase/", "hbase.cluster.distributed" : "true", "hbase.zookeeper.quorum": "localhost", "zookeeper.session.timeout": "60000" }}}}' http://localhost:8080/api/v1/clusters/c1
curl -i -X PUT -d '{"Clusters": {"desired_config": {"type": "hbase-env", "tag": "version1", "properties" : { "hbase_hdfs_root_dir" : "/apps/hbase/"}}}}' http://localhost:8080/api/v1/clusters/c1

curl -i -X POST http://localhost:8080/api/v1/clusters/c1/services/HDFS/components/NAMENODE
curl -i -X POST http://localhost:8080/api/v1/clusters/c1/services/HDFS/components/SECONDARY_NAMENODE
curl -i -X POST http://localhost:8080/api/v1/clusters/c1/services/HDFS/components/DATANODE
curl -i -X POST http://localhost:8080/api/v1/clusters/c1/services/HDFS/components/HDFS_CLIENT
curl -i -X POST http://localhost:8080/api/v1/clusters/c1/services/MAPREDUCE/components/JOBTRACKER
curl -i -X POST http://localhost:8080/api/v1/clusters/c1/services/MAPREDUCE/components/TASKTRACKER
curl -i -X POST http://localhost:8080/api/v1/clusters/c1/services/ZOOKEEPER/components/ZOOKEEPER_SERVER
curl -i -X POST http://localhost:8080/api/v1/clusters/c1/services/HBASE/components/HBASE_MASTER
curl -i -X POST http://localhost:8080/api/v1/clusters/c1/services/HBASE/components/HBASE_REGIONSERVER
curl -i -X POST http://localhost:8080/api/v1/clusters/c1/services/HBASE/components/HBASE_CLIENT
curl -i -X POST http://localhost:8080/api/v1/clusters/c1/services/GANGLIA/components/GANGLIA_SERVER
curl -i -X POST http://localhost:8080/api/v1/clusters/c1/services/GANGLIA/components/GANGLIA_MONITOR
curl -i -X POST http://localhost:8080/api/v1/clusters/c1/hosts/$AGENT_HOST
curl -i -X POST http://localhost:8080/api/v1/clusters/c1/hosts/$AGENT_HOST/host_components/NAMENODE
curl -i -X POST http://localhost:8080/api/v1/clusters/c1/hosts/$AGENT_HOST/host_components/SECONDARY_NAMENODE
curl -i -X POST http://localhost:8080/api/v1/clusters/c1/hosts/$AGENT_HOST/host_components/JOBTRACKER
curl -i -X POST http://localhost:8080/api/v1/clusters/c1/hosts/$AGENT_HOST/host_components/DATANODE
curl -i -X POST http://localhost:8080/api/v1/clusters/c1/hosts/$AGENT_HOST/host_components/TASKTRACKER
curl -i -X POST http://localhost:8080/api/v1/clusters/c1/hosts/$AGENT_HOST/host_components/ZOOKEEPER_SERVER
curl -i -X POST http://localhost:8080/api/v1/clusters/c1/hosts/$AGENT_HOST/host_components/GANGLIA_SERVER
curl -i -X POST http://localhost:8080/api/v1/clusters/c1/hosts/$AGENT_HOST/host_components/GANGLIA_MONITOR
curl -i -X POST http://localhost:8080/api/v1/clusters/c1/hosts/$AGENT_HOST/host_components/HDFS_CLIENT
curl -i -X POST http://localhost:8080/api/v1/clusters/c1/hosts/$AGENT_HOST/host_components/HBASE_MASTER
curl -i -X POST http://localhost:8080/api/v1/clusters/c1/hosts/$AGENT_HOST/host_components/HBASE_REGIONSERVER
curl -i -X POST http://localhost:8080/api/v1/clusters/c1/hosts/$AGENT_HOST/host_components/HBASE_CLIENT
curl -i -X PUT  -d '{"ServiceInfo": {"state" : "INSTALLED"}}'   http://localhost:8080/api/v1/clusters/c1/services?state=INIT
#curl -i -X PUT  -d '{"ServiceInfo": {"state" : "STARTED"}}'   http://localhost:8080/api/v1/clusters/c1/services?state=INSTALLED
# http://localhost:8080/api/v1/clusters/c1/requests/2
#curl -i -X PUT    http://localhost:8080/api/v1/clusters/c1/services?state="INSTALLED"
#curl -i -X POST  -d '{"ServiceInfo": {"state" : "STARTED"}}' http://localhost:8080/api/v1/clusters/c1/services/HDFS
