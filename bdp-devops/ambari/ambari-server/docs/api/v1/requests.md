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

List Requests
=====

[Back to Resources](index.md#resources)

**Summary**

Returns a collection of all requests for the cluster identified by ":clusterName".

    GET /clusters/:clusterName/requests

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
    <td>404</td>
    <td>Not Found</td>  
  </tr>
  <tr>
    <td>500</td>
    <td>Internal Server Error</td>  
  </tr>
</table>



**Example**

Get the collection of the requests for the cluster named "c1".

    GET /clusters/c1/requests?fields=Requests/request_context
    
    200 OK
    {
    	"href" : "http://your.ambari.server/api/v1/clusters/c1/requests?fields=Requests/request_context",
        "items" : [
          {
            "href" : "http://your.ambari.server/api/v1/clusters/c1/requests/1",
            "Requests" : {
              "cluster_name" : "c1",
              "id" : 1,
              "request_context" : "Install Services"
            }
          },
          {
            "href" : "http://your.ambari.server/api/v1/clusters/c1/requests/2",
            "Requests" : {
              "cluster_name" : "c1",
              "id" : 2,
              "request_context" : "Start Services"
            }
          }
        ]	
    }  	


Post on a request resource allows the user to execute Custom commands or custom actions on resources identified by a resource filter.
Custom Command - A command script defined for Services in the Ambari Services stack and identified by a command name. Example: Restart, Service checks / Decommission slave component.
Custom Action - A command script defined in the stack under custom_actions and identified by the action definition specified in /custom_action_definitions/system_action_definitions.xml.

    POST http://your.ambari.server/api/v1/clusters/c1/requests

    202 Created
    {
       "RequestInfo": {
          "context": "Restart DataNode",
          "command": "RESTART"
       },
       "Requests/resource_filters": [
          {
             "service_name": "HDFS",
             "component_name": "NAMENODE",
             "hosts": "host1,host2,host3"
          }
       ]
    }

