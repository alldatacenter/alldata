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

# Task Resources
 
 
### API Summary

- [List tasks](tasks.md)

### Properties

<table>
  <tr>
    <th>Property</th>
    <th>Description</th>
  </tr>
  <tr>
    <td>Tasks/id</td>
    <td>The task id</td>  
  </tr>
  <tr>
    <td>Tasks/request_id</td>
    <td>The parent request id</td>  
  </tr>
  <tr>
    <td>Tasks/cluster_name</td>
    <td>The name of the parent cluster</td>  
  </tr>
  <tr>
    <td>Tasks/attempt_cnt</td>
    <td>The number of attempts at completing this task</td>  
  </tr>
  <tr>
    <td>Tasks/command</td>
    <td>The task command</td>  
  </tr>
  <tr>
    <td>Tasks/exit_code</td>
    <td>The exit code</td>  
  </tr>
  <tr>
    <td>Tasks/host_name</td>
    <td>The name of the host</td>  
  </tr>
  <tr>
    <td>Tasks/role</td>
    <td>The role</td>  
  </tr>
  <tr>
    <td>Tasks/stage_id</td>
    <td>The stage id</td>  
  </tr>
  <tr>
    <td>Tasks/start_time</td>
    <td>The task start time</td>  
  </tr>
  <tr>
    <td>Tasks/status</td>
    <td>The task status</td>  
  </tr>
  <tr>
    <td>Tasks/stderr</td>
    <td>The stderr from running the taks</td>  
  </tr>
  <tr>
    <td>Tasks/stdout</td>
    <td>The stdout from running the task</td>  
  </tr>
</table>


### Status

The current status of a task resource can be determined by looking at the Tasks/status property.


    GET api/v1/clusters/c1/requests/2/tasks/12?fields=Tasks/status

    200 OK
    {
      "href" : "your.ambari.server/api/v1/clusters/c1/requests/2/tasks/12?fields=Tasks/status",
      "Tasks" : {
      "cluster_name" : "c1",
      "id" : 12,
      "request_id" : 2,
      "status" : "COMPLETED"
    }

The following table lists the possible values of the task resource Tasks/status.
<table>
  <tr>
    <th>State</th>
    <th>Description</th>
  </tr>
  <tr>
    <td>PENDING</td>
    <td>Not queued for a host.</td>  
  </tr>
  <tr>
    <td>QUEUED</td>
    <td>Queued for a host.</td>  
  </tr>
  <tr>
    <td>IN_PROGRESS</td>
    <td>Host reported it is working.</td>  
  </tr>
  <tr>
    <td>COMPLETED</td>
    <td>Host reported success.</td>  
  </tr>
  <tr>
    <td>FAILED</td>
    <td>Failed.</td>  
  </tr>
  <tr>
    <td>TIMEDOUT</td>
    <td>Host did not respond in time.</td>  
  </tr>
  <tr>
    <td>ABORTED</td>
    <td>Operation was abandoned.</td>  
  </tr>
</table>

