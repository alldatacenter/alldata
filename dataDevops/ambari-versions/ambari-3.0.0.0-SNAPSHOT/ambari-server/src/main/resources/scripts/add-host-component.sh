#!/bin/sh
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

CLUSTER_NAME=$1
HOST_NAME=$2
COMPONENT_NAME=$3

curl -i -u admin:admin -X POST -d "{
\"host_components\": [
{\"HostRoles\" : { \"component_name\": \"${COMPONENT_NAME}\"} }
]}" http://localhost:8080/api/v1/clusters/${CLUSTER_NAME}/hosts?Hosts/host_name=${HOST_NAME}

curl -i -u admin:admin -X PUT -d '{ "HostRoles": {"state": "INSTALLED"
} }' http://localhost:8080/api/v1/clusters/${CLUSTER_NAME}/host_components?HostRoles/host_name=${HOST_NAME}\&HostRoles/component_name=${COMPONENT_NAME}\&HostRoles/state=INIT

