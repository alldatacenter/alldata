#!/bin/sh

set -e
set -x

export JVM_XMX="256m"

exec java -Xmx${JVM_XMX} -Xms${JVM_XMX} -XX:ActiveProcessorCount=2 -Dloader.path=/app/ -jar /app/action.jar --spring.config.location=/app/