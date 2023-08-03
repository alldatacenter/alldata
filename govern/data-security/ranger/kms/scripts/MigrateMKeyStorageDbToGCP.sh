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
if [ -z "${JAVA_HOME}" ]; then
   echo "PLEASE EXPORT VARAIBLE JAVA_HOME"
exit;
else
   echo "JAVA_HOME : "$JAVA_HOME
fi

if [ -z "${RANGER_KMS_HOME}" ]; then
   echo "PLEASE EXPORT VARAIBLE RANGER_KMS_HOME"
exit;
else
   echo "RANGER_KMS_HOME : "$RANGER_KMS_HOME
fi

if [ -z "${RANGER_KMS_CONF}" ]; then
   echo "PLEASE EXPORT VARAIBLE RANGER_KMS_CONF"
exit;
else
   echo "RANGER_KMS_CONF : "$RANGER_KMS_CONF
fi

if [ -z "${SQL_CONNECTOR_JAR}" ]; then
   echo "PLEASE EXPORT VARAIBLE SQL_CONNECTOR_JAR"
exit;
else
   echo "SQL_CONNECTOR_JAR : "$SQL_CONNECTOR_JAR
fi

cp="${RANGER_KMS_HOME}/cred/lib/*:${RANGER_KMS_CONF}:${RANGER_KMS_HOME}/ews/webapp/WEB-INF/classes/lib/*:${SQL_CONNECTOR_JAR}:${RANGER_KMS_HOME}/ews/webapp/config:${RANGER_KMS_HOME}/ews/lib/*:${RANGER_KMS_HOME}/ews/webapp/lib/*:${RANGER_KMS_HOME}/ews/webapp/META-INF:${RANGER_KMS_CONF}/*"
${JAVA_HOME}/bin/java -cp "${cp}" org.apache.hadoop.crypto.key.MigrateDBMKeyToGCP ${1} ${2} ${3} ${4} ${5}
