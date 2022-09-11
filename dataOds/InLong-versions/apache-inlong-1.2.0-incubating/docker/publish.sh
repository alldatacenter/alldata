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
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#

if [ -z "$DOCKER_USER" ]; then
    echo "Docker user in variable \$DOCKER_USER was not set. Skipping image publishing"
    exit 1
fi

if [ -z "$DOCKER_PASSWORD" ]; then
    echo "Docker password in variable \$DOCKER_PASSWORD was not set. Skipping image publishing"
    exit 1
fi

DOCKER_ORG="${DOCKER_ORG:-inlong}"

echo $DOCKER_PASSWORD | docker login ${DOCKER_REGISTRY} -u="$DOCKER_USER" --password-stdin
if [ $? -ne 0 ]; then
    echo "Failed to login to ${DOCKER_REGISTRY}"
    exit 1
fi

MVN_VERSION=`python ./get-project-version.py`
echo "InLong version: ${MVN_VERSION}"

if [[ -z ${DOCKER_REGISTRY} ]]; then
    docker_registry_org=${DOCKER_ORG}
else
    docker_registry_org=${DOCKER_REGISTRY}/${DOCKER_ORG}
    echo "Starting to push images to ${docker_registry_org}..."
fi

set -x

# Fail if any of the subsequent commands fail
set -e

# tag all images
docker tag inlong/manager:1.2.0-incubating     ${docker_registry_org}/manager:1.2.0-incubating
docker tag inlong/agent:1.2.0-incubating           ${docker_registry_org}/agent:1.2.0-incubating
docker tag inlong/dataproxy:1.2.0-incubating       ${docker_registry_org}/dataproxy:1.2.0-incubating
docker tag inlong/tubemq-manager:1.2.0-incubating  ${docker_registry_org}/tubemq-manager:1.2.0-incubating
docker tag inlong/tubemq-all:1.2.0-incubating      ${docker_registry_org}/tubemq-all:1.2.0-incubating
docker tag inlong/tubemq-build:1.2.0-incubating    ${docker_registry_org}/tubemq-build:1.2.0-incubating
docker tag inlong/dashboard:1.2.0-incubating         ${docker_registry_org}/dashboard:1.2.0-incubating
docker tag inlong/tubemq-cpp:1.2.0-incubating      ${docker_registry_org}/tubemq-cpp:1.2.0-incubating
docker tag inlong/audit:1.2.0-incubating      ${docker_registry_org}/audit:1.2.0-incubating

docker tag inlong/manager:$MVN_VERSION     ${docker_registry_org}/manager:$MVN_VERSION
docker tag inlong/agent:$MVN_VERSION           ${docker_registry_org}/agent:$MVN_VERSION
docker tag inlong/dataproxy:$MVN_VERSION       ${docker_registry_org}/dataproxy:$MVN_VERSION
docker tag inlong/tubemq-manager:$MVN_VERSION  ${docker_registry_org}/tubemq-manager:$MVN_VERSION
docker tag inlong/tubemq-all:$MVN_VERSION      ${docker_registry_org}/tubemq-all:$MVN_VERSION
docker tag inlong/tubemq-build:$MVN_VERSION    ${docker_registry_org}/tubemq-build:$MVN_VERSION
docker tag inlong/dashboard:$MVN_VERSION         ${docker_registry_org}/dashboard:$MVN_VERSION
docker tag inlong/tubemq-cpp:$MVN_VERSION      ${docker_registry_org}/tubemq-cpp:$MVN_VERSION
docker tag inlong/audit:$MVN_VERSION      ${docker_registry_org}/audit:$MVN_VERSION

# Push all images and tags
docker push inlong/manager:1.2.0-incubating
docker push inlong/agent:1.2.0-incubating
docker push inlong/dataproxy:1.2.0-incubating
docker push inlong/tubemq-manager:1.2.0-incubating
docker push inlong/tubemq-all:1.2.0-incubating
docker push inlong/tubemq-build:1.2.0-incubating
docker push inlong/dashboard:1.2.0-incubating
docker push inlong/tubemq-cpp:1.2.0-incubating
docker push inlong/audit:1.2.0-incubating

docker push inlong/manager:$MVN_VERSION
docker push inlong/agent:$MVN_VERSION
docker push inlong/dataproxy:$MVN_VERSION
docker push inlong/tubemq-manager:$MVN_VERSION
docker push inlong/tubemq-all:$MVN_VERSION
docker push inlong/tubemq-build:$MVN_VERSION
docker push inlong/dashboard:$MVN_VERSION
docker push inlong/tubemq-cpp:$MVN_VERSION
docker push inlong/audit:$MVN_VERSION

echo "Finished pushing images to ${docker_registry_org}"
