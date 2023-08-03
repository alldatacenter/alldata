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

unset HADOOP_CONF_DIR

# included in all the celeborn scripts with source command
# should not be executable directly
# also should not be passed any arguments, since we need original $*

# symlink and absolute path should rely on CELEBORN_HOME to resolve
if [ -z "${CELEBORN_HOME}" ]; then
  export CELEBORN_HOME="$(cd "`dirname "$0"`"/..; pwd)"
fi

export CELEBORN_CONF_DIR="${CELEBORN_CONF_DIR:-"${CELEBORN_HOME}/conf"}"

if [ -z "$CELEBORN_ENV_LOADED" ]; then
  export CELEBORN_ENV_LOADED=1

  if [ -f "${CELEBORN_CONF_DIR}/celeborn-env.sh" ]; then
    # Promote all variable declarations to environment (exported) variables
    set -a
    . "${CELEBORN_CONF_DIR}/celeborn-env.sh"
    set +a
  fi
fi

# Find the java binary
if [ -n "${JAVA_HOME}" ]; then
  export JAVA="${JAVA_HOME}/bin/java"
else
  if [ "$(command -v java)" ]; then
    export JAVA="java"
  else
    echo "JAVA_HOME is not set" >&2
    exit 1
  fi
fi

# Get log directory
if [ "$CELEBORN_LOG_DIR" = "" ]; then
  export CELEBORN_LOG_DIR="${CELEBORN_HOME}/logs"
fi
mkdir -p "$CELEBORN_LOG_DIR"
touch "$CELEBORN_LOG_DIR"/.celeborn_test > /dev/null 2>&1
TEST_LOG_DIR=$?
if [ "${TEST_LOG_DIR}" = "0" ]; then
  rm -f "$CELEBORN_LOG_DIR"/.celeborn_test
else
  chown "$CELEBORN_IDENT_STRING" "$CELEBORN_LOG_DIR"
fi

if [ "$CELEBORN_PID_DIR" = "" ]; then
  export CELEBORN_PID_DIR="${CELEBORN_HOME}/pids"
fi

