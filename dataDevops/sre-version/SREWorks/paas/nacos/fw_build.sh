#!/bin/bash

set -x
set -e

if [[ -z "$1" || -z "$2" || -z "$3" ]]; then
    echo "Invalid parameter, exit 1"
    exit 1
fi

IMAGE_PREFIX="reg.docker.alibaba-inc.com/abm-aone/$1"
TAG=$2
if [[ "${TAG}" =~ ^CICD.* ]]; then
    TAG="live"
fi

if [[ "$3" == "aarch64" ]]; then
    PULL_IMAGE_PREFIX="reg.docker.alibaba-inc.com/abm-arm64v8/$1"
else
    PULL_IMAGE_PREFIX="reg.docker.alibaba-inc.com/abm-aone/$1"
fi

sudo docker pull ${PULL_IMAGE_PREFIX}:${TAG}
sudo docker pull ${PULL_IMAGE_PREFIX}-db-migration:${TAG}
sudo docker pull ${PULL_IMAGE_PREFIX}-config-init:${TAG}

sudo docker tag ${PULL_IMAGE_PREFIX}:${TAG} ${IMAGE_PREFIX}:$2_$3
sudo docker tag ${PULL_IMAGE_PREFIX}-db-migration:${TAG} ${IMAGE_PREFIX}-db-migration:$2_$3
sudo docker tag ${PULL_IMAGE_PREFIX}-config-init:${TAG} ${IMAGE_PREFIX}-config-init:$2_$3


