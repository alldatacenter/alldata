#!/bin/bash

APP_TGZ="/home/admin/"$APP_NAME"/target/"$APP_NAME".tgz"
APP_RELEASE="/tmp/"$APP_NAME".release"
if [ -f "$APP_RELEASE" ] ; then
        rm -rf "$APP_RELEASE" || exit
fi
tar tvf $APP_TGZ|grep -q ""$APP_NAME".release"
if [ $? == 0 ] ; then
     tar zxf $APP_TGZ -C /tmp $APP_NAME".release" || exit
fi
if [ -f "$APP_RELEASE" ] && grep -q 'baseline.jdk' "$APP_RELEASE" ; then
        command -v dos2unix >/dev/null 2>&1 && dos2unix "$APP_RELEASE"
        BASELINE_JDK=`grep "baseline.jdk" "$APP_RELEASE" | awk -F'=' '{print $2}'|tr -d ' '`
        if [ ! -d "/opt/taobao/install/$BASELINE_JDK" ]; then
            echo "Error: baseline.jdk $BASELINE_JDK specified in ${APP_NAME}.release is not installed."
            exit 1;
        fi
        export JAVA_HOME=/opt/taobao/install/"$BASELINE_JDK"
        JAVA_VERSION=`$JAVA_HOME"/bin/java" -version 2>&1` || { echo $JAVA_HOME"/bin/java -version failed"; exit 1; }
        JAVA_VERSION_MAJOR=${JAVA_VERSION:16:1}
        if [ "$JAVA_VERSION_MAJOR" -ge 8 ]; then
                CATALINA_OPTS="${CATALINA_OPTS//PermSize/MetaspaceSize}"
                CATALINA_OPTS="${CATALINA_OPTS//MaxPermSize/MaxMetaspaceSize}"
                CATALINA_OPTS="${CATALINA_OPTS//-XX:+UseCMSCompactAtFullCollection/}"
                SERVICE_OPTS="${SERVICE_OPTS//PermSize/MetaspaceSize}"
                SERVICE_OPTS="${SERVICE_OPTS//MaxPermSize/MaxMetaspaceSize}"
                SERVICE_OPTS="${SERVICE_OPTS//-XX:+UseCMSCompactAtFullCollection/}"
                export CATALINA_OPTS
        fi
fi
