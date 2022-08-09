#!/bin/bash
# ------------------------------------------------------------------------
# Copyright 2020 ABSA Group Limited
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
# ------------------------------------------------------------------------
#

SPLINE_GATEWAY_URL=http://localhost:8080

_producer_url=$SPLINE_GATEWAY_URL/producer
_consumer_url=$SPLINE_GATEWAY_URL/consumer

_job1_search="Jan%27s%20beer%20job"
_job2_search="Marek%27s%20job"

_job1_output_datasource=$(curl -s $_consumer_url/execution-events?searchTerm="$_job1_search" | jq ".items[0].dataSourceUri")
_job1_execute_timestamp=$(curl -s $_consumer_url/execution-events?searchTerm="$_job1_search" | jq ".items[0].timestamp")
_job2_execute_timestamp=$(curl -s $_consumer_url/execution-events?searchTerm="$_job2_search" | jq ".items[0].timestamp")
_new_job_exec_timestamp=$(((_job1_execute_timestamp + _job2_execute_timestamp) / 2))

# Prepare execution plan
_exec_plan_json=$(
  cat <<END
{
  "id": "$(uuidgen)",
  "name": "Dummy Beer Job",
  "systemInfo": {
    "name": "Foo Bar",
    "version": "0.0.0"
  },
  "operations": {
    "write": {
      "id": 0,
      "outputSource": $_job1_output_datasource,
      "append": false,
      "childIds": [1]
    },
    "other": [{
      "name": "Filter",
      "id": 1,
      "childIds": [2]
    }],
    "reads": [{
      "id": 2,
      "inputSources": [$_job1_output_datasource]
    }]
  }
}
END
)

# POST execution plan
_exec_plan_id=$(curl -s -d "$_exec_plan_json" -H 'Content-Type: application/vnd.absa.spline.producer.v1.1+json' $_producer_url/execution-plans)

# Prepare execution event
_exec_event_json=$(
  cat <<END
[{
  "planId": $_exec_plan_id,
  "timestamp": $_new_job_exec_timestamp
}]
END
)

# POST execution event
curl -d "$_exec_event_json" -H 'Content-Type: application/vnd.absa.spline.producer.v1.1+json' $_producer_url/execution-events

echo "
  Non-Spark Lineage recorded:
    Execution Plan ID: $_exec_plan_id
    Execution Timestamp: $_new_job_exec_timestamp
"
