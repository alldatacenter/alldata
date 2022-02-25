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

curl -i -X POST http://localhost:8080/api/clusters/c1/services/HBASE
curl -i -X POST http://localhost:8080/api/clusters/c1/services/HBASE/components/HBASE_MASTER
curl -i -X POST http://localhost:8080/api/clusters/c1/services/HBASE/components/HBASE_REGIONSERVER
curl -i -X POST http://localhost:8080/api/clusters/c1/services/HBASE/components/HBASE_CLIENT
curl -i -X POST http://localhost:8080/api/clusters/c1/hosts/localhost.localdomain/host_components/HBASE_MASTER
curl -i -X POST http://localhost:8080/api/clusters/c1/hosts/localhost.localdomain/host_components/HBASE_REGIONSERVER
curl -i -X POST http://localhost:8080/api/clusters/c1/hosts/localhost.localdomain/host_components/HBASE_CLIENT
curl -i -X POST -d '{"type": "hbase-site", "tag": "version1", "properties" : { "hbase.rootdir" : "hdfs://localhost:8020/apps/hbase/", "hbase.cluster.distributed" : "true", "hbase.zookeeper.quorum": "localhost", "zookeeper.session.timeout": "60000" }}' http://localhost:8080/api/clusters/c1/configurations
curl -i -X POST -d '{"type": "hbase-env", "tag": "version1", "properties" : { "hbase_hdfs_root_dir" : "/apps/hbase/"}}' http://localhost:8080/api/clusters/c1/configurations
curl -i -X PUT -d '{"config": {"hbase-site": "version1", "hbase-env": "version1"}}'  http://localhost:8080/api/clusters/c1/services/HBASE
curl -i -X PUT  -d '{"ServiceInfo": {"state" : "INSTALLED"}}' http://localhost:8080/api/clusters/c1/services/HBASE/
