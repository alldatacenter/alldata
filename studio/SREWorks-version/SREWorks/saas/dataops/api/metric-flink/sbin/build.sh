#!/bin/sh

SW_ROOT=$(cd `dirname $0`; pwd)

envsubst < /app/sbin/common.properties.tpl > /app/sbin/common.properties

cd /app/sbin && jar uvf ${UDF_ARTIFACT_JAR} common.properties
