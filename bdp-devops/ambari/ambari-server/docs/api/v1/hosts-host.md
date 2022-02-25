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

View Host Information
=====

[Back to Resources](index.md#resources)

**Summary**

Returns information about a single host in the cluster identified by ":clusterName".

    GET /clusters/:clusterName/hosts/:hostName

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

Returns information about the host name "host1" in the cluster named "c1".

    200 OK
    {
    	"href" : "http://your.ambari.server/api/v1/clusters/c1/hosts/host1",
    	"metrics" : {
    		"process" : {
    			...    
    		},
      		"rpc" : {
        		...
      		},
      		"ugi" : {
      			...
      		}
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
    	"Hosts" : {
      		"cluster_name" : "c1",
      		"host_name" : "host1",
      		"host_state" : "HEALTHY",
      		"public_host_name" : "host1.yourDomain.com",
      		"cpu_count" : 1,
      		"rack_info" : "rack-name",
      		"os_arch" : "x86_64",
      		disk_info : [
      			{
      				"available" : "41497444",
        			"used" : "9584560",
        			"percent" : "19%",
        			"size" : "51606140",
        			"type" : "ext4",
       	 			"mountpoint" : "/"
      			}
      		],
      		"ip" : "10.0.2.15",
      		"os_type" : "rhel6",
      		"total_mem" : 2055208,
      		...        	      		
    	},
    	"host_components" : [
      		{
      			"href" : "http://your.ambari.server/api/v1/clusters/c1/hosts/host1/host_components/DATANODE",
      			"HostRoles" : {
        			"cluster_name" : "c1",
        			"component_name" : "DATANODE",
        			"host_name" : "host1"
        		}
      		},
      		...
       	]
    }


