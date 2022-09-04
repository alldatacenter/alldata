#!/usr/bin/env bash

set -x

worker_path=$(cd "$(dirname "$0")"; pwd)

#rm -rf ${worker_path}/health-service/src/main/resources/mybatis/mapper/tddl/*.xml
#rm -rf ${worker_path}/health-service/src/main/java/com/alibaba/sreworks/health/domain/*

cd ${worker_path}/health-service
mvn mybatis-generator:generate
