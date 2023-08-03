#!/bin/sh

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

set -
WORKDIR=$(
  cd "$(dirname "$0")" || exit
  pwd
)

DOCKER_VERSION=1.0.0-snapshot

# build code
code() {
  /bin/sh $WORKDIR/mvnw clean package -DskipTests
  # mv release zip
  mv $WORKDIR/seatunnel-web-dist/target/apache-seatunnel-web-1.0.0-SNAPSHOT.zip $WORKDIR/
}

# build image
image() {
  docker buildx build --load --no-cache -t apache/seatunnel-web:$DOCKER_VERSION -t apache/seatunnel-web:latest -f $WORKDIR/docker/backend.dockerfile .
}

# main
case "$1" in
"code")
  code
  ;;
"image")
  image
  ;;
*)
  echo "Usage: seatunnel-daemon.sh {start|stop|status}"
  exit 1
  ;;
esac
set +
