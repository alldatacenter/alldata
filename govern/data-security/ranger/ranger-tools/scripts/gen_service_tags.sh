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

echo_stderr ()
{
    echo "$@" >&2
}

if [ $# -ne 3 ]
then
	echo_stderr "usage: $0 <service_name> <num_of_service_resources> <initial_id>"
	exit 1
fi

service_name=$1
num_of_service_resources=$2
initial_id=$3

echo_stderr "Assuming service_name=${service_name}, num_of_service_resources=${num_of_service_resources} initial_id=${initial_id}"

echo "{
  \"op\": \"add_or_update\",
  \"serviceName\": \"${service_name}\",
  \"tagVersion\": 100,
  \"tagDefinitions\": {
    \"1\": {
      \"name\": \"EXPIRES_ON\",
      \"source\": \"Internal\",
      \"attributeDefs\": [
        {
          \"name\": \"activates_on\",
          \"type\": \"datetime\"
        },
        {
          \"name\": \"expiry_date\",
          \"type\": \"datetime\"
        }
      ],
      \"id\": 1,
      \"isEnabled\": true
    }
  },
  \"tags\": {"
for ((i = ${initial_id}; i < ${initial_id} + $num_of_service_resources; i++)); do
    if [ $i -ne ${initial_id} ]
    then
         echo "  ,"
    fi
    echo "  \"${i}\": {
       \"type\": \"EXPIRES_ON\",
        \"attributes\": {
          \"expiry_date\": \"2027/12/31\",
          \"activates_on\": \"2020/01/01\"
        },
        \"id\": ${i},
        \"isEnabled\": true
      }"
done
  echo "  },"
echo "  \"serviceResources\": ["
for ((i = ${initial_id}; i < ${initial_id} + $num_of_service_resources; i++)); do
    if [ $i -ne ${initial_id} ]
    then
       echo "  ,"
    fi
    echo "   { \"resourceElements\": {
        \"database\": { \"values\": [ \"finance_${i}\" ], \"isExcludes\": false, \"isRecursive\": false },
        \"table\": { \"values\": [ \"tax_2020_${i}\" ], \"isExcludes\": false, \"isRecursive\": false }
      },
      \"serviceName\": \"${service_name}\",
      \"id\": ${i},
      \"isEnabled\": true
   }"
done
echo "  ],"
echo "  \"resourceToTagIds\": {"
for ((i = ${initial_id}; i < ${initial_id} + $num_of_service_resources; i++)); do
    if [ $i -ne ${initial_id} ]
    then
       echo "  ,"
    fi
    echo "    \"${i}\": [ ${i} ]"
done
echo "  }
}"
