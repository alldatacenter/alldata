#!/usr/bin/env bash
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

set -e               # exit on error

cd "$(dirname "$0")" # connect to root

DOCKER_DIR=dev-support/docker
DOCKER_FILE="${DOCKER_DIR}/Dockerfile"

CONTAINER_NAME=drill-dev-${USER}-$$

#CPU_ARCH=$(echo "$MACHTYPE" | cut -d- -f1)
#if [ "$CPU_ARCH" = "aarch64" ]; then
#  DOCKER_FILE="${DOCKER_DIR}/Dockerfile_aarch64"
#fi

docker build -t drill-build -f $DOCKER_FILE $DOCKER_DIR

USER_NAME=${SUDO_USER:=$USER}
USER_ID=$(id -u "${USER_NAME}")

if [ "$(uname -s)" = "Darwin" ]; then
  GROUP_ID=100
fi

if [ "$(uname -s)" = "Linux" ]; then
  GROUP_ID=$(id -g "${USER_NAME}")
  # man docker-run
  # When using SELinux, mounted directories may not be accessible
  # to the container. To work around this, with Docker prior to 1.7
  # one needs to run the "chcon -Rt svirt_sandbox_file_t" command on
  # the directories. With Docker 1.7 and later the z mount option
  # does this automatically.
  if command -v selinuxenabled >/dev/null && selinuxenabled; then
    DCKR_VER=$(docker -v|
    awk '$1 == "Docker" && $2 == "version" {split($3,ver,".");print ver[1]"."ver[2]}')
    DCKR_MAJ=${DCKR_VER%.*}
    DCKR_MIN=${DCKR_VER#*.}
    if [ "${DCKR_MAJ}" -eq 1 ] && [ "${DCKR_MIN}" -ge 7 ] ||
        [ "${DCKR_MAJ}" -gt 1 ]; then
      V_OPTS=:z
    else
      for d in "${PWD}" "${HOME}/.m2"; do
        ctx=$(stat --printf='%C' "$d"|cut -d':' -f3)
        if [ "$ctx" != svirt_sandbox_file_t ] && [ "$ctx" != container_file_t ]; then
          printf 'INFO: SELinux is enabled.\n'
          printf '\tMounted %s may not be accessible to the container.\n' "$d"
          printf 'INFO: If so, on the host, run the following command:\n'
          printf '\t# chcon -Rt svirt_sandbox_file_t %s\n' "$d"
        fi
      done
    fi
  fi
fi

# Set the home directory in the Docker container.
DOCKER_HOME_DIR=${DOCKER_HOME_DIR:-/home/${USER_NAME}}

DOCKER_GROUP_ID=$(getent group docker | cut -d':' -f3)

docker build -t "drill-build-${USER_ID}" - <<UserSpecificDocker
FROM drill-build
RUN rm -f /var/log/faillog /var/log/lastlog
RUN groupadd --non-unique -g ${GROUP_ID} ${USER_NAME}
RUN groupmod -g ${DOCKER_GROUP_ID} docker
RUN useradd -g ${GROUP_ID} -G docker -u ${USER_ID} -k /root -m ${USER_NAME} -d "${DOCKER_HOME_DIR}"
RUN echo "${USER_NAME} ALL=NOPASSWD: ALL" > "/etc/sudoers.d/drill-build-${USER_ID}"
ENV HOME "${DOCKER_HOME_DIR}"

UserSpecificDocker

echo ""
echo "Docker image build completed."
echo "=============================================================================================="
echo ""

#If this env varible is empty, docker will be started
# in non interactive mode
DOCKER_INTERACTIVE_RUN=${DOCKER_INTERACTIVE_RUN-"-i -t"}

DOCKER_SOCKET_MOUNT=""
if [ -S /var/run/docker.sock ];
then
  DOCKER_SOCKET_MOUNT="-v /var/run/docker.sock:/var/run/docker.sock${V_OPTS:-}"
  echo "Enabling Docker support with the docker build environment."
else
  echo "There is NO Docker support with the docker build environment."
fi

COMMAND=( "$@" )
if [ $# -eq 0 ];
then
  COMMAND=( "bash" )
fi

[ -d "${HOME}/.gradle_drill_build_env" ] || mkdir -p "${HOME}/.gradle_drill_build_env"

# By mapping the .m2 directory you can do an mvn install from
# within the container and use the result on your normal
# system.  And this also is a significant speedup in subsequent
# builds because the dependencies are downloaded only once.
docker run --rm=true ${DOCKER_INTERACTIVE_RUN}                         \
           --name "${CONTAINER_NAME}"                                  \
           -v "${HOME}/.m2:${DOCKER_HOME_DIR}/.m2${V_OPTS:-}"          \
           -v "${HOME}/.gnupg:${DOCKER_HOME_DIR}/.gnupg${V_OPTS:-}"    \
           -v "${HOME}/.gradle_drill_build_env:${DOCKER_HOME_DIR}/.gradle${V_OPTS:-}"  \
           -v "${PWD}:${DOCKER_HOME_DIR}/drill${V_OPTS:-}"              \
           -w "${DOCKER_HOME_DIR}/drill"                                \
           ${DOCKER_SOCKET_MOUNT}                                      \
           -u "${USER_ID}"                                             \
           "drill-build-${USER_ID}" "${COMMAND[@]}"
