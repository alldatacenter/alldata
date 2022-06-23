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

_conformance_job_search="My%20Conformance"

_conformance_job_exec_id=$(eval echo "$(curl -s $_consumer_url/execution-events?searchTerm="$_conformance_job_search" | jq ".items[0].executionPlanId")")
_mdr_source=$(curl -s $_consumer_url/lineage-detailed?execId="$_conformance_job_exec_id" | jq ".executionPlan.inputs[].source" | grep CZK)
_affected_job_timestamp=$(curl -s $_consumer_url/execution-events?searchTerm="$_conformance_job_search" | jq ".items[0].timestamp")
_file_replace_timestamp=$((_affected_job_timestamp - 1000))

# Prepare execution plan
_exec_plan_json=$(
  cat <<END
{
  "id": "$(uuidgen)",
  "name": "Manual File Replacement",
  "agentInfo": {
    "name": "Bash",
    "version": "$BASH_VERSION"
  },
  "systemInfo": {
    "name": "Manual",
    "version": "0.0.0"
  },
  "operations": {
    "write": {
      "id": 0,
      "outputSource": $_mdr_source,
      "append": false,
      "childIds": [1]
    },
    "reads": [{
      "id": 1,
      "inputSources": [$_mdr_source]
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
  "timestamp": $_file_replace_timestamp
}]
END
)

# POST execution event
curl -d "$_exec_event_json" -H 'Content-Type: application/vnd.absa.spline.producer.v1.1+json' $_producer_url/execution-events

echo "
  Non-Spark Lineage recorded:
    Execution Plan ID: $_exec_plan_id
    Execution Timestamp: $_file_replace_timestamp
"
