#!/bin/sh

SW_ROOT=$(cd `dirname $0`; pwd)

$SW_ROOT/mc alias set sw http://${MINIO_ENDPOINT} ${MINIO_ACCESS_KEY} ${MINIO_SECRET_KEY}

###### UPLOAD BUILD-IN RULES CONF
$SW_ROOT/mc mb -p sw/metric-rules
$SW_ROOT/mc cp /app/sbin/vvp-resources/rules.json sw/metric-rules/sreworks/metric/

###### UPLOAD MINIO UDF ARTIFACT JAR
$SW_ROOT/mc mb -p sw/vvp
$SW_ROOT/mc cp /app/sbin/${UDF_ARTIFACT_JAR} sw/vvp/artifacts/namespaces/default/udfs/

