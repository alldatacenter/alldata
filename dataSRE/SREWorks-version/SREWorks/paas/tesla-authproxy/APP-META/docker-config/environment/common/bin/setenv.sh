#!/bin/bash

# SETENV_SETTED promise run this only once.
if [ -z $SETENV_SETTED ]; then
    SETENV_SETTED="true"

    # app
    # set ${APP_NAME}, if empty $(basename "${APP_HOME}") will be used.
    APP_HOME=$(cd $(dirname ${BASH_SOURCE[0]})/..; pwd)
    if [[ "${APP_NAME}" = "" ]]; then
        APP_NAME=$(basename "${APP_HOME}")
    fi

    NGINX_HOME=/home/admin/cai

    export JAVA_HOME=/opt/taobao/java
    export PATH=${PATH}:${JAVA_HOME}/bin
    ulimit -c unlimited

    echo "INFO: OS max open files: "`ulimit -n`

    # when stop pandora boot process, will try to stop old tomcat process
    export CATALINA_HOME=/opt/taobao/tomcat
    export CATALINA_BASE=$APP_HOME/.default
    export CATALINA_PID=$CATALINA_BASE/catalina.pid
    # time to wait tomcat to stop before killing it
    TOMCAT_STOP_WAIT_TIME=5
    TOMCAT_PORT=7001

    if [[ ! -f ${APP_HOME}/target/${APP_NAME}/bin/appctl.sh ]]; then
        # env for service(pandora boot)
        export LANG=zh_CN.UTF-8
        export JAVA_FILE_ENCODING=UTF-8
        export NLS_LANG=AMERICAN_AMERICA.ZHS16GBK
        export LD_LIBRARY_PATH=/opt/taobao/oracle/lib:/opt/taobao/lib:$LD_LIBRARY_PATH
        export CPU_COUNT="$(grep -c 'cpu[0-9][0-9]*' /proc/stat)"

        mkdir -p "$APP_HOME"/.default
        export SERVICE_PID=$APP_HOME/.default/${APP_NAME}.pid
        export SERVICE_OUT=$APP_HOME/logs/service_stdout.log
        export MIDDLEWARE_LOGS="${HOME}/logs"
        export MIDDLEWARE_SNAPSHOTS="${HOME}/snapshots"

        if [ -z "$SERVICE_TMPDIR" ] ; then
            # Define the java.io.tmpdir to use for Service(pandora boot)
            SERVICE_TMPDIR="${APP_HOME}"/.default/temp
        fi

        SERVICE_OPTS="${SERVICE_OPTS} -server"

        let memTotal=`cat /proc/meminfo | grep MemTotal | awk '{printf "%d", $2/1024 }'`
        echo "INFO: OS total memory: "$memTotal"M"
        # if os memory <= 2G
        if [ $memTotal -le 2048 ]; then
          SERVICE_OPTS="${SERVICE_OPTS} -Xms1536m -Xmx1536m"
          SERVICE_OPTS="${SERVICE_OPTS} -Xmn768m"
        else
          SERVICE_OPTS="${SERVICE_OPTS} -Xms4g -Xmx4g"
          SERVICE_OPTS="${SERVICE_OPTS} -Xmn2g"
        fi

        SERVICE_OPTS="${SERVICE_OPTS} -XX:PermSize=256m -XX:MaxPermSize=512m"
        SERVICE_OPTS="${SERVICE_OPTS} -XX:MaxDirectMemorySize=1g"
        SERVICE_OPTS="${SERVICE_OPTS} -XX:SurvivorRatio=10"
        SERVICE_OPTS="${SERVICE_OPTS} -XX:+UseConcMarkSweepGC -XX:+UseCMSCompactAtFullCollection -XX:CMSMaxAbortablePrecleanTime=5000"
        SERVICE_OPTS="${SERVICE_OPTS} -XX:+CMSClassUnloadingEnabled -XX:CMSInitiatingOccupancyFraction=80 -XX:+UseCMSInitiatingOccupancyOnly"
        SERVICE_OPTS="${SERVICE_OPTS} -XX:+ExplicitGCInvokesConcurrent -Dsun.rmi.dgc.server.gcInterval=2592000000 -Dsun.rmi.dgc.client.gcInterval=2592000000"
        SERVICE_OPTS="${SERVICE_OPTS} -XX:ParallelGCThreads=4"
        SERVICE_OPTS="${SERVICE_OPTS} -Xloggc:${MIDDLEWARE_LOGS}/gc.log -XX:+PrintGCDetails -XX:+PrintGCDateStamps"
        SERVICE_OPTS="${SERVICE_OPTS} -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=${MIDDLEWARE_LOGS}/java.hprof"
        SERVICE_OPTS="${SERVICE_OPTS} -Djava.awt.headless=true"
        SERVICE_OPTS="${SERVICE_OPTS} -Dsun.net.client.defaultConnectTimeout=10000"
        SERVICE_OPTS="${SERVICE_OPTS} -Dsun.net.client.defaultReadTimeout=30000"
        SERVICE_OPTS="${SERVICE_OPTS} -DJM.LOG.PATH=${MIDDLEWARE_LOGS}"
        SERVICE_OPTS="${SERVICE_OPTS} -DJM.SNAPSHOT.PATH=${MIDDLEWARE_SNAPSHOTS}"
        SERVICE_OPTS="${SERVICE_OPTS} -Dfile.encoding=${JAVA_FILE_ENCODING}"
        SERVICE_OPTS="${SERVICE_OPTS} -Dhsf.publish.delayed=true"
        SERVICE_OPTS="${SERVICE_OPTS} -Dproject.name=${APP_NAME}"
        SERVICE_OPTS="${SERVICE_OPTS} -Dpandora.boot.wait=true -Dlog4j.defaultInitOverride=true"
        SERVICE_OPTS="${SERVICE_OPTS} -Dserver.port=${TOMCAT_PORT} -Dmanagement.port=7002 -Dmanagement.server.port=7002"

        # debug opts

        # jpda options
        test -z "$JPDA_ENABLE" && JPDA_ENABLE=0
        test -z "$JPDA_ADDRESS" && export JPDA_ADDRESS=8000
        test -z "$JPDA_SUSPEND" && export JPDA_SUSPEND=n

        if [ "$JPDA_ENABLE" -eq 1 ]; then
            if [ -z "$JPDA_TRANSPORT" ]; then
                JPDA_TRANSPORT="dt_socket"
            fi
            if [ -z "$JPDA_ADDRESS" ]; then
                JPDA_ADDRESS="8000"
            fi
            if [ -z "$JPDA_SUSPEND" ]; then
                JPDA_SUSPEND="n"
            fi
            if [ -z "$JPDA_OPTS" ]; then
                JPDA_OPTS="-agentlib:jdwp=transport=$JPDA_TRANSPORT,address=$JPDA_ADDRESS,server=y,suspend=$JPDA_SUSPEND"
            fi
            SERVICE_OPTS="$SERVICE_OPTS $JPDA_OPTS"
        fi

        export SERVICE_OPTS

        if [ -z "$NGINX_HOME" ]; then
            NGINX_HOME=/home/admin/cai
        fi

        # if set to "1", skip start nginx.
        test -z "$NGINX_SKIP" && NGINX_SKIP=0
        # set port for checking status.taobao file. Comment it if no need.
        STATUS_PORT=80
        # time to wait for /status.taobao is ready
        STATUS_TAOBAO_WAIT_TIME=3
        STATUSROOT_HOME="${APP_HOME}/target/${APP_NAME}/META-INF/resources"
        # make sure the directory exist, before tomcat start
        mkdir -p $STATUSROOT_HOME
        NGINXCTL=$NGINX_HOME/bin/nginxctl

        # search pandora by "${PANDORA_NAME_LIST[@]}" order
        PANDORA_NAME_LIST=(pandora taobao-hsf)

        # set hsf online/offline time out (in second)
        HSF_ONLINE_TIMEOUT=120

        # if update pandora
        UPDATE_PANDORA=true
    else
        # compatible with the existing jar application
        export LANG=zh_CN.UTF-8
    fi

    if [ -f $APP_HOME/bin/update_setenv.sh ]; then
        echo "source ""$APP_HOME/bin/update_setenv.sh"
        source "$APP_HOME/bin/update_setenv.sh"
    fi
fi
