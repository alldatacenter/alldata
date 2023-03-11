#!/bin/sh

APP_NAME=lineage-server

cd $(dirname "$0") || exit
cd .. || exit
DEPLOY_DIR=$(pwd)

PIDS=$(ps ax |grep -v tail|grep -v grep|grep "$DEPLOY_DIR"|awk '{print $1}')
if [ -z "$PIDS" ]; then
    echo "WARN: The $APP_NAME does not started!"
    exit 1
fi

echo -e "Stopping the $APP_NAME ...\c"
for PID in ${PIDS} ; do
    kill "${PID}" > /dev/null 2>&1
done

COUNT=0
while [ ${COUNT} -lt 1 ]; do
    echo -e ".\c"
    sleep 1
    COUNT=1
    for PID in ${PIDS} ; do
        PID_EXIST=$(ps -f -p "${PID}" | grep java)
        if [ -n "$PID_EXIST" ]; then
            COUNT=0
            break
        fi
    done
done

printf "\n"
echo "$APP_NAME service stopped successfully."
printf "\n"

