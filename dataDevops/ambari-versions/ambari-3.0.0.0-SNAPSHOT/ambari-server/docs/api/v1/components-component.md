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

View Component Information
=====

[Back to Resources](index.md#resources)

**Summary**

Return the component identified by ":componentName" for for the service identifired by ":serviceName".

    GET /clusters/:name/services/:serviceName/components/:componentName

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


    GET /clusters/c1/services/HDFS/components/DATANODE

    200 OK
    {
    	"href" : "http://your.ambari.server/api/v1/clusters/c1/services/HDFS/components/DATANODE",
    	"metrics" : {
    		"process" : {
    			...    
    		},
      		"rpc" : {
        		...
      		},
      		"ugi" : {
      			...
      		},
      		"dfs" : {
        		"datanode" : {
          		...
        		}
      		},
      		"disk" : {
        		...
      		},
      		"cpu" : {
        		...
      		},
      		"rpcdetailed" : {
      			...
      		},
      		"jvm" : {
        		...
      		},
      		"load" : {
        		...
      		},
      		"memory" : {
        		...
      		},
      		"network" : {
        		...
      		},
    	},
    	"ServiceComponentInfo" : {
      		"cluster_name" : "c1",
      		"component_name" : "DATANODE",
      		"service_name" : "HDFS"
      		"state" : "STARTED"
    	},
    	"host_components" : [
      		{
      			"href" : "http://your.ambari.server/api/v1/clusters/c1/hosts/host1/host_components/DATANODE",
      			"HostRoles" : {
        			"cluster_name" : "c1",
        			"component_name" : "DATANODE",
        			"host_name" : "host1"
        		}
      		}
       	]
    }
