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

View Service Components
=====

[Back to Resources](index.md#resources)

**Summary**

Returns a collection of all components for the service identified by ":serviceName" and the cluster identified by ":clusterName".

    GET /clusters/:clusterName/services/:serviceName/components

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

Returns a collection of all components for the HDFS service of the cluster "c1".


    GET /clusters/c1/services/HDFS/components

    200 OK
    {
    	"href" : "http://your.ambari.server/api/v1/clusters/c1/services/HDFS/components",
    	"items" : [
      		{
      			"href" : "http://your.ambari.server/api/v1/clusters/c1/services/HDFS/components/DATANODE",
      			"ServiceComponentInfo" : {
        			"cluster_name" : "c1",
        			"component_name" : "DATANODE",
        			"service_name" : "HDFS"
        		}
      		},
      		{
      			"href" : "http://your.ambari.server/api/v1/clusters/c1/services/HDFS/components/SECONDARY_NAMENODE",
      			"ServiceComponentInfo" : {
        			"cluster_name" : "c1",
        			"component_name" : "SECONDARY_NAMENODE",
        			"service_name" : "HDFS"
        		}
      		},
      		{
      			"href" : "http://your.ambari.server/api/v1/clusters/c1/services/HDFS/components/NAMENODE",
      			"ServiceComponentInfo" : {
        			"cluster_name" : "c1",
        			"component_name" : "NAMENODE",
        			"service_name" : "HDFS"
        		}
      		},
      		{
      			"href" : "http://your.ambari.server/api/v1/clusters/c1/services/HDFS/components/HDFS_CLIENT",
      			"ServiceComponentInfo" : {
        			"cluster_name" : "c1",
        			"component_name" : "HDFS_CLIENT",
        			"service_name" : "HDFS"
        		}
      		}
      	]
    }
