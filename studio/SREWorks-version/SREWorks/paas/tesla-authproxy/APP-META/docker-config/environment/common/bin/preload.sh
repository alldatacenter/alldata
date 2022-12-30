#!/bin/bash

APP_HOME=$(cd $(dirname ${BASH_SOURCE[0]})/..; pwd)
source "${APP_HOME}/bin/setenv.sh"

CHECK_PORT=$STATUS_PORT
if [ $1 -a "$1" -gt 0 ] 2>/dev/null; then
    CHECK_PORT=$1
fi

CURL_BIN=/usr/bin/curl
SPACE_STR="..................................................................................................."

OUTIF=`/sbin/route -n | tail -1  | sed -e 's/.* \([^ ]*$\)/\1/'`
HTTP_IP="http://`/sbin/ifconfig | grep -A1 ${OUTIF} | grep inet | awk '{print $2}' | sed 's/addr://g'`:${CHECK_PORT}"

#####################################
checkpage() {
    if [[ -f ${APP_HOME}/target/${APP_NAME}/bin/appctl.sh ]]; then
        echo ${SPACE_STR}
        sh ${APP_HOME}/target/${APP_NAME}/bin/appctl.sh "status"
        if [ "$?" != "0" ]; then
            echo "ERROR: ${APP_NAME} check status [FAILED]"
            status=0
            error=1
        else
            echo "INFO: ${APP_NAME} check status [  OK  ]"
            status=1
            error=0
        fi
        echo ${SPACE_STR}
        return $error
    else
        if [ ! -z "$SERVICE_PID" ]; then
            if [ -f "$SERVICE_PID" ]; then
                if [ -s "$SERVICE_PID" ]; then
                    if [ -r "$SERVICE_PID" ]; then
                        PID=`cat "$SERVICE_PID"`
                        ps -p $PID >/dev/null 2>&1
                        if [ $? -eq 0 ] ; then
                            ret=`$JAVA_HOME/bin/java -classpath "${APP_HOME}/target/${APP_NAME}:${JAVA_HOME}/lib/tools.jar" \
                                    -Dproject.name="${APP_NAME}" -Dpid="$PID" \
                                    com.taobao.pandora.boot.loader.jmx.Executor preload 2>&1 | \
                                    tee -a ${APP_HOME}/logs/${APP_NAME}_deploy.log | \
                                    fgrep "check preload for app ${APP_NAME} success"`
                            preload_htm "/checkpreload.htm" "${APP_NAME}" "success"
                            if [ $? -eq 0 -a ! -z "$ret" ]; then
                                echo ${SPACE_STR}
                                echo "INFO: ${APP_NAME} check status [  OK  ]"
                                echo ${SPACE_STR}
                                status=1
                                return 0
                            fi
                        fi
                    fi
                fi
            fi
        fi
        echo ${SPACE_STR}
        echo "INFO: ${APP_NAME} check status [FAILED]"
        echo ${SPACE_STR}
        return 1
    fi
}

preload_htm() {
    # check preload.htm
    portret=`(/usr/sbin/ss -ln4 sport = :${TOMCAT_PORT}; /usr/sbin/ss -ln6 sport = :${TOMCAT_PORT}) | grep -c ":${TOMCAT_PORT}"`
    if [ $portret -ne 0 -a "${NGINX_SKIP}" -ne "1" ]; then
        echo "INFO: tomcat is running at port: ${TOMCAT_PORT}"
        if [ "$CHECK_PORT" == 80 ]; then
          echo "INFO: try to chek checkpreload.htm through nginx port: 80"
        else
          echo "INFO: try to check checkpreload.htm through tomcat port: ${CHECK_PORT}"
        fi

        URL=$1
        TITLE=$2
        CHECK_TXT=$3
        echo "$CURL_BIN" "${HTTP_IP}${URL}"
        if [ "$TITLE" == "" ]; then
            TITLE=$URL
        fi
        len=`echo $TITLE | wc -c`
        len=`expr 60 - $len`
        echo -n -e "$TITLE ...${SPACE_STR:1:$len}"
        TMP_FILE=`$CURL_BIN --silent -m 150 "${HTTP_IP}${URL}" 2>&1`
        echo ""
        echo "$CURL_BIN" "${HTTP_IP}${URL}" " return: "
        echo "$TMP_FILE"
        if [ "$CHECK_TXT" != "" ]; then
            checkret=`echo "$TMP_FILE" | fgrep "$CHECK_TXT"`
            if [ "$checkret" == "" ]; then
                echo "ERROR: Please make sure checkpreload.htm return: success"
                error=1
            else
                error=0
            fi
        fi
        return $error
    fi

    return 0
}
#####################################
checkpage
