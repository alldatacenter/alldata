#!/usr/bin/env bash
#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# Starts the celeborn master on the machine this script is executed on.

if [ -z "${CELEBORN_HOME}" ]; then
  export CELEBORN_HOME="$(cd "`dirname "$0"`"/..; pwd)"
fi

. "${CELEBORN_HOME}/sbin/load-celeborn-env.sh"

if [ "$CELEBORN_MASTER_MEMORY" = "" ]; then
  CELEBORN_MASTER_MEMORY="1g"
fi

CELEBORN_JAVA_OPTS="$CELEBORN_MASTER_JAVA_OPTS"
CELEBORN_JAVA_OPTS="$CELEBORN_JAVA_OPTS -Xmx$CELEBORN_MASTER_MEMORY"
CELEBORN_JAVA_OPTS="$CELEBORN_JAVA_OPTS -Dio.netty.tryReflectionSetAccessible=true"
CELEBORN_JAVA_OPTS="$CELEBORN_JAVA_OPTS --illegal-access=warn"
CELEBORN_JAVA_OPTS="$CELEBORN_JAVA_OPTS --add-opens=java.base/java.lang=ALL-UNNAMED"
CELEBORN_JAVA_OPTS="$CELEBORN_JAVA_OPTS --add-opens=java.base/java.lang.invoke=ALL-UNNAMED"
CELEBORN_JAVA_OPTS="$CELEBORN_JAVA_OPTS --add-opens=java.base/java.lang.reflect=ALL-UNNAMED"
CELEBORN_JAVA_OPTS="$CELEBORN_JAVA_OPTS --add-opens=java.base/java.io=ALL-UNNAMED"
CELEBORN_JAVA_OPTS="$CELEBORN_JAVA_OPTS --add-opens=java.base/java.net=ALL-UNNAMED"
CELEBORN_JAVA_OPTS="$CELEBORN_JAVA_OPTS --add-opens=java.base/java.nio=ALL-UNNAMED"
CELEBORN_JAVA_OPTS="$CELEBORN_JAVA_OPTS --add-opens=java.base/java.util=ALL-UNNAMED"
CELEBORN_JAVA_OPTS="$CELEBORN_JAVA_OPTS --add-opens=java.base/java.util.concurrent=ALL-UNNAMED"
CELEBORN_JAVA_OPTS="$CELEBORN_JAVA_OPTS --add-opens=java.base/java.util.concurrent.atomic=ALL-UNNAMED"
CELEBORN_JAVA_OPTS="$CELEBORN_JAVA_OPTS --add-opens=java.base/jdk.internal.misc=ALL-UNNAMED"
CELEBORN_JAVA_OPTS="$CELEBORN_JAVA_OPTS --add-opens=java.base/sun.nio.ch=ALL-UNNAMED"
CELEBORN_JAVA_OPTS="$CELEBORN_JAVA_OPTS --add-opens=java.base/sun.nio.cs=ALL-UNNAMED"
CELEBORN_JAVA_OPTS="$CELEBORN_JAVA_OPTS --add-opens=java.base/sun.security.action=ALL-UNNAMED"
CELEBORN_JAVA_OPTS="$CELEBORN_JAVA_OPTS --add-opens=java.base/sun.util.calendar=ALL-UNNAMED"
export CELEBORN_JAVA_OPTS="$CELEBORN_JAVA_OPTS"

exec "${CELEBORN_HOME}/sbin/celeborn-daemon.sh" start org.apache.celeborn.service.deploy.master.Master 1 "$@"