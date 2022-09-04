#!/bin/bash

set -x
set -e

if [[ -z "$1" || -z "$2" ]]; then
    echo "Invalid parameter, exit 1"
    exit 1
fi

TAG=$1
if [[ "${TAG}" =~ ^CICD.* ]]; then
    TAG="live"
fi

if [[ "$2" == "arm64" ]]; then
    PULL_IMAGE_PREFIX="reg.docker.alibaba-inc.com/abm-arm64v8/paas-appmanager-operator"
else
    PULL_IMAGE_PREFIX="reg.docker.alibaba-inc.com/sw/paas-appmanager-operator"
fi

sudo docker pull ${PULL_IMAGE_PREFIX}:${TAG}

sudo docker tag ${PULL_IMAGE_PREFIX}:${TAG} reg.docker.alibaba-inc.com/abm-aone/abm-appmanager-operator:$1_$2
