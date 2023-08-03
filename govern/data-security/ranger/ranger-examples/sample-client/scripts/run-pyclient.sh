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
# limitations under the License.

usage() {
  echo "usage: run-pyclient.sh
  -n <arg> url to connect to
  -h       show help."
  exit 1
}

cd venv || exit
source bin/activate
INSTALL="python3 ./../setup.py install"
$INSTALL >> build.log
PYTHON_CMD="python3 build/lib/pylib/sample_client.py"
while getopts "n:h" opt; do
  case $opt in
    n) URL=$OPTARG
	PYTHON_CMD="$PYTHON_CMD --url $URL"
	;;
    h) usage
	;;
    \?) echo -e \\n"Option -$OPTARG not allowed."
	usage
	;;
  esac
done

prompt="Sample Authentication User Name:"
read -p "$prompt" userName
prompt="Sample Authentication User Password:"
read -p "$prompt" -s password
printf "\n"
PYTHON_CMD="$PYTHON_CMD --username $userName --password $password"
printf "Python command : %s\n" "$PYTHON_CMD"
$PYTHON_CMD