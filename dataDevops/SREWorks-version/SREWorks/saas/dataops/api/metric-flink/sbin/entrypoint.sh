#!/bin/sh

set -x
set -e

export DATA_DB_HOST=${DATA_DB_HOST}
export DATA_DB_PORT=${DATA_DB_PORT}
export DATA_DB_USER=${DATA_DB_USER}
export DATA_DB_PASSWORD=${DATA_DB_PASSWORD}
export DATA_DB_HEALTH_NAME=${DATA_DB_HEALTH_NAME}
#export DATA_DB_PMDB_NAME=${DATA_DB_PMDB_NAME}

export HEALTH_ENDPOINT=${HEALTH_ENDPOINT}

export MINIO_ENDPOINT=${MINIO_ENDPOINT}
export MINIO_ACCESS_KEY=${MINIO_ACCESS_KEY}
export MINIO_SECRET_KEY=${MINIO_SECRET_KEY}

export UDF_ARTIFACT_NAME="metric-flink-12"
export UDF_ARTIFACT_JAR="metric-flink-1.2.jar"

export VVP_ENDPOINT=${VVP_ENDPOINT}
export KAFKA_URL=${KAFKA_URL}
#export ES_URL=${ES_URL}
export DATA_ES_HOST=${DATA_ES_HOST}
export DATA_ES_PORT=${DATA_ES_PORT}
export DATA_ES_USER=${DATA_ES_USER}
export DATA_ES_PASSWORD=${DATA_ES_PASSWORD}

export VVP_WORK_NS="default"

/bin/sh /app/sbin/build.sh

#/bin/sh /app/sbin/upload.sh

/bin/sh /app/sbin/flink_job_init.sh

python3 /app/sbin/init-kafka.py
