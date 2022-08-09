<!---
Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements. See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
The ASF licenses this file to You under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-->

# Request Schedule
A request schedule resource identifies a batch of request sub resources to be executed according to a schedule or a point in time execution if no schedule is specified. The schedule determines the start time for the batch of requests.
A batch request is a sub-resource of the request schedule and represents the Http request that will be executed on the server. It captures the request properties along with the status, return code and an optional request id for long running/asynchronous operations.

Example list of all request schedules defined for a cluster:

    GET /api/v1/clusters/c1/request_schedules
    {
      "href" : "http://your.ambari.server/api/v1/clusters/c1/request_schedules",
      "items" : [
        {
          "href" : "http://your.ambari.server/api/v1/clusters/c1/request_schedules/2",
          "RequestSchedule" : {
            "cluster_name" : "c1",
            "id" : 2
          }
        }
      ]
    }

All batch requests are executed linearly ordered by the order_id field and if a batch request fails, subsequent batch requests will no longer be executed.

Example to create a request schedule with batch requests to perform Rolling restarts for a set of 3 hosts at a time

    POST /api/v1/clusters/c1/request_schedules

    [
       {
          "RequestSchedule": {
             "batch": [
                {
                   "requests": [
                      {
                         "order_id": 1,
                         "request_type": "POST",
                         "request_uri": "/api/v1/clusters/c1/requests",
                         "RequestBodyInfo": {
                            "RequestInfo": {
                               "context": "ROLLING-RESTART NODEMANAGERs batch 1 of 2",
                               "command": "RESTART",
                               "service_name": "YARN",
                               "component_name": "NODEMANAGER",
                               "hosts": "host1,host2,host3"
                            }
                         }
                      },
                      {
                         "order_id": 2,
                         "request_type": "POST",
                         "request_uri": "/api/v1/clusters/c1/requests",
                         "RequestBodyInfo": {
                            "RequestInfo": {
                               "context": "ROLLING-RESTART NODEMANAGERs batch 2 of 2",
                               "command": "RESTART",
                               "service_name": "YARN",
                               "component_name": "NODEMANAGER",
                               "hosts": "host4,host5,host6"
                            }
                         }
                      }
                   ]
                },
                {
                   "batch_settings": {
                      "batch_separation_in_seconds": 120,
                      "task_failure_tolerance": 1
                   }
                }
             ]
          }
       }
    ]

    201 Created

The batch_settings allow for specifying how much time in seconds should be the separation between two requests and a failure tolerance count which identifies the total number of failed tasks, if at all, resulting from the batch requests are acceptable before the batch execution is aborted.

Example to create a recurring request schedule.

    POST /api/v1/clusters/c1/request_schedules

    [
       {
          "RequestSchedule": {
             "batch": [
                {
                   "requests": [
                      {
                         "order_id": 1,
                         "request_type": "POST",
                         "request_uri": "/api/v1/clusters/c1/requests",
                         "RequestBodyInfo": {
                            "RequestInfo": {
                               "context": "ROLLING-RESTART NODEMANAGERs batch 1 of 1",
                               "command": "RESTART",
                               "service_name": "YARN",
                               "component_name": "NODEMANAGER",
                               "hosts": "host1,host2,host3"
                            }
                         }
                      }
                   ]
                },
                {
                   "batch_settings": {
                      "batch_separation_in_minutes": "15",
                      "task_failure_tolerance": "3"
                   }
                }
             ],
             "schedule": {
                "minutes": "30",
                "hours": "0",
                "days_of_month": "*",
                "month": "*",
                "day_of_week": "?",
                "startTime": "",
                "endTime": "2013-11-18T14:29:29-0800"
             }
          }
       }
    ]

    201 Created

**Schedule specification**

The schedule is a Quartz scheduler cron expression minus the seconds specifier.

<table>
  <tr>
    <th>Field</th>
    <th>Allowed values</th>
    <th>Allowed special characters</th>
  </tr>
  <tr>
    <td>Minutes</td>
    <td>0-59</td>
    <td>, - * /</td>
  </tr>
  <tr>
    <td>Hours</td>
    <td>0-23</td>
    <td>, - * /</td>
  </tr>
  <tr>
    <td>Day of month</td>
    <td>1-31</td>
    <td>, - * / L W</td>
  </tr>
  <tr>
    <td>Month</td>
    <td>1-12 or JAN-DEC</td>
    <td>, - * /</td>
  </tr>
  <tr>
    <td>Day of week</td>
    <td>1-7 or SUN-SAT</td>
    <td>, - * ? / L #</td>
  </tr>
  <tr>
    <td>Year</td>
    <td>empty, 1970-2099</td>
    <td>, - * /</td>
  </tr>
</table>

**Note**: A Request schedule is immutable, update on the resource is not allowed.

An example of GET call on a request schedule identified by the :id that has completed execution.

    GET /api/v1/clusters/c1/request_schedules/:id
    {
      "href" : "http://your.ambari.server/api/v1/clusters/c1/request_schedules/:id",
      "RequestSchedule" : {
        "batch" : {
          "batch_requests" : [
            {
              "order_id" : 1,
              "request_type" : "POST",
              "request_uri" : "/api/v1/clusters/c1/requests",
              "request_body" : {
                 "RequestInfo": {
                   "context": "ROLLING-RESTART NODEMANAGERs batch 1 of 1",
                   "command": "RESTART",
                   "service_name": "HDFS",
                   "component_name": "DATANODE",
                   "hosts": "host1,host2,host3"
                }
              }
            }
          ],
          "batch_settings" : {
            "batch_separation_in_seconds" : 10,
            "task_failure_tolerance_limit" : 1
          }
        },
        "cluster_name" : "c1",
        "description" : null,
        "id" : :id,
        "last_execution_status" : "COMPLETED",
        "schedule" : { },
        "status" : "COMPLETED"
      }
    }

    200 OK

A delete on request schedule results in marking the request schedule as DISABLED and suspends all future executions of the batch requests minus the currently executing batch request.

    DELETE /api/v1/clusters/c1/request_schedules/:id

    200 OK





