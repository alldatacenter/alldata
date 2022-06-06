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

List Task Attempts
=====

[Back to Resources](index.md#resources)

Returns a collection of all task attempts for a given job.

    GET /clusters/:name/workflows/:workflowid/jobs/:jobid/taskattempts

**Response**

    200 OK
    {
        "href" : "http://your.ambari.server/api/v1/clusters/c1/workflows/mr_201305061943_0001/jobs/job_201305061943_0001/taskattempts",
        "items" : [
            {
                "href" : "http://your.ambari.server/api/v1/clusters/c1/workflows/mr_201305061943_0001/jobs/job_201305061943_0001/taskattempts/attempt_201305061943_0001_m_000001_0",
                "TaskAttempt" : {
                    "cluster_name" : "c1",
                    "task_attempt_id" : "attempt_201305061943_0001_m_000001_0",
                    "workflow_id" : "mr_201305061943_0001",
                    "job_id" : "job_201305061943_0001"
                }
            },
            {
                "href" : "http://your.ambari.server/api/v1/clusters/c1/workflows/mr_201305061943_0001/jobs/job_201305061943_0001/taskattempts/attempt_201305061943_0001_m_000002_0",
                "TaskAttempt" : {
                    "cluster_name" : "c1",
                    "task_attempt_id" : "attempt_201305061943_0001_m_000002_0",
                    "workflow_id" : "mr_201305061943_0001",
                    "job_id" : "job_201305061943_0001"
                }
            },
            {
                "href" : "http://your.ambari.server/api/v1/clusters/c1/workflows/mr_201305061943_0001/jobs/job_201305061943_0001/taskattempts/attempt_201305061943_0001_r_000000_0",
                "TaskAttempt" : {
                    "cluster_name" : "c1",
                    "task_attempt_id" : "attempt_201305061943_0001_r_000000_0",
                    "workflow_id" : "mr_201305061943_0001",
                    "job_id" : "job_201305061943_0001"
                }
            },
            {
                "href" : "http://your.ambari.server/api/v1/clusters/c1/workflows/mr_201305061943_0001/jobs/job_201305061943_0001/taskattempts/attempt_201305061943_0001_m_000000_0",
                "TaskAttempt" : {
                    "cluster_name" : "c1",
                    "task_attempt_id" : "attempt_201305061943_0001_m_000000_0",
                    "workflow_id" : "mr_201305061943_0001",
                    "job_id" : "job_201305061943_0001"
                }
            }
        ]
    }
