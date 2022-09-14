#!/bin/bash

cd `dirname $0`
cd ..
HOME=`pwd`
export STREAMIS_HOME=$HOME

export STREAMIS_PID=$HOME/bin/linkis.pid

if [[ -f "${STREAMIS_PID}" ]]; then
    pid=$(cat ${STREAMIS_PID})
    if kill -0 ${pid} >/dev/null 2>&1; then
      echo "Streamis Server is already running."
      return 0;
    fi
fi

export STREAMIS_LOG_PATH=$HOME/logs
export STREAMIS_HEAP_SIZE="1G"
export STREAMIS_JAVA_OPTS="-Xms$STREAMIS_HEAP_SIZE -Xmx$STREAMIS_HEAP_SIZE -XX:+UseG1GC -XX:MaxPermSize=500m -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=11729"

nohup java $STREAMIS_JAVA_OPTS -cp $HOME/conf:$HOME/lib/* org.apache.linkis.DataWorkCloudApplication 2>&1 > $STREAMIS_LOG_PATH/streamis.out &
pid=$!
if [[ -z "${pid}" ]]; then
    echo "Streamis Server start failed!"
    sleep 1
    exit 1
else
    echo "Streamis Server start succeeded!"
    echo $pid > $STREAMIS_PID
    sleep 1
fi
exit 1
