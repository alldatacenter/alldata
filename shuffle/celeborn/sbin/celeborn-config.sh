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
