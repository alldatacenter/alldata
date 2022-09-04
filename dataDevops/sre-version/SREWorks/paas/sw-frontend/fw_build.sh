#!/bin/bash

set -x
set -e

if [[ -z "$1" || -z "$2" || -z "$3" ]]; then
    echo "Invalid parameter, exit 1"
    exit 1
fi

IMAGE_PREFIX=""
TAG=$2
if [[ "${TAG}" =~ ^CICD.* ]]; then
    TAG="live"
fi

if [[ "$3" == "aarch64" ]]; then
    PULL_IMAGE_PREFIX=""
else
    PULL_IMAGE_PREFIX=""
fi

sudo docker pull ${PULL_IMAGE_PREFIX}:${TAG}

sudo docker tag ${PULL_IMAGE_PREFIX}:${TAG} ${IMAGE_PREFIX}:$2_$3
