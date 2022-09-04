#!/bin/bash

#######  error code specification  #########
# Please update this documentation if new error code is added.
# 1   => reserved for script error
# 2   => bad usage
# 3   => bad user
# 4   => service start failed
# 5   => preload.sh check failed
# 6   => hsf online failed
# 7   => nginx start failed
# 8   => status.taobao check failed
# 9   => hsf offline failed
# 128 => exit with error message


PROG_NAME=$0
ACTION=$1

usage() {
    echo "Usage: $PROG_NAME {start|stop|online|offline|pubstart|restart|deploy}"
    exit 2 # bad usage
}

if [ "$UID" -eq 0 ]; then
    echo "ERROR: can't run as root, please use: sudo -u admin $0 $@"
    exit 3 # bad user
fi

if [ $# -lt 1 ]; then
    usage
    exit 2 # bad usage
fi

APP_HOME=$(cd $(dirname ${BASH_SOURCE[0]})/..; pwd)
source "${APP_HOME}/bin/hook.sh"
source "${APP_HOME}/bin/setenv.sh"

APP_TGZ_FILE=${APP_NAME}.tgz
if [ ! -f ${APP_HOME}/target/${APP_TGZ_FILE} ]; then
    # support b2b app, which tgz name like appName____hz.tgz
    echo "INFO: can not find ${APP_HOME}/target/${APP_TGZ_FILE}, try to find ${APP_HOME}/target/${APP_NAME}____*.tgz"
    APP_TGZ_FILE=$(basename $(ls ${APP_HOME}/target/${APP_NAME}____*.tgz))
    test -f "${APP_HOME}/target/${APP_TGZ_FILE}" || die "app tgz file ${APP_TGZ_FILE} not exist"
fi

# check previous pipe command exit code. if no zero, whill exit with previous pipe status.
check_first_pipe_exit_code() {
  local first_pipe_exit_code=${PIPESTATUS[0]};
  if test $first_pipe_exit_code -ne 0; then
    exit $first_pipe_exit_code;
  fi
}

die() {
    if [[ "$#" -gt 0 ]]; then
        echo "ERROR: " "$@"
    fi
    exit 128
}

printLogPathInfo() {
  echo "Please check deploy log: ${APP_HOME}/logs/${APP_NAME}_deploy.log"
  echo "Please check application stdout: ${SERVICE_OUT}"
}

exit1() {
  echo "exit code 1"
  printLogPathInfo
  exit 1
}

exit4() {
  echo "exit code 4"
  printLogPathInfo
  exit 4
}

exit5() {
  echo "exit code 5"
  printLogPathInfo
  exit 5
}


extract_tgz() {
    local tgz_path="$1"
    local dir_path="$2"

    echo "extract ${tgz_path}"
    cd "${APP_HOME}/target" || exit1
    rm -rf "${dir_path}" || exit1
    tar xzf "${tgz_path}" || exit1
    # in order to support fat.jar, unzip it.
    test -f "${dir_path}.jar" && unzip -q "${dir_path}.jar" -d "${dir_path}"
    test -d "${dir_path}" || die "no directory: ${dir_path}"
    touch --reference "${tgz_path}" "${tgz_path}.timestamp" || exit1
}

update_target() {
    local tgz_name="$1"
    local dir_name="$2"

    local tgz_path="${APP_HOME}/target/${tgz_name}"
    local dir_path="${APP_HOME}/target/${dir_name}"

    local error=0
    # dir exists
    if [ -d "${dir_path}" ]; then
        # tgz exists
        if [ -f "${tgz_path}" ]; then
            local need_tar=0
            if [ ! -e "${tgz_path}.timestamp" ]; then
                need_tar=1
            else
                local tgz_time=$(stat -L -c "%Y" "${tgz_path}")
                local last_time=$(stat -L -c "%Y" "${tgz_path}.timestamp")
                if [ $tgz_time -gt $last_time ]; then
                    need_tar=1
                fi
            fi
            # tgz is new - extract_tgz
            if [ "${need_tar}" -eq 1 ]; then
                extract_tgz "${tgz_path}" "${dir_path}"
            fi
            # tgz is not new - return SUCCESS
        fi
        # tgz not exists - return SUCCESS
    # dir not exists
    else
        # tgz exists - extract_tgz
        if [ -f "${tgz_path}" ]; then
            extract_tgz "${tgz_path}" "${dir_path}"
        # tgz not exists - return FAIL
        else
            echo "ERROR: ${tgz_path} NOT EXISTS"
            error=1
        fi
    fi

    return $error
}

update_pandora() {
    local pandora_ok=""
    local last_log error_log
    for PANDORA_NAME in "${PANDORA_NAME_LIST[@]}"; do
        if [ -z "$pandora_ok" ]; then
            # capture log while search pandora
            last_log=$(update_target "${PANDORA_NAME}.tgz" "${PANDORA_NAME}.sar")
            if [ "$?" -eq 0 ]; then
                # discard previous error log
                error_log="${last_log}"
                test -n "${error_log}" && echo "${error_log}" && error_log=""
                pandora_ok="${APP_HOME}/target/${PANDORA_NAME}.sar"
                echo "USING PANDORA: ${pandora_ok}"
                SERVICE_OPTS="${SERVICE_OPTS} -Dpandora.location=${pandora_ok}"
                export SERVICE_OPTS
            else
                # concat previous error log
                test -n "${error_log}" && error_log="${error_log}"$'\n'
                error_log="${error_log}${last_log}"
            fi
        else
            # rename old pandora tgz to "*.bak"
            rm -rf "${APP_HOME}/target/${PANDORA_NAME}."{sar,tgz.timestamp}
            tgz_path="${APP_HOME}/target/${PANDORA_NAME}.tgz"
            if [ -e "${tgz_path}" ]; then
                mv -T "${tgz_path}" "${tgz_path}.bak"
            fi
            # symbolic old pandora dir to ${PANDORA_NAME}.sar
            ln -sf "$(basename ${pandora_ok})" "${APP_HOME}/target/${PANDORA_NAME}.sar"
        fi
    done
    # echo error_log at end
    test -n "${error_log}" && echo "${error_log}" && error_log=""
    test -n "${pandora_ok}" && test -d "${pandora_ok}"
}

call_tappctl(){
    local action=$1
    if [[ -f ${APP_HOME}/target/${APP_NAME}/bin/appctl.sh ]]; then
        echo "INFO: do ${action} app..."
        echo "INFO: call ${APP_HOME}/target/${APP_NAME}/bin/appctl.sh ${action}"
        sh ${APP_HOME}/target/${APP_NAME}/bin/appctl.sh ${action}
        if [[ "$?" = "0" ]]; then
            echo "INFO: call ${APP_NAME} ${action} success"
        else
            echo "ERROR: call ${APP_NAME} ${action} failed"
            exit1
        fi
    else
        echo "INFO: ${APP_HOME}/target/${APP_NAME}/bin/appctl.sh"
        echo "INFO: please follow this link"
        echo "INFO: http://docs.alibaba-inc.com:8090/pages/viewpage.action?pageId=259635541"
        exit1
    fi
}

do_start() {
    call_tappctl "start"
    call_tappctl "status"
    echo "INFO: ${APP_NAME} start success and service start up"
}

start() {
    echo "INFO: ${APP_NAME} try to start..."
    mkdir -p "${APP_HOME}/target" || exit1
    mkdir -p "${APP_HOME}/logs" || exit1
    HOME="$(getent passwd "$UID" | awk -F":" '{print $6}')" # fix "$HOME" by "$UID"
    # create symlink for middleware logs.
    ln -sfT "${MIDDLEWARE_LOGS}" "${APP_HOME}/logs/middleware"
    # update app
    echo "[start 1] start to unzip app tgz file..."
    update_target "${APP_TGZ_FILE}" "${APP_NAME}" || exit1

    echo "[start 2] try to unzip taobao-hsf.tgz file..."
    if [ "$UPDATE_PANDORA" == "true" ]; then
        update_pandora || exit1
    else
        echo "UPDATE_PANDORA is false, ignore this"
    fi

    beforeStartApp
    if [[ -f ${APP_HOME}/target/${APP_NAME}/bin/appctl.sh ]]; then
        do_start
    else
        echo "[start 3] try to start pandora boot..."
        start_pandora_boot
    fi
    echo "INFO: ${APP_NAME} start success"
    afterStartApp
}

start_pandora_boot() {
    # prepare_service_out
    # delete old SERVICE_OUT, keep last 20 logs
    ls -t "$SERVICE_OUT".* 2>/dev/null | tail -n +$((20 + 1)) | xargs --no-run-if-empty rm -f
    if [ -e "$SERVICE_OUT" ]; then
        mv "$SERVICE_OUT" "$SERVICE_OUT.$(date '+%Y%m%d%H%M%S')" || exit1
    fi
    mkdir -p "$(dirname "${SERVICE_OUT}")" || exit1
    touch "$SERVICE_OUT" || exit1

    echo "INFO: Pandora boot service log: $SERVICE_OUT"

    if [ ! -z "$SERVICE_PID" ]; then
        if [ -f "$SERVICE_PID" ]; then
            if [ -s "$SERVICE_PID" ]; then
                echo "Existing PID file found during start."
                if [ -r "$SERVICE_PID" ]; then
                    PID=`cat "$SERVICE_PID"`
                    ps -p $PID >/dev/null 2>&1
                    if [ $? -eq 0 ] ; then
                        echo "Service(pandora boot) appears to still be running with PID $PID. Start aborted."
                        exit1
                    else
                        echo "Removing/clearing stale PID file."
                        rm -f "$SERVICE_PID" >/dev/null 2>&1
                        if [ $? != 0 ]; then
                            if [ -w "$SERVICE_PID" ]; then
                                cat /dev/null > "$SERVICE_PID"
                            else
                                echo "Unable to remove or clear stale PID file. Start aborted."
                                exit1
                            fi
                        fi
                    fi
                else
                    echo "Unable to read PID file. Start aborted."
                    exit1
                fi
            else
                rm -f "$SERVICE_PID" >/dev/null 2>&1
                if [ $? != 0 ]; then
                    if [ ! -w "$SERVICE_PID" ]; then
                        echo "Unable to remove or write to empty PID file. Start aborted."
                        exit1
                    fi
                fi
            fi
        fi
    fi

    ## adjust vm args for jdk8
    JAVA_VERSION=`$JAVA_HOME"/bin/java" -version 2>&1` || { echo $JAVA_HOME"/bin/java -version failed"; exit 1; }
    JAVA_VERSION_MAJOR=${JAVA_VERSION:16:1}
    if [ "$JAVA_VERSION_MAJOR" -ge 8 ]; then
        SERVICE_OPTS="${SERVICE_OPTS//PermSize/MetaspaceSize}"
        SERVICE_OPTS="${SERVICE_OPTS//MaxPermSize/MaxMetaspaceSize}"
        SERVICE_OPTS="${SERVICE_OPTS//-XX:+UseCMSCompactAtFullCollection/}"
        export SERVICE_OPTS
    fi

    eval exec "\"$JAVA_HOME/bin/java\"" $SERVICE_OPTS \
            -classpath "\"${APP_HOME}/target/${APP_NAME}\"" \
            -Dapp.location="\"${APP_HOME}/target/${APP_NAME}\"" \
            -Djava.endorsed.dirs="\"$JAVA_ENDORSED_DIRS\""  \
            -Djava.io.tmpdir="\"$SERVICE_TMPDIR\"" \
            com.taobao.pandora.boot.loader.SarLauncher "$@" \
            >> "$SERVICE_OUT" 2>&1 "&"

    if [ ! -z "$SERVICE_PID" ]; then
        echo $! > "$SERVICE_PID"
    fi

    local OUTIF=`/sbin/route -n | tail -1  | sed -e 's/.* \([^ ]*$\)/\1/'`
    local IP="`/sbin/ifconfig | grep -A1 ${OUTIF} | grep inet | awk '{print $2}' | sed 's/addr://g'`"
    local exptime=0
    local time=300
    local tomcat_web="http://start.alibaba-inc.com/node-log?device=${IP}"
    while true
    do
        ret=`fgrep "Service(pandora boot) startup in" $SERVICE_OUT`
        if [ -z "$ret" ]; then
            sleep 1
            ((exptime++))
            if [ ${exptime} -gt ${time} ]; then
                echo -n -e "\rWait Service(pandora boot) Start: $exptime..., you can visit $tomcat_web to check boot log"
            else
                echo -n -e "\rWait Service(pandora boot) Start: $exptime..."
            fi

            if [ `expr $exptime \% 10` -eq 0 ]; then
                ps -p `cat "$SERVICE_PID"` >/dev/null 2>&1
                if [ $? -gt 0 ] ; then
                    echo
                    echo "Service(pandora boot) appears exit, start failed."
                    exit4 # service start failed
                fi
            fi
        else
            echo
            ## ret=`fgrep "Tomcat started on port(s)" $SERVICE_OUT`
            ret=`(/usr/sbin/ss -ln4 sport = :7001; /usr/sbin/ss -ln6 sport = :7001) | grep -c ":7001"`
            if [ $ret -ne 0 ]; then
                echo "detected 7001, so start nginx later"
                mkdir -p $STATUSROOT_HOME
                touch -m $STATUSROOT_HOME/status.taobao || exit1
            else
                echo "WARN: not detected 7001, nginx will not start"
            fi

            . "$APP_HOME/bin/preload.sh" $TOMCAT_PORT
            if [ $? -gt 0 ]; then
                echo "preload.sh check failed."
                exit5 # preload.sh check failed
            else
                online
                echo "INFO: Service(pandora boot) ${APP_NAME} start success and service start up."
                break
            fi
        fi
    done
}

online() {
    if [[ -f ${APP_HOME}/target/${APP_NAME}/bin/appctl.sh ]]; then
        call_tappctl "status"
        echo "INFO: ${APP_NAME} online success"
    else
        echo "app auto online..."
        mkdir -p $STATUSROOT_HOME
        touch -m $STATUSROOT_HOME/status.taobao || exit1
        HSF_ONLINE=$(echo ${SERVICE_OPTS} | grep -o hsf.publish.delayed=\\w* | awk -F'=' '{print tolower($2);}')
        if [ "$HSF_ONLINE" == "true" ]; then
            echo "-Dhsf.publish.delayed=true is configured, calling online_hsf explicitly..."
            online_hsf
            echo "INFO: ${APP_NAME} online success."
        else
            echo "Could not found -Dhsf.publish.delayed or -Dhsf.publish.delayed=false, skip calling online_hsf."
        fi
    fi
}

online_hsf() {
    # using ss -ln4 instead of ss -tln.
    # ss -tln will cause core dump if redhat version <= 5.4
    # see http://gitlab.alibaba-inc.com/middleware/apps-deploy/issues/20
    # update 2015.2.13:
    # see http://gitlab.alibaba-inc.com/middleware/apps-deploy/issues/36
    check_hsf=`(/usr/sbin/ss -ln4 sport = :12200; /usr/sbin/ss -ln6 sport = :12200) | grep -c ":12200"`
    check_pandora=`(/usr/sbin/ss -ln4 sport = :12201; /usr/sbin/ss -ln6 sport = :12201) | grep -c ":12201"`
    if [ $check_hsf -ne 0 -a $check_pandora -ne 0 ]; then
        echo "Online hsf...."
        ret_str=`curl --max-time ${HSF_ONLINE_TIMEOUT} -s "http://localhost:12201/hsf/online?k=hsf" 2>&1`
        if echo "$ret_str" | grep "server is resumed to register on cs(dr)" &>/dev/null; then
            echo "hsf online success."
        else
            echo "hsf online failed in ${HSF_ONLINE_TIMEOUT} seconds."
            echo "You can adjust the value of HSF_ONLINE_TIMEOUT in setenv.sh"
            echo "Note that hsf version < 2.1.0.7 does not support this feature. Please upgrade to 2.1.0.7 and later. \
                  If you still want to use the lower version, please set -Dhsf.publish.delayed=false in setenv.sh"
            exit 6 # hsf online failed
        fi
    else
        if [ $check_pandora -eq 0 ]; then
            echo "WARN: port 12201 cannot be detected."
            echo "hsf online failed. HSF is NOT online!"
            exit 6 # hsf online failed
        fi
        if [ $check_hsf -eq 0 ]; then
            ret_str=`curl -s 127.0.0.1:12201/hsf/isProvider 2>&1 | grep -c "false"`
            if [ $ret_str -eq 1 ]; then
                echo "server is not hsf provider, ignore port 12200, go on..."
            else
                echo "WARN: port 12200 cannot be detected."
                echo "hsf online failed. HSF is NOT online!"
                exit 6 # hsf online failed
            fi
        fi
    fi
}

offline() {
    echo "INFO: ${APP_NAME} try to offline..."
    if [[ -f ${APP_HOME}/target/${APP_NAME}/bin/appctl.sh ]]; then
        call_tappctl "stop"
    else
        rm -f $STATUSROOT_HOME/status.taobao
        offline_hsf
        echo "INFO: ${APP_NAME} offline success"
        return $?
    fi
}

offline_hsf() {
    check_hsf=`(/usr/sbin/ss -ln4 sport = :12200; /usr/sbin/ss -ln6 sport = :12200) | grep -c ":12200"`
    check_pandora=`(/usr/sbin/ss -ln4 sport = :12201; /usr/sbin/ss -ln6 sport = :12201) | grep -c ":12201"`
    echo "try to offline hsf..."
    if [ $check_hsf -ne 0 -a $check_pandora -ne 0 ]; then
        echo "start to offline hsf...."
        ret_str=`curl --max-time ${HSF_ONLINE_TIMEOUT} -s "http://localhost:12201/hsf/offline?k=hsf" 2>&1`
        if echo "$ret_str" | grep "server is unregistered on cs(dr)" &>/dev/null; then
            echo "hsf offline success."
            return 0
        else
            echo "hsf offline failed."
            exit 9 # hsf offline failed
        fi
    else
        if [ $check_hsf -eq 0 ]; then
            echo "WARN: port 12200 cannot be detected."
        fi
        if [ $check_pandora -eq 0 ]; then
            echo "WARN: port 12201 cannot be detected."
        fi
        echo "WARN: hsf offline failed."
        # entity NOT exit here
    fi
}

stop() {
    beforeStopApp
    echo "INFO: ${APP_NAME} try to stop..."
    if [[ -f ${APP_HOME}/target/${APP_NAME}/bin/appctl.sh ]]; then
        call_tappctl "stop"
    else
        # 1. hsf offline
        echo "[stop 1] before call offline hsf..."
        offline
        echo "[stop 2] wait app offline..."
        for e in $(seq 15); do
            echo -n " $e"
            sleep 1
        done
        echo

        # 2. stop nginx
        echo "[stop 3] try to stop nginx..."
        echo "stop nginx..."
        "$NGINXCTL" stop

        # 3. try to stop old tomcat process
        echo "[stop 4] try stop old tomcat..."
        stop_tomcat

        # 4. stop pandora boot
        echo "[stop 5] try to stop pandora boot..."
        if [ -f "$SERVICE_PID" ]; then
            if [ -s "$SERVICE_PID" ]; then
                kill -0 `cat "$SERVICE_PID"` >/dev/null 2>&1
                if [ $? -gt 0 ]; then
                    echo "PID file found but no matching process was found. Stop aborted."
                else
                    stop_pandora_boot
                    if [ -f ${MIDDLEWARE_LOGS}/gc.log ]; then
                        mv ${MIDDLEWARE_LOGS}/gc.log ${MIDDLEWARE_LOGS}/gc.log.`date +%Y%m%d%H%M%S`
                    fi
                fi
            else
                echo "PID file is empty and has been ignored."
                rm -f "$SERVICE_PID" >/dev/null 2>&1
            fi
        else
            echo "\$SERVICE_PID was set but the specified file does not exist. Is Service running? Stop aborted."
        fi
    fi
    echo "INFO: ${APP_NAME} stop success"
    afterStopApp
}

stop_tomcat() {
    if [ -f "$CATALINA_PID" ]; then
        echo "stop old tomcat..."
        local PID=$(cat "$CATALINA_PID")
        if kill -0 "$PID" 2>/dev/null; then
            "$CATALINA_HOME"/bin/catalina.sh stop $TOMCAT_STOP_WAIT_TIME -force
            mv /home/admin/logs/gc.log /home/admin/logs/gc.log.`date +%Y%m%d%H%M%S`
            echo "delete old tomcat pid file"
            rm -f "$CATALINA_PID" >/dev/null 2>&1
        fi
    else
        echo "no old tomcat, ignore this"
    fi
}

stop_pandora_boot() {
    SLEEP=5
    FORCE=1

    # notify pandora boot to shutdown
    PID=`cat "$SERVICE_PID"`
    ret=`$JAVA_HOME/bin/java -classpath "${APP_HOME}/target/${APP_NAME}:${JAVA_HOME}/lib/tools.jar" \
            -Dproject.name="${APP_NAME}" -Dpid="$PID" \
            com.taobao.pandora.boot.loader.jmx.Executor shutdown 2>&1 | \
            fgrep "shutdown for app ${APP_NAME} success"`
    if [ ! -z "$ret" ]; then
        echo "Service(pandora boot) receive the shutdown."
        exptime=0
        while true
        do
            kill -0 "$PID" >/dev/null 2>&1
            if [ $? -gt 0 ]; then
                echo
                rm -f "$SERVICE_PID" >/dev/null 2>&1
                if [ $? != 0 ]; then
                    if [ -w "$SERVICE_PID" ]; then
                        cat /dev/null > "$SERVICE_PID"
                    else
                        echo "The PID file could not be removed."
                    fi
                fi
                echo "Service(pandora boot) shutdown completed."
                return 0
            else
                sleep 1
                ((exptime++))
                echo -n -e "\rWait Service(pandora boot) to shutdown: $exptime..."
                if [ $exptime -gt 15 ]; then
                    echo
                    break
                fi
            fi
        done
    fi

    # stop failed. Try a normal kill.
    echo "The stop command failed. Attempting to signal the process to stop through OS signal."
    kill -15 "$PID" >/dev/null 2>&1

    while [ $SLEEP -ge 0 ]; do
        kill -0 "$PID" >/dev/null 2>&1
        if [ $? -gt 0 ]; then
            rm -f "$SERVICE_PID" >/dev/null 2>&1
            if [ $? != 0 ]; then
                if [ -w "$SERVICE_PID" ]; then
                    cat /dev/null > "$SERVICE_PID"
                else
                    echo "The PID file could not be removed or cleared."
                fi
            fi
            echo "Service stopped."
            # If Service has stopped don't try and force a stop with an empty PID file
            FORCE=0
            break
        fi
        if [ $SLEEP -gt 0 ]; then
            sleep 1
        fi
        SLEEP=`expr $SLEEP - 1 `
    done

    KILL_SLEEP_INTERVAL=5
    if [ $FORCE -eq 1 ]; then
        if [ -f "$SERVICE_PID" ]; then
            PID=`cat "$SERVICE_PID"`
            echo "Killing Service with the PID: $PID"
            kill -9 $PID
            while [ $KILL_SLEEP_INTERVAL -ge 0 ]; do
                kill -0 `cat "$SERVICE_PID"` >/dev/null 2>&1
                if [ $? -gt 0 ]; then
                    rm -f "$SERVICE_PID" >/dev/null 2>&1
                    if [ $? != 0 ]; then
                        if [ -w "$SERVICE_PID" ]; then
                            cat /dev/null > "$SERVICE_PID"
                        else
                            echo "The PID file could not be removed."
                        fi
                    fi
                    # Set this to zero else a warning will be issued about the process still running
                    KILL_SLEEP_INTERVAL=0
                    echo "The Service process has been killed."
                    break
                fi
                if [ $KILL_SLEEP_INTERVAL -gt 0 ]; then
                    sleep 1
                fi
                KILL_SLEEP_INTERVAL=`expr $KILL_SLEEP_INTERVAL - 1 `
            done
            if [ $KILL_SLEEP_INTERVAL -gt 0 ]; then
                echo "Service has not been killed completely yet. The process might be waiting on some system call or might be UNINTERRUPTIBLE."
            fi
        fi
    fi
}

start_nginx() {
    echo "INFO: ${APP_NAME} try to start nginx..."
    ret=`(/usr/sbin/ss -ln4 sport = :7001; /usr/sbin/ss -ln6 sport = :7001) | grep -c ":7001"`
    if [ $ret -ne 0 -a "${NGINX_SKIP}" -ne "1" ]; then
        echo "now start nginx..."
        "$NGINXCTL" start
        if [ "$?" == "0" ]; then
            echo "Nginx Start SUCCESS."
        else
            echo "Nginx Start Failed."
            exit 7 # nginx start failed
        fi

        if test -n "${STATUS_PORT}"; then
            sleep ${STATUS_TAOBAO_WAIT_TIME}
            status_code=`/usr/bin/curl -L -o /dev/null --connect-timeout 5 -s -w %{http_code}  "http://127.0.0.1:${STATUS_PORT}/status.taobao"`
            if [ x$status_code != x200 ];then
                echo "check http://127.0.0.1:${STATUS_PORT}/status.taobao failed with status ${status_code} after wait ${STATUS_TAOBAO_WAIT_TIME} seconds."
                echo "You can adjust STATUS_TAOBAO_WAIT_TIME in setenv.sh"
                echo "See http://gitlab.alibaba-inc.com/middleware/apps-deploy/issues/31"
                exit 8 # status.taobao check failed
            fi
            echo "check http://127.0.0.1:${STATUS_PORT}/status.taobao success"
        fi
        echo "app online success"
    else
        echo "NGINX_SKIP=1 or no no tomcat start up on 7001, ignore start nginx"
    fi
}

backup() {
    if [ -f "${APP_HOME}/target/${APP_TGZ_FILE}" ]; then
        mkdir -p "${APP_HOME}/target/backup" || exit1
        tgz_time=$(date --reference "${APP_HOME}/target/${APP_TGZ_FILE}" +"%Y%m%d%H%M%S")
        local backup_file=$(basename ${APP_TGZ_FILE} .tgz)".$tgz_time"".tgz"
        cp -f "${APP_HOME}/target/${APP_TGZ_FILE}" "${APP_HOME}/target/${backup_file}"
    fi
}

main() {
    now=`date "+%Y-%m-%d %H:%M:%S"`
    echo "$now--------------------------"

    echo "INFO: deploy log: ${APP_HOME}/logs/${APP_NAME}_deploy.log"

    case "$ACTION" in
        start)
            start
        ;;
        stop)
            stop
        ;;
        pubstart)
            stop
            start
            start_nginx
        ;;
        online)
            online
        ;;
        offline)
            offline
        ;;
        restart)
            stop
            start
            start_nginx
        ;;
        deploy)
            stop
            start
            start_nginx
            backup
        ;;
        *)
            usage
        ;;
    esac
}

main | tee -a ${APP_HOME}/logs/${APP_NAME}_deploy.log; check_first_pipe_exit_code;