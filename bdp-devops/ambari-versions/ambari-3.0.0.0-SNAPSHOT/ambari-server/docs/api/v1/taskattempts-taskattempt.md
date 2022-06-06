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

View Task Attempt Information
=====

[Back to Resources](index.md#resources)

Returns information about a single task attempt for a given job.

    GET /clusters/:name/workflows/:workflowid/jobs/:jobid/taskattempts/:taskattempt

**Response**

    200 OK
    {
        "href" : "http://your.ambari.server/api/v1/clusters/c1/workflows/mr_201305061943_0001/jobs/job_201305061943_0001/taskattempts/attempt_201305061943_0001_m_000000_0",
        "TaskAttempt" : {
            "cluster_name" : "c1",
            "workflow_id" : "mr_201305061943_0001",
            "job_id" : "job_201305061943_0001",
            "task_attempt_id" : "attempt_201305061943_0001_m_000000_0",
            "status" : "SUCCESS",
            "finish_time" : 1367883874107,
            "input_bytes" : 2009,
            "type" : "MAP",
            "output_bytes" : 61842,
            "shuffle_finish_time" : 0,
            "locality" : "NODE_LOCAL",
            "start_time" : 1367883871399,
            "sort_finish_fime" : 0,
            "map_finish_time" : 1367883874107
        }
    }
