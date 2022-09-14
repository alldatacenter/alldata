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
BUILD_ARCH=""

NEED_BUILD=false
NEED_TAG=false
NEED_PUBLISH=false
NEED_MANIFEST=false
USE_BUILDX=""

SRC_POSTFIX=""
DES_POSTFIX="-x86"

SHELL_FOLDER=$(cd "$(dirname "$0")";pwd)
MVN_VERSION=$(python ${SHELL_FOLDER}/get-project-version.py)

buildImage() {
  echo "Start building images"
  cd "${SHELL_FOLDER}"
  cd ..
  if [ "$BUILD_ARCH" = "$ARCH_X86" ] && [ "$ENV_ARCH" = "$ARCH_X86" ]; then
    mvn --batch-mode --update-snapshots -e -V clean package -DskipTests -Pdocker
  else
    mvn --batch-mode --update-snapshots -e -V clean package -DskipTests
    sh ./docker/build-docker-images.sh ${USE_BUILDX} ${BUILD_ARCH}
  fi
  echo "End building images"
}

initTagImageForx86() {
  SRC_POSTFIX=""
  DES_POSTFIX="-x86"
  docker tag inlong/manager:latest${SRC_POSTFIX}         inlong/manager:latest${DES_POSTFIX}
  docker tag inlong/agent:latest${SRC_POSTFIX}           inlong/agent:latest${DES_POSTFIX}
  docker tag inlong/dataproxy:latest${SRC_POSTFIX}       inlong/dataproxy:latest${DES_POSTFIX}
  docker tag inlong/tubemq-manager:latest${SRC_POSTFIX}  inlong/tubemq-manager:latest${DES_POSTFIX}
  docker tag inlong/tubemq-all:latest${SRC_POSTFIX}      inlong/tubemq-all:latest${DES_POSTFIX}
  docker tag inlong/tubemq-build:latest${SRC_POSTFIX}    inlong/tubemq-build:latest${DES_POSTFIX}
  docker tag inlong/dashboard:latest${SRC_POSTFIX}       inlong/dashboard:latest${DES_POSTFIX}
  docker tag inlong/tubemq-cpp:latest${SRC_POSTFIX}      inlong/tubemq-cpp:latest${DES_POSTFIX}
  docker tag inlong/audit:latest${SRC_POSTFIX}           inlong/audit:latest${DES_POSTFIX}

  docker tag inlong/manager:${MVN_VERSION}${SRC_POSTFIX}         inlong/manager:${MVN_VERSION}${DES_POSTFIX}
  docker tag inlong/agent:${MVN_VERSION}${SRC_POSTFIX}           inlong/agent:${MVN_VERSION}${DES_POSTFIX}
  docker tag inlong/dataproxy:${MVN_VERSION}${SRC_POSTFIX}       inlong/dataproxy:${MVN_VERSION}${DES_POSTFIX}
  docker tag inlong/tubemq-manager:${MVN_VERSION}${SRC_POSTFIX}  inlong/tubemq-manager:${MVN_VERSION}${DES_POSTFIX}
  docker tag inlong/tubemq-all:${MVN_VERSION}${SRC_POSTFIX}      inlong/tubemq-all:${MVN_VERSION}${DES_POSTFIX}
  docker tag inlong/tubemq-build:${MVN_VERSION}${SRC_POSTFIX}    inlong/tubemq-build:${MVN_VERSION}${DES_POSTFIX}
  docker tag inlong/dashboard:${MVN_VERSION}${SRC_POSTFIX}       inlong/dashboard:${MVN_VERSION}${DES_POSTFIX}
  docker tag inlong/tubemq-cpp:${MVN_VERSION}${SRC_POSTFIX}      inlong/tubemq-cpp:${MVN_VERSION}${DES_POSTFIX}
  docker tag inlong/audit:${MVN_VERSION}${SRC_POSTFIX}           inlong/audit:${MVN_VERSION}${DES_POSTFIX}
}

tagImage() {
  echo "Start tagging images"
  if [[ -z ${DOCKER_REGISTRY} ]]; then
    docker_registry_org=${DOCKER_ORG}
  else
    docker_registry_org=${DOCKER_REGISTRY}/${DOCKER_ORG}
  fi
  if [ "$BUILD_ARCH" = "$ARCH_AARCH64" ]; then
    SRC_POSTFIX="-aarch64"
    DES_POSTFIX="-aarch64"
  else
    initTagImageForx86
    SRC_POSTFIX="-x86"
    DES_POSTFIX="-x86"
  fi

  docker tag inlong/manager:latest${SRC_POSTFIX}         ${docker_registry_org}/manager:latest${DES_POSTFIX}
  docker tag inlong/agent:latest${SRC_POSTFIX}           ${docker_registry_org}/agent:latest${DES_POSTFIX}
  docker tag inlong/dataproxy:latest${SRC_POSTFIX}       ${docker_registry_org}/dataproxy:latest${DES_POSTFIX}
  docker tag inlong/tubemq-manager:latest${SRC_POSTFIX}  ${docker_registry_org}/tubemq-manager:latest${DES_POSTFIX}
  docker tag inlong/dashboard:latest${SRC_POSTFIX}       ${docker_registry_org}/dashboard:latest${DES_POSTFIX}
  docker tag inlong/audit:latest${SRC_POSTFIX}           ${docker_registry_org}/audit:latest${DES_POSTFIX}
  if [ "$BUILD_ARCH" = "$ARCH_X86" ]; then
    docker tag inlong/tubemq-cpp:latest${SRC_POSTFIX}      ${docker_registry_org}/tubemq-cpp:latest${DES_POSTFIX}
    docker tag inlong/tubemq-all:latest${SRC_POSTFIX}      ${docker_registry_org}/tubemq-all:latest${DES_POSTFIX}
    docker tag inlong/tubemq-build:latest${SRC_POSTFIX}    ${docker_registry_org}/tubemq-build:latest${DES_POSTFIX}
  fi

  docker tag inlong/manager:${MVN_VERSION}${SRC_POSTFIX}         ${docker_registry_org}/manager:${MVN_VERSION}${DES_POSTFIX}
  docker tag inlong/agent:${MVN_VERSION}${SRC_POSTFIX}           ${docker_registry_org}/agent:${MVN_VERSION}${DES_POSTFIX}
  docker tag inlong/dataproxy:${MVN_VERSION}${SRC_POSTFIX}       ${docker_registry_org}/dataproxy:${MVN_VERSION}${DES_POSTFIX}
  docker tag inlong/tubemq-manager:${MVN_VERSION}${SRC_POSTFIX}  ${docker_registry_org}/tubemq-manager:${MVN_VERSION}${DES_POSTFIX}
  docker tag inlong/dashboard:${MVN_VERSION}${SRC_POSTFIX}       ${docker_registry_org}/dashboard:${MVN_VERSION}${DES_POSTFIX}
  docker tag inlong/audit:${MVN_VERSION}${SRC_POSTFIX}           ${docker_registry_org}/audit:${MVN_VERSION}${DES_POSTFIX}
  if [ "$BUILD_ARCH" = "$ARCH_X86" ]; then
    docker tag inlong/tubemq-cpp:${MVN_VERSION}${SRC_POSTFIX}      ${docker_registry_org}/tubemq-cpp:${MVN_VERSION}${DES_POSTFIX}
    docker tag inlong/tubemq-all:${MVN_VERSION}${SRC_POSTFIX}      ${docker_registry_org}/tubemq-all:${MVN_VERSION}${DES_POSTFIX}
    docker tag inlong/tubemq-build:${MVN_VERSION}${SRC_POSTFIX}    ${docker_registry_org}/tubemq-build:${MVN_VERSION}${DES_POSTFIX}
  fi
  echo "End tagging images"
}

publishImages() {
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

  set -x

  set -e

  pushImage
}

pushDefaultImage() {
  docker push inlong/manager:latest
  docker push inlong/agent:latest
  docker push inlong/dataproxy:latest
  docker push inlong/tubemq-manager:latest
  docker push inlong/tubemq-all:latest
  docker push inlong/tubemq-build:latest
  docker push inlong/dashboard:latest
  docker push inlong/tubemq-cpp:latest
  docker push inlong/audit:latest

  docker push inlong/manager:${MVN_VERSION}
  docker push inlong/agent:${MVN_VERSION}
  docker push inlong/dataproxy:${MVN_VERSION}
  docker push inlong/tubemq-manager:${MVN_VERSION}
  docker push inlong/tubemq-all:${MVN_VERSION}
  docker push inlong/tubemq-build:${MVN_VERSION}
  docker push inlong/dashboard:${MVN_VERSION}
  docker push inlong/tubemq-cpp:${MVN_VERSION}
  docker push inlong/audit:${MVN_VERSION}
}

pushImage() {
  if [[ -z ${DOCKER_REGISTRY} ]]; then
      docker_registry_org=${DOCKER_ORG}
  else
      docker_registry_org=${DOCKER_REGISTRY}/${DOCKER_ORG}
  fi
  echo "Start pushing images to ${docker_registry_org}"

  SRC_POSTFIX=""
  if [ "$BUILD_ARCH" = "$ARCH_AARCH64" ]; then
    SRC_POSTFIX="-aarch64"
  elif [ "$NEED_TAG" = true ]; then
    pushDefaultImage
    SRC_POSTFIX="-x86"
  fi

  docker push inlong/manager:latest${SRC_POSTFIX}
  docker push inlong/agent:latest${SRC_POSTFIX}
  docker push inlong/dataproxy:latest${SRC_POSTFIX}
  docker push inlong/tubemq-manager:latest${SRC_POSTFIX}
  docker push inlong/dashboard:latest${SRC_POSTFIX}
  docker push inlong/audit:latest${SRC_POSTFIX}
  if [ "$BUILD_ARCH" = "$ARCH_X86" ]; then
    docker push inlong/tubemq-build:latest${SRC_POSTFIX}
    docker push inlong/tubemq-all:latest${SRC_POSTFIX}
    docker push inlong/tubemq-cpp:latest${SRC_POSTFIX}
  fi

  docker push inlong/manager:${MVN_VERSION}${SRC_POSTFIX}
  docker push inlong/agent:${MVN_VERSION}${SRC_POSTFIX}
  docker push inlong/dataproxy:${MVN_VERSION}${SRC_POSTFIX}
  docker push inlong/tubemq-manager:${MVN_VERSION}${SRC_POSTFIX}
  docker push inlong/dashboard:${MVN_VERSION}${SRC_POSTFIX}
  docker push inlong/audit:${MVN_VERSION}${SRC_POSTFIX}
  if [ "$BUILD_ARCH" = "$ARCH_X86" ]; then
    docker push inlong/tubemq-all:${MVN_VERSION}${SRC_POSTFIX}
    docker push inlong/tubemq-build:${MVN_VERSION}${SRC_POSTFIX}
    docker push inlong/tubemq-cpp:${MVN_VERSION}${SRC_POSTFIX}
  fi

  echo "Finished pushing images to inlong"
}

pushManifest() {
  echo "Start pushing manifest ..."
  docker manifest create --insecure --amend inlong/manager:latest        inlong/manager:latest-aarch64    inlong/manager:latest-x86
  docker manifest create --insecure --amend inlong/agent:latest          inlong/agent:latest-aarch64      inlong/agent:latest-x86
  docker manifest create --insecure --amend inlong/dataproxy:latest      inlong/dataproxy:latest-aarch64  inlong/dataproxy:latest-x86
  docker manifest create --insecure --amend inlong/dashboard:latest      inlong/dashboard:latest-aarch64  inlong/dashboard:latest-x86
  docker manifest create --insecure --amend inlong/audit:latest          inlong/audit:latest-aarch64      inlong/audit:latest-x86
  docker manifest create --insecure --amend inlong/tubemq-cpp:latest     inlong/tubemq-cpp:latest-x86
  docker manifest create --insecure --amend inlong/tubemq-manager:latest inlong/tubemq-manager:latest-x86
  docker manifest create --insecure --amend inlong/tubemq-all:latest     inlong/tubemq-all:latest-x86
  docker manifest create --insecure --amend inlong/tubemq-build:latest   inlong/tubemq-build:latest-x86

  docker manifest push inlong/manager:latest
  docker manifest push inlong/agent:latest
  docker manifest push inlong/dataproxy:latest
  docker manifest push inlong/tubemq-manager:latest
  docker manifest push inlong/tubemq-all:latest
  docker manifest push inlong/tubemq-build:latest
  docker manifest push inlong/dashboard:latest
  docker manifest push inlong/tubemq-cpp:latest
  docker manifest push inlong/audit:latest

  docker manifest create --insecure --amend inlong/manager:${MVN_VERSION}        inlong/manager:${MVN_VERSION}-aarch64    inlong/manager:${MVN_VERSION}-x86
  docker manifest create --insecure --amend inlong/agent:${MVN_VERSION}          inlong/agent:${MVN_VERSION}-aarch64      inlong/agent:${MVN_VERSION}-x86
  docker manifest create --insecure --amend inlong/dataproxy:${MVN_VERSION}      inlong/dataproxy:${MVN_VERSION}-aarch64  inlong/dataproxy:${MVN_VERSION}-x86
  docker manifest create --insecure --amend inlong/dashboard:${MVN_VERSION}      inlong/dashboard:${MVN_VERSION}-aarch64  inlong/dashboard:${MVN_VERSION}-x86
  docker manifest create --insecure --amend inlong/audit:${MVN_VERSION}          inlong/audit:${MVN_VERSION}-aarch64      inlong/audit:${MVN_VERSION}-x86
  docker manifest create --insecure --amend inlong/tubemq-manager:${MVN_VERSION} inlong/tubemq-manager:${MVN_VERSION}-x86
  docker manifest create --insecure --amend inlong/tubemq-all:${MVN_VERSION}     inlong/tubemq-all:${MVN_VERSION}-x86
  docker manifest create --insecure --amend inlong/tubemq-build:${MVN_VERSION}   inlong/tubemq-build:${MVN_VERSION}-x86
  docker manifest create --insecure --amend inlong/tubemq-cpp:${MVN_VERSION}     inlong/tubemq-cpp:${MVN_VERSION}-x86

  docker manifest push inlong/manager:${MVN_VERSION}
  docker manifest push inlong/agent:${MVN_VERSION}
  docker manifest push inlong/dataproxy:${MVN_VERSION}
  docker manifest push inlong/tubemq-manager:${MVN_VERSION}
  docker manifest push inlong/tubemq-all:${MVN_VERSION}
  docker manifest push inlong/tubemq-build:${MVN_VERSION}
  docker manifest push inlong/dashboard:${MVN_VERSION}
  docker manifest push inlong/tubemq-cpp:${MVN_VERSION}
  docker manifest push inlong/audit:${MVN_VERSION}

  echo "End pushing manifest"
}

helpFunc() {
  cat <<EOF
Usage: ./publish-by-arch.sh [option]
Options:
  -b, --build           Add build operation before publish. Build docker images by arch.
  -t, --tag             Add tag operation before publish. Add arch after version and add docker registry org.
  -p, --publish         Publish images according to docker registry information.
  -m, --manifest        Push manifest. This option doesn't need arch.
  -a, --arch <ARCH>     Use buildx to build docker images for another arch.
                        Arch must be provided, as aarch64 or x86.
  -h, --help            Show help information.
Example:
  Use "./publish-by-arch.sh -b" to publish arm images after build operation.
  Use "./publish-by-arch.sh -t" to publish amd images after tag already x86 images as x86.
  Use "./publish-by-arch.sh -x -p" to build arm docker images on x86 environment, then publish.
EOF
}

for (( i=1; i<="$#"; i++)); do
  if [ "${!i}" = "-b" ] || [ "${!i}" = "--build" ]; then
    NEED_BUILD=true
  elif [ "${!i}" = "-t" ] || [ "${!i}" = "--tag" ]; then
    NEED_TAG=true
  elif [ "${!i}" = "-m" ] || [ "${!i}" = "--manifest" ]; then
    NEED_MANIFEST=true
  elif [ "${!i}" = "-p" ] || [ "${!i}" = "--publish" ]; then
    NEED_PUBLISH=true
  elif [ "${!i}" = "-a" ] || [ "${!i}" = "--arch" ]; then
    USE_BUILDX="--buildx"
    j=$((i+1))
    BUILD_ARCH=${!j}
    if [ "$BUILD_ARCH" != "$ARCH_AARCH64" ] && [ "$BUILD_ARCH" != "$ARCH_X86" ]; then
      echo "Wrong arch name: ${BUILD_ARCH}. Please input aarch64 or x86."
      exit 1
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

if [ "$NEED_BUILD" = true ]; then
  buildImage
fi

if [ "$NEED_TAG" = true ]; then
  tagImage
fi

if [ "$NEED_PUBLISH" = true ]; then
  publishImages
fi

if [ "$NEED_MANIFEST" = true ]; then
  pushManifest
fi
