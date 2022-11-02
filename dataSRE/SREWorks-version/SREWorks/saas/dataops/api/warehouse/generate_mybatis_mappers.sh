#!/usr/bin/env bash

set -x

worker_path=$(cd "$(dirname "$0")"; pwd)

rm -rf ${worker_path}/warehouse-service/src/main/resources/mybatis/mapper/tddl/*.xml

cd ${worker_path}/warehouse-service
mvn mybatis-generator:generate
