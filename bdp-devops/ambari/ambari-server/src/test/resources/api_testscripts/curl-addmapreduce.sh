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

curl -i -X POST http://localhost:8080/api/clusters/c1/services/MAPREDUCE
curl -i -X POST -d '{"type": "core-site", "tag": "version2", "properties" : { "fs.default.name" : "localhost:8020"}}' http://localhost:8080/api/clusters/c1/configurations
curl -i -X POST -d '{"type": "mapred-site", "tag": "version1", "properties" : { "mapred.job.tracker" : "localhost:50300", "mapreduce.history.server.embedded": "false", "mapreduce.history.server.http.address": "localhost:51111"}}' http://localhost:8080/api/clusters/c1/configurations
curl -i -X PUT -d '{"config": {"core-site": "version2", "mapred-site": "version1"}}'  http://localhost:8080/api/clusters/c1/services/MAPREDUCE
curl -i -X POST http://localhost:8080/api/clusters/c1/services/MAPREDUCE/components/JOBTRACKER
curl -i -X POST http://localhost:8080/api/clusters/c1/services/MAPREDUCE/components/TASKTRACKER
curl -i -X POST http://localhost:8080/api/clusters/c1/hosts/localhost.localdomain/host_components/JOBTRACKER
curl -i -X POST http://localhost:8080/api/clusters/c1/hosts/localhost.localdomain/host_components/TASKTRACKER
curl -i -X PUT  -d '{"ServiceInfo": {"state" : "INSTALLED"}}'   http://localhost:8080/api/clusters/c1/services/MAPREDUCE/
