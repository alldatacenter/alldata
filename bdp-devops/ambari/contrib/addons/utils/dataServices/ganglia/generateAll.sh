#!/bin/sh
#
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#
#

GRAPH_INFO_JSON_PATH="../../../src/dataServices/ganglia/graph_info";

JSON_PRETTY_PRINT="python -mjson.tool"

### WARNING: These PHP definitions have diverged from the actual JSON definitions
###          (which I started to modify directly), so running the scripts below
###          will result in data loss!

### php ./generate_dashboard_json.php | ${JSON_PRETTY_PRINT} > ${GRAPH_INFO_JSON_PATH}/dashboard/all.json;
### php ./generate_dashboard_hdp_json.php | ${JSON_PRETTY_PRINT} > ${GRAPH_INFO_JSON_PATH}/dashboard/custom/hdp.json;
### php ./generate_mapreduce_json.php | ${JSON_PRETTY_PRINT} > ${GRAPH_INFO_JSON_PATH}/mapreduce/all.json;
### php ./generate_mapreduce_hdp_json.php | ${JSON_PRETTY_PRINT} > ${GRAPH_INFO_JSON_PATH}/mapreduce/custom/hdp.json;
### php ./generate_hdfs_json.php | ${JSON_PRETTY_PRINT} > ${GRAPH_INFO_JSON_PATH}/hdfs/all.json;
### php ./generate_hdfs_hdp_json.php | ${JSON_PRETTY_PRINT} > ${GRAPH_INFO_JSON_PATH}/hdfs/custom/hdp.json;
