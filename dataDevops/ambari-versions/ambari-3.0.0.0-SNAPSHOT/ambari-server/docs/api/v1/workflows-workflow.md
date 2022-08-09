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

View Workflow Information
=====

[Back to Resources](index.md#resources)

Returns information about a single workflow in a given cluster.

    GET /clusters/:name/workflows/:workflowid

**Response**

    200 OK
    {
        "href" : "http://your.ambari.server/api/v1/clusters/c1/workflows/mr_201305061943_0001",
        "Workflow" : {
            "cluster_name" : "c1",
            "workflow_id" : "mr_201305061943_0001"
            "last_update_time" : 1367883887044,
            "input_bytes" : 2009,
            "output_bytes" : 1968,
            "user_name" : "ambari-qa",
            "elapsed_time" : 25734,
            "num_jobs_total" : 1,
            "num_jobs_completed" : 1,
            "name" : "word count",
            "context" : "{\"workflowId\":null,\"workflowName\":null,\"workflowDag\":{\"entries\":[{\"source\":\"X\",\"targets\":[]}]},\"parentWorkflowContext\":null,\"workflowEntityName\":null}",
            "start_time" : 1367883861310,
            "parent_id" : null
        },
        "jobs" : [
            {
                "href" : "http://your.ambari.server/api/v1/clusters/c1/workflows/mr_201305061943_0001/jobs/job_201305061943_0001",
                "Job" : {
                    "cluster_name" : "c1",
                    "workflow_id" : "mr_201305061943_0001",
                    "job_id" : "job_201305061943_0001"
                }
            }
        ]
    }
