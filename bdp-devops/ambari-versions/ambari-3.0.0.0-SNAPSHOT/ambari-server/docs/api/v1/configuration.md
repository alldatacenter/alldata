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

# Configuration
A configuration is a set of key/value pairs that is persisted on Ambari server.  A cluster has many configuration sets, each of which can be applied to a particular piece of functionality.  A configuration consists of a type, a tag, and properties that define them.  For example:
    
    {
      "type": "core-site",
      "tag": "version1",
      "properties": {
        "fs.default.name": "hdfs://h0:8020"
        // ...
      }
    }
    
The `type` represents the semantic meaning of the configuration, while the `tag` identifies the revision of that `type`.  In Ambari, the combination of `type` and `tag` is unique.  However, multiple configurations of the same `type` having different `tag`s can exist.  You cannot have two `core-site/version1`, nor is it supported to change the properties of an already defined property set.

## Property Sets
To list all the configurations defined in a cluster:

    GET /api/v1/clusters/c1/configurations
    {
      "items" : [
        {
          "tag" : "version1",
          "type" : "global",
          "Config" : {
            "cluster_name" : "c1"
          }
        },
        /* etc */
      ]
    }
To view the real set of key/value pairs, use the `type` and `tag` as request parameters:

    GET /api/v1/clusters/c1/configurations?type=global&tag=version1
    {
      "items" : [
      {
        "tag" : "version1",
        "type" : "global",
        "Config" : {
          "cluster_name" : "c1"
        },
        "properties" : {
          "hdfs_enable_shortcircuit_skipchecksum" : "false",
          "hive_lib" : "/usr/lib/hive/lib/",
          /* etc */
        }
      ]}
    }

To create a new configuration, the below call can be made. Creating a configuration is different than applying a configuration.
    
    POST /api/v1/clusters/c1/configurations
    {
      "tag" : "version2",
      "type" : "global",
      "Config" : {
        "cluster_name" : "c2"
      }
    }


## Desired Configuration
Desired configurations is the set of `tag`s that defines what you desire the cluster to have.  It 
is possible to save configurations, even if they are not applied.  You can define `tag`s having 
the same type, but tag must be unique and only one of them is active for the cluster at any 
time.

## Overrides
Overrides are enabled by the Config Group resource. (see [Config Groups](config-groups.md))
Configurations are used by individual components on a host. The design is to define and apply configuration at the cluster level, with optional overrides per host. The order of application is as follows:

For a given host:
1. Get all desired configurations defined for the cluster.
2. Find all config groups for the host.
3. Apply the config group overrides on top of the cluster configurations.

The optional config group overrides contain only the properties which need to be changed on a host from the cluster level configuration.
There must always be a cluster-level configuration of the same type.


### Cluster
There are two ways to apply a configuration to a cluster. 

One way is to create a configuration independently as described in *Property Sets* section above, 
and then apply it to the cluster:

    PUT /api/v1/clusters/c1
    {
      "Clusters": {
        "desired_config": {
          "type": "core-site",
          "tag": "version2"
        }
      }
    }

The other, more efficient way is to create and apply a configuration on a cluster, in one call:

    PUT /api/v1/clusters/c1
    {
      "Clusters": {
        "desired_config": {
          "type": "core-site",
          "tag": "version2",
          "properties": {
            "a": "b",
            /* etc */
          }
        }
      }
    }
    
The properties object is optional to avoid making two calls to create a configuration.  If you provide them, the backend will first make the config instance, then apply it.  Omit them, and it will apply the `type`/`tag` pair to the cluster as the default.

The list of all desired configs is reported in a GET call:
    
    GET /api/v1/clusters/c1
    {
      "Clusters" : {
        "cluster_name" : "c1",
         "cluster_id" : 2,
         "desired_configs" : {
           "global" : {
             "tag" : "version1"
           },
           "hdfs-site" : {
             "tag" : "version1"
           },
           "core-site": {
             "tag": "version1"
           },
            /* etc */
         }
       }
    }

To list the desired configs for a host:
    
    GET /api/v1/clusters/c1/hosts/h0
    {
      "Hosts" : {
        "cluster_name" : "c1",
        "host_name" : "h0",
        "desired_configs" : {
          "global" : {
            "overrides" : {
              "2" : "version10"
            },
            "default" : "version1"
          },
          /* etc */
        }
      }
    }

Notice overrides for a configuration type are listed with the key as the config group id and value is the tag identifying the configuration resource.

## Actual Configuration
Actual configuration represents the set of `tag`s that identify the cluster's current configuration.  When configurations are changed, they are saved into the backing database, even if the host has not yet received the change.  When a 
host receives the desired configuration changes AND applies them, it will respond with the applied tags. This is called the actual configuration.

The most common use-case for actual configuration is knowing when a service or component restart is required.  Consider the following query (response abbreviated for readability):

    GET /api/v1/clusters/c1/services?fields=components/host_components/HostRoles/actual_configs/*
    {
      "items" : [
        {
          "ServiceInfo" : {
            "cluster_name" : "c1",
            "service_name" : "OOZIE"
          },
          "components" : [
            {
              "ServiceComponentInfo" : {
                "cluster_name" : "c1",
                "component_name" : "OOZIE_SERVER",
                "service_name" : "OOZIE"
              },
              "host_components" : [
              {
                "HostRoles" : {
                  "cluster_name" : "c1",
                  "component_name" : "OOZIE_SERVER",
                  "host_name" : "h0",
                  "actual_configs" : {
                    "oozie-site" : {
                      "tag" : "version1"
                    }
                  }
                }
              }
            ]
          },
          /* etc */
         ]
        }
      ]
    }

The actual configurations are used at the host_component level to indicate which services are different from the desired configurations.  Any differences requires a restart of the component (or the entire service, if needed).  This is because components of the the same service may be on different hosts.
