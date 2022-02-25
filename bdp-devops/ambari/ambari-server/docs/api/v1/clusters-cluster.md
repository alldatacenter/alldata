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

View Cluster Information
=====

[Back to Resources](index.md#resources)

**Summary**

Returns information for the specified cluster identified by ":name"

    GET /clusters/:name

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

Get information for the cluster "cluster001".

    GET /clusters/cluster001
    
    200 OK
    {
	"href" : "http://your.ambari.server/api/v1/clusters/cluster001",
	"Clusters" : {
		"cluster_id" : 9,
		"cluster_name" : "cluster001",
		"health_report" : {
			"Host/stale_config" : 1,
			"Host/maintenance_state" : 0,
			"Host/host_state/HEALTHY" : 3,
			"Host/host_state/UNHEALTHY" : 0,
			"Host/host_state/HEARTBEAT_LOST" : 0,
			"Host/host_state/INIT" : 0,
			"Host/host_status/HEALTHY" : 3,
			"Host/host_status/UNHEALTHY" : 0,
			"Host/host_status/UNKNOWN" : 0,
			"Host/host_status/ALERT" : 0
		},
		"provisioning_state" : "INIT",
		"security_type" : "NONE",
		"total_hosts" : 3,
		"version" : "HDP-2.0",
		"desired_configs" : {
			"capacity-scheduler" : {
				"user" : "admin",
				"tag" : "version1408514705943"
			},
			"core-site" : {
				"user" : "admin",
				"tag" : "version1409806913314"
			},
			"global" : {
				"user" : "admin",
				"tag" : "version1409806913314"
			},
			"hdfs-log4j" : {
				"user" : "admin",
				"tag" : "version1"
			},
			"hdfs-site" : {
				"user" : "admin",
				"tag" : "version1407908591996"
			},
			"mapred-site" : {
				"user" : "admin",
				"tag" : "version1408514705943"
			},
			"mapreduce2-log4j" : {
				"user" : "admin",
				"tag" : "version1408514705943"
			},
			"yarn-log4j" : {
				"user" : "admin",
				"tag" : "version1408514705943"
			},
			"yarn-site" : {
				"user" : "admin",
				"tag" : "version1408514705943"
			},
			"zoo.cfg" : {
				"user" : "admin",
				"tag" : "version1"
			},
			"zookeeper-log4j" : {
				"user" : "admin",
				"tag" : "version1"
			}
		}
	},
    "alerts_summary" : {
      "CRITICAL" : 0,
      "MAINTENANCE" : 0,
      "OK" : 65,
      "UNKNOWN" : 0,
      "WARNING" : 0
    },
    "alerts_summary_hosts" : {
      "CRITICAL" : 0,
      "OK" : 3,
      "UNKNOWN" : 0,
      "WARNING" : 0
    },
	"requests" : [
		{
			"href" : "http://your.ambari.server/api/v1/clusters/cluster001/requests/304",
			"Requests" : {
			"cluster_name" : "cluster001",
			"id" : 304
			}
		},
		{
			"href" : "http://your.ambari.server/api/v1/clusters/cluster001/requests/305",
			"Requests" : {
			"cluster_name" : "cluster001",
			"id" : 305
			}
		}
		],
	"services" : [
	{
		"href" : "http://your.ambari.server/api/v1/clusters/cluster001/services/GANGLIA",
		"ServiceInfo" : {
		"cluster_name" : "cluster001",
		"service_name" : "GANGLIA"
		}
	},
	{
		"href" : "http://your.ambari.server/api/v1/clusters/cluster001/services/HDFS",
		"ServiceInfo" : {
		"cluster_name" : "cluster001",
		"service_name" : "HDFS"
		}
	},
	{
		"href" : "http://your.ambari.server/api/v1/clusters/cluster001/services/MAPREDUCE2",
		"ServiceInfo" : {
		"cluster_name" : "cluster001",
		"service_name" : "MAPREDUCE2"
		}
	},
	{
		"href" : "http://your.ambari.server/api/v1/clusters/cluster001/services/ZOOKEEPER",
		"ServiceInfo" : {
		"cluster_name" : "cluster001",
		"service_name" : "ZOOKEEPER"
		}
	}
    	],
	"config_groups" : [
	{
		"href" : "http://your.ambari.server/api/v1/clusters/cluster001/config_groups/2",
		"ConfigGroup" : {
		 "cluster_name" : "cluster001",
		  "id" : 2
		}
	}
	],
	"workflows" : [ ],
	"hosts" : [
	{
		"href" : "http://your.ambari.server/api/v1/clusters/cluster001/hosts/host1.domain.com",
		"Hosts" : {
		  "cluster_name" : "cluster001",
		  "host_name" : "host1.domain.com"
		}
	},
	{
		"href" : "http://your.ambari.server/api/v1/clusters/cluster001/hosts/host2.domain.com",
		"Hosts" : {
		  "cluster_name" : "cluster001",
		  "host_name" : "host2.domain.com"
		}
	},
	{
		"href" : "http://your.ambari.server/api/v1/clusters/cluster001/hosts/host3.domain.com",
		"Hosts" : {
		  "cluster_name" : "cluster001",
		  "host_name" : "host3.domain.com"
		}
	}
	],
	"configurations" : [
	{
		"href" : "http://your.ambari.server/api/v1/clusters/cluster001/configurations?type=core-site&tag=version1",
		"tag" : "version1",
		"type" : "core-site",
		"Config" : {
		  "cluster_name" : "cluster001"
		}
	},
	{
		"href" : "http://your.ambari.server/api/v1/clusters/cluster001/configurations?type=global&tag=version1",
		"tag" : "version1",
		"type" : "global",
		"Config" : {
		  "cluster_name" : "cluster001"
		}
	},
	{
		"href" : "http://your.ambari.server/api/v1/clusters/cluster001/configurations?type=hdfs-site&tag=version1",
		"tag" : "version1",
		"type" : "hdfs-site",
		"Config" : {
		  "cluster_name" : "cluster001"
		}
	},
	{
		"href" : "http://your.ambari.server/api/v1/clusters/cluster001/configurations?type=zoo.cfg&tag=version1",
		"tag" : "version1",
		"type" : "zoo.cfg",
		"Config" : {
		  "cluster_name" : "cluster001"
		}
	},
	]
    }
