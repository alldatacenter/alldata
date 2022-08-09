
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

Update Host Component
=====
[Back to Resources](index.md#resources)

**Summary**

Update a host component for a given host and component.

    PUT api/v1/clusters/:name/hosts/:hostName/host_components/:hostComponentName

**Response**

<table>
  <tr>
    <th>HTTP CODE</th>
    <th>Description</th>
  </tr>
  <tr>
    <td>200</td>
    <td>OK</td>  
  </tr>
  <tr>
    <td>202</td>
    <td>Accepted</td>  
  </tr>
  <tr>
    <td>400</td>
    <td>Bad Request</td>  
  </tr>
  <tr>
    <td>401</td>
    <td>Unauthorized</td>  
  </tr>
  <tr>
    <td>403</td>
    <td>Forbidden</td>  
  </tr> 
  <tr>
    <td>500</td>
    <td>Internal Server Error</td>  
  </tr>
</table>



**Example 1**

Start the NAMENODE component by updating its state to 'STARTED'.


    PUT api/v1/clusters/c1/hosts/hostname/host_components/NAMENODE
    
    {
      "HostRoles":{
        "state":"STARTED"
      }
    }


    202 Accepted
    {
      "href" : "http://your.ambari.server:8080/api/v1/clusters/c1/requests/12",
      "Requests" : {
        "id" : 12,
        "status" : "InProgress"
      }
    }
    
**Example 2 **

Stop the NAMENODE component by updating its state to 'INSTALLED'.


    PUT api/v1/clusters/c1/hosts/hostname/host_components/NAMENODE
    
    {
      "HostRoles":{
        "state":"INSTALLED"
      }
    }


    202 Accepted
    {
      "href" : "http://your.ambari.server:8080/api/v1/clusters/c1/requests/13",
      "Requests" : {
        "id" : 13,
        "status" : "InProgress"
      }
    }
    
**Example 3 **

Put the NAMENODE component into 'MAINTENANCE' mode.


    PUT api/v1/clusters/c1/hosts/hostname/host_components/NAMENODE
    
    {
      "HostRoles":{
        "state":"MAINTENANCE"
      }
    }


    202 Accepted
    {
      "href" : "http://your.ambari.server:8080/api/v1/clusters/c1/requests/14",
      "Requests" : {
        "id" : 14,
        "status" : "InProgress"
      }
    }    