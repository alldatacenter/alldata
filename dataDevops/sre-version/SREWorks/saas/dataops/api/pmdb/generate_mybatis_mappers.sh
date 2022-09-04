#!/usr/bin/env bash

set -x

worker_path=$(cd "$(dirname "$0")"; pwd)

rm -rf ${worker_path}/pmdb-service/src/main/resources/mybatis/mapper/tddl/*.xml
rm -rf ${worker_path}/pmdb-service/src/main/java/com/alibaba/sreworks/pmdb/domain/*

cd ${worker_path}/pmdb-service
mvn mybatis-generator:generate
