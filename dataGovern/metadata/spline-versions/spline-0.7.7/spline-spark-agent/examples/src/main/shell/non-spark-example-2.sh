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

_jan_job_search="Jan%27s%20beer%20job"

_jan_job_exec_id=$(eval echo "$(curl -s $_consumer_url/execution-events?searchTerm="$_jan_job_search" | jq ".items[0].executionPlanId")")
_jan_job_timestamp=$(curl -s $_consumer_url/execution-events?searchTerm="$_jan_job_search" | jq ".items[0].timestamp")
_beer_source=$(curl -s $_consumer_url/lineage-detailed?execId="$_jan_job_exec_id" | jq ".executionPlan.inputs[].source" | grep beer)
_new_job_timestamp=$((_jan_job_timestamp - 1000))

# Prepare execution plan
_exec_plan_json=$(
  cat <<END
{
  "id": "$(uuidgen)",
  "name": "Beer Consumption Stats",
  "systemInfo": {
    "name": "Foo Bar",
    "version": "0.0.0"
  },
  "operations": {
    "reads": [
      {
        "id": 10,
        "inputSources": ["hdfs://.../country_codes"],
        "name": "Read - Country Codes"
      },
      {
        "id": 11,
        "inputSources": ["hdfs://.../beer_consum_2003.csv"],
        "name": "Get stats (year 2003)"
      },
      {
        "id": 12,
        "inputSources": ["hdfs://.../beer_consum_2004.csv"],
        "name": "Get stats (year 2004)"
      },
      {
        "id": 13,
        "inputSources": ["hdfs://.../beer_consum_2005.csv"],
        "name": "Get stats (year 2005)"
      },
      {
        "id": 14,
        "inputSources": ["hdfs://.../beer_consum_2006.csv"],
        "name": "Get stats (year 2006)"
      },
      {
        "id": 15,
        "inputSources": ["hdfs://.../beer_consum_2007.csv"],
        "name": "Get stats (year 2007)"
      },
      {
        "id": 16,
        "inputSources": ["hdfs://.../beer_consum_2008.csv"],
        "name": "Get stats (year 2008)"
      },
      {
        "id": 17,
        "inputSources": ["hdfs://.../beer_consum_2009.csv"],
        "name": "Get stats (year 2009)"
      },
      {
        "id": 18,
        "inputSources": ["hdfs://.../beer_consum_2010.csv"],
        "name": "Get stats (year 2010)"
      },
      {
        "id": 19,
        "inputSources": ["hdfs://.../beer_consum_2011.csv"],
        "name": "Get stats (year 2011)"
      }
    ],
    "write": {
      "id": 0,
      "outputSource": $_beer_source,
      "append": false,
      "childIds": [1]
    },
    "other": [
      {
        "id": 1,
        "childIds": [2, 19],
        "name": "Join"
      },
      {
        "id": 2,
        "childIds": [3, 18],
        "name": "Join"
      },
      {
        "id": 3,
        "childIds": [4, 17],
        "name": "Join"
      },
      {
        "id": 4,
        "childIds": [5, 16],
        "name": "Join"
      },
      {
        "id": 5,
        "childIds": [6, 15],
        "name": "Join"
      },
      {
        "id": 6,
        "childIds": [7, 14],
        "name": "Join"
      },
      {
        "id": 7,
        "childIds": [8, 13],
        "name": "Join"
      },
      {
        "id": 8,
        "childIds": [9, 12],
        "name": "Join"
      },
      {
        "id": 9,
        "childIds": [10, 11],
        "name": "Join"
      }
    ]
  }
}
END
)

#echo $_exec_plan_json


# POST execution plan
_exec_plan_id=$(curl -s -d "$_exec_plan_json" -H 'Content-Type: application/vnd.absa.spline.producer.v1.1+json' $_producer_url/execution-plans)

# Prepare execution event
_exec_event_json=$(
  cat <<END
[{
  "planId": $_exec_plan_id,
  "timestamp": $_new_job_timestamp
}]
END
)

# POST execution event
curl -d "$_exec_event_json" -H 'Content-Type: application/vnd.absa.spline.producer.v1.1+json' $_producer_url/execution-events

echo "
  Non-Spark Lineage recorded:
    Execution Plan ID: $_exec_plan_id
    Execution Timestamp: $_new_job_timestamp
"
