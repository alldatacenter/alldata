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
# -------------------------------------------------------------------------------------
if [ "$JAVA_HOME" != "" ]; then
    export PATH=$JAVA_HOME/bin:$PATH
else
    exit ;
fi
RANGER_KMS_HOME=`dirname $0`
cp="${RANGER_KMS_HOME}/cred/lib/*:${RANGER_KMS_HOME}/ews/webapp/WEB-INF/classes/conf/:${RANGER_KMS_HOME}/ews/webapp/WEB-INF/classes:${RANGER_KMS_HOME}/ews/webapp/WEB-INF/lib/*:${RANGER_KMS_HOME}/ews/lib/*:${RANGER_KMS_HOME}/ews/webapp/META-INF"
java -cp "${cp}" org.apache.hadoop.crypto.key.Ranger2JKSUtil ${1} ${2}
