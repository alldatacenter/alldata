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

# Configuration Group
A Configuration group or Config group is type of resource that supports grouping of configuration resources and host resources for a service.
Host is identified using the registered hostname of the host and the configuration resource is identified by the type and tag.

Example list of all config groups defined for a cluster:

    GET /api/v1/clusters/c1/config_groups
    {
      "href" : "http://your.ambari.server/api/v1/clusters/c1/config_groups",
      "items" : [
        {
          "href" : "http://your.ambari.server/api/v1/clusters/c1/config_groups/2",
          "ConfigGroup" : {
            "cluster_name" : "c1",
            "id" : 2
          }
        }
      ]
    }

### Intuition
Hosts grouped together based on certain similar characteristics will essentially share the same configuration for a Service and its components deployed on those hosts; consequently, different configuration for a different service.

## Management
Get call to view details of a particular config group identified by an :id

    GET /api/v1/clusters/c1/config_groups/:id
    {
      "href" : "http://your.ambari.server/api/v1/clusters/c1/config_groups/:id",
      "ConfigGroup" : {
        "cluster_name" : "c1",
        "description" : "Next Gen Slaves Group",
        "desired_configs" : [
          {
            "tag" : "nextgen1",
            "type" : "core-site",
            "href" : "http://your.ambari.server/api/v1/clusters/c1/configurations?type=core-site&tag=nextgen1"
          }
        ],
        "group_name" : "Test Datanode group",
        "hosts" : [
          {
            "host_name" : "host1",
            "href" : "http://your.ambari.server/api/v1/clusters/c1/hosts/host1"
          }
        ],
        "id" : :id,
        "tag" : "HDFS"
      }
    }

    200 OK

To create a new config group, it is mandatory to provide a config group name, a tag and the cluster name to which it belongs.
The tag as seen in this example is a name of the service. Two config groups with the same tag cannot be associated with the same host.

    POST /api/v1/clusters/c1/config_groups
    [
       {
          "ConfigGroup": {
             "cluster_name": "c1",
             "group_name": "hdfs-nextgenslaves",
             "tag": "HDFS",
             "description": "HDFS configs for rack added on May 19, 2010",
             "hosts": [
                {
                   "host_name": "host1"
                }
             ],
             "desired_configs": [
                {
                   "type": "core-site",
                   "tag": "nextgen1",
                   "properties": {
                      "key": "value"
                   }
                }
             ]
          }
       }
    ]

    201 Created

Notice that the desired_configs specifies the property set for the configuration resource. In this particular case, a new configuration will be created with the type and tag specified and associated with the resulting Config group if the configuration does not exist.
If a configuration with given type and tag exists, the property set would be ignored since a configuration is immutable.

An update on a Config group expects the entire resource definition to be sent with the PUT request.

    PUT /api/v1/clusters/c1/config_groups/2
    [
       {
          "ConfigGroup": {
             "cluster_name": "c1",
             "group_name": "hdfs-nextgenslaves",
             "tag": "HDFS",
             "description": "HDFS configs for rack added on May 19, 2010",
             "hosts": [
                {
                   "host_name": "host1",
                   "host_name": "host2",
                   "host_name": "host3"
                }
             ],
             "desired_configs": [
                {
                   "type": "core-site",
                   "tag": "nextgen1"
                },
                {
                   "type": "hdfs-site",
                   "tag": "nextgen1"
                }
             ]
          }
       }
    ]

    202 Accepted

A Config group identified by the :id can be deleted.

    DELETE /api/v1/clusters/c1/config_groups/:id

    200 OK