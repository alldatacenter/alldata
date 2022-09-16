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
ARCH_X86="x86"
ARCH_AARCH64="aarch64"
ENV_ARCH=$(uname -m)
BUILD_ARCH="aarch64"
POSTFIX="-aarch64"

PLATFORM_AARCH64="--platform linux/arm64/v8"
PLATFORM_X86="--platform linux/amd64"
USE_PLATFORM=""

SHELL_FOLDER=$(cd "$(dirname "$0")";pwd)

cd ${SHELL_FOLDER}
cd ..

USE_BUILDX=""

helpFunc() {
  cat <<EOF
Usage: ./build-arm-docker-images.sh [option]
Options:
  No option              Default build arm images on aarch64 environment.
  -x, --buildx <ARCH>    Use buildx to build docker images for another arch.
  -h, --help             Show help information.
Example:
  Use "./build-arm-docker-images.sh" to build arm images on aarch64 environment.
  Use "./build-arm-docker-images.sh --buildx" to build arm images with buildx.
EOF
}

for (( i=1; i<=$#; i++)); do
  if [ "${!i}" = "-x" ] || [ "${!i}" = "--buildx" ]; then
    NEED_BUILD=true
    USE_BUILDX="buildx"
    j=$((i+1))
    BUILD_ARCH=${!j}
    if [ "$BUILD_ARCH" != "$ARCH_AARCH64" ] && [ "$BUILD_ARCH" != "$ARCH_X86" ]; then
      echo "Wrong arch name: ${BUILD_ARCH}. Please input aarch64 or x86."
      exit 1
    fi
    if [ "$BUILD_ARCH" = "$ARCH_AARCH64" ]; then
      USE_PLATFORM="$PLATFORM_AARCH64"
    else
      USE_PLATFORM="$PLATFORM_X86"
    fi
    shift
  elif [ "${!i}" = "-h" ] || [ "${!i}" = "--help" ]; then
    helpFunc
    exit 0
  else
    echo "Wrong param: ${!i}. Please check help information."
    helpFunc
    exit 1
  fi
done

if [ "$BUILD_ARCH" = "$ARCH_X86" ] && [ "$ENV_ARCH" = "$ARCH_X86" ]; then
  mvn --batch-mode --update-snapshots -e -V clean package -DskipTests -Pdocker
  exit 0
fi

version=`awk '/<version>[^<]+<\/version>/{i++}i==2{gsub(/<version>|<\/version>/,"",$1);print $0;exit;}' pom.xml`
tag=${version}-aarch64
if [ "$BUILD_ARCH" = "$ARCH_X86" ]; then
  tag=${version}-x86
  POSTFIX="-x86"
fi

manager_dockerfile_path="inlong-manager/manager-docker/"
agent_dockerfile_path="inlong-agent/agent-docker/"
audit_dockerfile_path="inlong-audit/audit-docker/"
dataproxy_dockerfile_path="inlong-dataproxy/dataproxy-docker/"
tubemq_manager_dockerfile_path="inlong-tubemq/tubemq-docker/tubemq-manager/"
tubemq_all_dockerfile_path="inlong-tubemq/tubemq-docker/tubemq-all/"

manager_tarball_name="apache-inlong-manager-web-${version}-bin.tar.gz"
agent_tarball_name="apache-inlong-agent-${version}-bin.tar.gz"
audit_tarball_name="apache-inlong-audit-${version}-bin.tar.gz"
dataproxy_tarball_name="apache-inlong-dataproxy-${version}-bin.tar.gz"
dashboard_file_name="build"
tubemq_manager_tarball_name="apache-inlong-tubemq-manager-${version}-bin.tar.gz"
tubemq_all_tarball_name="apache-inlong-tubemq-server-${version}-bin.tar.gz"
manager_target_tarball_name="manager-web-${version}-bin.tar.gz"

manager_tarball="inlong-manager/manager-web/target/${manager_tarball_name}"
agent_tarball="inlong-agent/agent-release/target/${agent_tarball_name}"
audit_tarball="inlong-audit/audit-release/target/${audit_tarball_name}"
dataproxy_tarball="inlong-dataproxy/dataproxy-dist/target/${dataproxy_tarball_name}"
tubemq_manager_tarball="inlong-tubemq/tubemq-manager/target/${tubemq_manager_tarball_name}"
tubemq_all_tarball="inlong-tubemq/tubemq-server/target/${tubemq_all_tarball_name}"

DATAPROXY_TARBALL="target/${dataproxy_tarball_name}"
AUDIT_TARBALL="target/${audit_tarball_name}"
TUBEMQ_MANAGER_TARBALL="target/${tubemq_manager_tarball_name}"
DASHBOARD_FILE="${dashboard_file_name}"
AGENT_TARBALL="target/${agent_tarball_name}"
TUBEMQ_TARBALL="target/${tubemq_all_tarball_name}"

cp ${manager_tarball} ${manager_dockerfile_path}/target/${manager_target_tarball_name}
cp inlong-sort/sort-dist/target/sort-dist-${version}.jar ${manager_dockerfile_path}/target/
cp inlong-sort/sort-connectors/pulsar/target/sort-connector-pulsar-${version}.jar ${manager_dockerfile_path}/target/
cp inlong-sort/sort-connectors/jdbc/target/sort-connector-jdbc-${version}.jar ${manager_dockerfile_path}/target/
cp inlong-sort/sort-connectors/hive/target/sort-connector-hive-${version}.jar ${manager_dockerfile_path}/target/
cp ${agent_tarball} ${agent_dockerfile_path}/target/${agent_tarball_name}
cp ${audit_tarball} ${audit_dockerfile_path}/target/${audit_tarball_name}
cp ${dataproxy_tarball} ${dataproxy_dockerfile_path}/target/${dataproxy_tarball_name}
cp ${tubemq_manager_tarball} ${tubemq_manager_dockerfile_path}/target/${tubemq_manager_tarball_name}
if [ "$BUILD_ARCH" = "$ARCH_X86" ]; then
  cp ${tubemq_all_tarball} ${tubemq_all_dockerfile_path}/target/${tubemq_all_tarball_name}
fi

docker ${USE_BUILDX} build ${USE_PLATFORM} -t inlong/manager:${tag}        inlong-manager/manager-docker/      --build-arg VERSION=${version}
docker ${USE_BUILDX} build ${USE_PLATFORM} -t inlong/dataproxy:${tag}      inlong-dataproxy/dataproxy-docker/  --build-arg DATAPROXY_TARBALL=${DATAPROXY_TARBALL}
docker ${USE_BUILDX} build ${USE_PLATFORM} -t inlong/audit:${tag}          inlong-audit/audit-docker/          --build-arg AUDIT_TARBALL=${AUDIT_TARBALL}
docker ${USE_BUILDX} build ${USE_PLATFORM} -t inlong/tubemq-manager:${tag} inlong-tubemq/tubemq-docker/tubemq-manager/ --build-arg TUBEMQ_MANAGER_TARBALL=${TUBEMQ_MANAGER_TARBALL}
docker ${USE_BUILDX} build ${USE_PLATFORM} -t inlong/dashboard:${tag}      inlong-dashboard/                   --build-arg DASHBOARD_FILE=${DASHBOARD_FILE}
docker ${USE_BUILDX} build ${USE_PLATFORM} -t inlong/agent:${tag}          inlong-agent/agent-docker/          --build-arg AGENT_TARBALL=${AGENT_TARBALL}
if [ "$BUILD_ARCH" = "$ARCH_X86" ]; then
  docker ${USE_BUILDX} build ${USE_PLATFORM} -t inlong/tubemq-all:${tag}    inlong-tubemq/tubemq-docker/tubemq-all/ --build-arg TUBEMQ_TARBALL=${TUBEMQ_TARBALL}
  docker ${USE_BUILDX} build ${USE_PLATFORM} -t inlong/tubemq-cpp:${tag}    inlong-tubemq/tubemq-docker/tubemq-cpp/
  docker ${USE_BUILDX} build ${USE_PLATFORM} -t inlong/tubemq-build:${tag}  inlong-tubemq/tubemq-docker/tubemq-build/
fi

docker tag inlong/manager:${tag}         inlong/manager:latest${POSTFIX}
docker tag inlong/dataproxy:${tag}       inlong/dataproxy:latest${POSTFIX}
docker tag inlong/audit:${tag}           inlong/audit:latest${POSTFIX}
docker tag inlong/tubemq-manager:${tag}  inlong/tubemq-manager:latest${POSTFIX}
docker tag inlong/dashboard:${tag}       inlong/dashboard:latest${POSTFIX}
docker tag inlong/agent:${tag}           inlong/agent:latest${POSTFIX}
if [ "$BUILD_ARCH" = "$ARCH_X86" ]; then
  docker tag inlong/tubemq-cpp:${tag}   inlong/tubemq-cpp:latest${POSTFIX}
  docker tag inlong/tubemq-build:${tag} inlong/tubemq-build:latest${POSTFIX}
  docker tag inlong/tubemq-all:${tag}   inlong/tubemq-all:latest${POSTFIX}
fi
