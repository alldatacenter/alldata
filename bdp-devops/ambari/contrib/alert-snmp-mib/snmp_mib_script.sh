#!/bin/bash
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
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

HOST=localhost
COMMUNITY=public

STATE=0
if [ $4 == "OK" ]; then
  STATE=0
elif [ $4 == "UNKNOWN" ]; then
  STATE=1
elif [ $4 == "WARNING" ]; then
  STATE=2
elif [ $4 == "CRITICAL" ]; then
  STATE=3
fi

/usr/bin/snmptrap -v 2c -c $COMMUNITY $HOST '' APACHE-AMBARI-MIB::apacheAmbariAlert alertDefinitionName s "$1" alertName s "$2" alertText s "$5" alertState i $STATE alertService s "$3"
