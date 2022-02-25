#!/bin/bash
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and

SUDO_BINARY="/usr/bin/sudo"

if [[ $# -eq 0 ]] ; then
  echo 'usage: ambari-sudo.sh [sudo_arg1, sudo_arg2 ...] command [arg1, arg2 ...]'
  exit 1
fi

# if user is non-root
if [ "$EUID" -ne 0 ] ; then
  $SUDO_BINARY "$@"
else
  ENV=()
  SUDO_ARGS=()

  for i ; do
    if [[ "$i" == *"="* ]] ; then
      ENV+=("$i")
      shift
    elif [[ "$i" == "-"* ]] ; then
      SUDO_ARGS+=("$i")
      shift
    else
      break
    fi
  done
  
  #echo "sudo arguments: ${SUDO_ARGS[@]}"
  #echo "env: ${ENV[@]}"
  #echo "args: $@"

  if [ "$ENV" ] ; then
    export "${ENV[@]}"
  fi

  "$@"
fi