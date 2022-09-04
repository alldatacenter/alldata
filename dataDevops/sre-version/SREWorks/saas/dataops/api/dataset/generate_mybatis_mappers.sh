#!/usr/bin/env bash

set -x

worker_path=$(cd "$(dirname "$0")"; pwd)

rm -rf ${worker_path}/dataset-service/src/main/resources/mybatis/mapper/primary/*.xml
rm -rf ${worker_path}/dataset-service/src/main/java/com/alibaba/sreworks/dataset/domain/primary/*

cd ${worker_path}/dataset-service
mvn mybatis-generator:generate

rm -rf ${worker_path}/dataset-service/src/main/resources/tmp
