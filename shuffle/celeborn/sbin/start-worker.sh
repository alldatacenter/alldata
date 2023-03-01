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

# Starts the celeborn worker on the machine this script is executed on.

if [ -z "${CELEBORN_HOME}" ]; then
  export CELEBORN_HOME="$(cd "`dirname "$0"`"/..; pwd)"
fi

. "${CELEBORN_HOME}/sbin/celeborn-config.sh"

if [ "$CELEBORN_WORKER_MEMORY" = "" ]; then
  CELEBORN_WORKER_MEMORY="1g"
fi

if [ "$CELEBORN_WORKER_OFFHEAP_MEMORY" = "" ]; then
  CELEBORN_WORKER_OFFHEAP_MEMORY="1g"
fi

export CELEBORN_JAVA_OPTS="-Xmx$CELEBORN_WORKER_MEMORY -XX:MaxDirectMemorySize=$CELEBORN_WORKER_OFFHEAP_MEMORY $CELEBORN_WORKER_JAVA_OPTS"
JAVA_VERSION=$(java -version 2>&1 | grep " version " | head -1 | awk '{print $3}' | tr -d '"')
if [[ ! "$JAVA_VERSION" == 1.8.* ]]; then
  export CELEBORN_JAVA_OPTS="${CELEBORN_JAVA_OPTS} --add-opens java.base/jdk.internal.misc=ALL-UNNAMED --illegal-access=warn -Dio.netty.tryReflectionSetAccessible=true"
fi

if [ "$WORKER_INSTANCE" = "" ]; then
  WORKER_INSTANCE=1
fi

"${CELEBORN_HOME}/sbin/celeborn-daemon.sh" start org.apache.celeborn.service.deploy.worker.Worker "$WORKER_INSTANCE" "$@"