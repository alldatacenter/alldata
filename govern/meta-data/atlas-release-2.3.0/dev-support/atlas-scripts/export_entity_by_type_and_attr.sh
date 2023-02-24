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
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

#
# Export an entity by type and given attr=value
#

realScriptDir=$(cd "$(dirname "$0")"; pwd)

source ${realScriptDir}/env_atlas.sh
source ./env_atlas.sh

typeName=$1
attrName=$2
attrValue=$3
matchType=$4
fetchType=$5
outputFileName=$6


function checkUsage() {
  if [ "${typeName}" == "" -o "${attrName}" == "" -o "${attrValue}" == "" ]
  then
    echo "Usage: $0 typeName attrName attrValue [matchType] [fetchType] [outputFileName]"
    exit 1
  fi

  if [ "${outputFileName}" == "" ]
  then
	outputFileName=`getDataFilePath "export-${typeName}-${attrName}.zip"`
  fi

  if [ "${matchType}" == "" ]
  then
    matchType="equals"
  fi

  if [ "${fetchType}" == "" ]
  then
    fetchType="connected"
  fi
}
checkUsage

postBody="{ \"itemsToExport\": [ { \"typeName\": \"${typeName}\", \"uniqueAttributes\": { \"${attrName}\": \"${attrValue}\" } } ], \"options\": { \"matchType\":\"${matchType}\", \"fetchType\":\"${fetchType}\" }}"

tmpFileName=/tmp/export_$$.txt

echo ${postBody} >> ${tmpFileName}

${CURL_CMDLINE} -X POST -u ${ATLAS_USER}:${ATLAS_PASS} -H "Accept: application/zip" -H "Content-Type: application/json" ${ATLAS_URL}/api/atlas/admin/export -d @${tmpFileName} > ${outputFileName}
ret=$?

if [ $ret == 0 ]
then
  echo "saved to ${outputFileName}"
else
  echo "failed with error code: ${ret}"
fi
