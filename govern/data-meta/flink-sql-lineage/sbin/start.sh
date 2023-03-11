#!/bin/sh

APP_NAME=lineage-server

cd $(dirname "$0") || exit
cd .. || exit
DEPLOY_DIR=$(pwd)

LOGS_DIR=${DEPLOY_DIR}/logs
if [ ! -d "${LOGS_DIR}" ]; then
    mkdir "${LOGS_DIR}"
fi
LOG_FILE=${LOGS_DIR}/lineage-server.log

JAVA_OUT=${DEPLOY_DIR}/${APP_NAME}_std_out.log
PID_FILE=${DEPLOY_DIR}/${APP_NAME}.pid

RUN_JAR=$(ls -t "${DEPLOY_DIR}"/lineage-server/*.jar |head -n 1)
echo "Starting the $APP_NAME ..."

nohup java -Xmx2g -Xms2g -jar "$RUN_JAR" &> "$JAVA_OUT" & echo $! > "$PID_FILE"

sleep 1

printf "\n\n"
echo "Please check the std_out files: $JAVA_OUT"
echo "Please check the log files: $LOG_FILE"
printf "\n"