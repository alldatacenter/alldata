#! /bin/bash
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements. See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership. The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License. You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#

#======================================================================
# Project restart shell script
# First call shutdown.sh to stop the server
# Then call startup.sh to start the service
#======================================================================

# Project name
APPLICATION="InlongManagerMain"
echo restart ${APPLICATION} Application...

BIN_PATH=$(
  # shellcheck disable=SC2164
  cd "$(dirname $0)"
  pwd
)
echo "restart in" ${BIN_PATH}

# Stop service
bash +x "$BIN_PATH"/shutdown.sh

sleep 1s
echo "begin to execute the startup command..."

# Start service
bash +x "$BIN_PATH"/startup.sh
