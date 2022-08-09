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

Ambari API Response Schemas
=========


- [GET clusters](#get-clusters)
- [GET cluster](#get-cluster)
- [GET service](#get-service)
- [GET services](#get-services)
- [GET components](#get-components)
- [GET component](#get-component)
- [GET hosts](#get-hosts)
- [GET host](#get-host)
- [GET host_components](#get-host_components)
- [GET host_component](#get-host_component)
- [GET configurations](#get-configurations)
- [GET configuration](#get-configuration)
- [GET request](#get-request)
- [GET task](#get-task)
- [POST/PUT/DELETE resource](#postputdelete-resource)


GET clusters
----

**Example**


    GET api/v1/clusters/


    200 OK
    {
      "href" : "http://your.ambari.server/api/v1/clusters/",
      "items" : [
        {
          "href" : "http://your.ambari.server/api/v1/clusters/c1",
          "Clusters" : {
            "cluster_name" : "c1",
            "version" : "HDP-1.3.0"
          }
        }
      ]
    }

**Schema**

    {
      "type":"object",
      "$schema": "http://json-schema.org/draft-03/schema",
      "title": "Clusters",
      "required":true,
      "properties":{
        "href": {
          "type":"string",
          "description": "This clusters API href.",
          "required":true
        },
        "items": {
          "type":"array",
          "title": "Cluster set",
          "required":true,
          "items":
          {
            "type":"object",
            "title": "Cluster",
            "description": "A Hadoop cluster.",
            "required":false,
            "properties":{
              "Clusters": {
                "type":"object",
                "title": "ClusterInfo",
                "description": "Cluster information.",
                "required":true,
                "properties":{
                  "cluster_name": {
                    "type":"string",
                    "title": "ClusterName",
                    "description": "The cluster name.",
                    "required":true
                  },
                  "version": {
                    "type":"string",
                    "title": "Version",
                    "description": "The stack version.",
                    "required":true
                  }
                }
              },
              "href": {
                "type":"string",
                "description": "The cluster API href.",
                "required":true
              }
            }
          }
        }
      }
    }



GET cluster
----

**Example**


    GET api/v1/clusters/c1

    200 OK
    {
      "href" : "http://your.ambari.server/api/v1/clusters/c1",
      "Clusters" : {
        "cluster_name" : "c1",
        "cluster_id" : 2,
        "version" : "HDP-1.3.0",
        "desired_configs" : {
          "mapred-site" : {
            "user" : "admin",
            "tag" : "version1"
          },
          "hdfs-site" : {
            "user" : "admin",
            "tag" : "version1"
          },
          "global" : {
            "user" : "admin",
            "tag" : "version1369851987025"
          },
          "core-site" : {
            "user" : "admin",
            "tag" : "version1369851987025"
          }
        }
      },
      "requests" : [
        {
          "href" : "http://your.ambari.server/api/v1/clusters/c1/requests/1",
          "Requests" : {
            "id" : 1,
            "cluster_name" : "c1"
          }
        },
        {
          "href" : "http://your.ambari.server/api/v1/clusters/c1/requests/4",
          "Requests" : {
            "id" : 4,
            "cluster_name" : "c1"
          }
        },
        {
          "href" : "http://your.ambari.server/api/v1/clusters/c1/requests/5",
          "Requests" : {
            "id" : 5,
            "cluster_name" : "c1"
          }
        },
        {
          "href" : "http://your.ambari.server/api/v1/clusters/c1/requests/6",
          "Requests" : {
            "id" : 6,
            "cluster_name" : "c1"
          }
        },
        {
          "href" : "http://your.ambari.server/api/v1/clusters/c1/requests/3",
          "Requests" : {
            "id" : 3,
            "cluster_name" : "c1"
          }
        },
        {
          "href" : "http://your.ambari.server/api/v1/clusters/c1/requests/2",
          "Requests" : {
            "id" : 2,
            "cluster_name" : "c1"
          }
        }
      ],
      "services" : [
        {
          "href" : "http://your.ambari.server/api/v1/clusters/c1/services/GANGLIA",
          "ServiceInfo" : {
            "cluster_name" : "c1",
            "service_name" : "GANGLIA"
          }
        },
        {
          "href" : "http://your.ambari.server/api/v1/clusters/c1/services/HDFS",
          "ServiceInfo" : {
            "cluster_name" : "c1",
            "service_name" : "HDFS"
          }
        },
        {
          "href" : "http://your.ambari.server/api/v1/clusters/c1/services/MAPREDUCE",
          "ServiceInfo" : {
            "cluster_name" : "c1",
            "service_name" : "MAPREDUCE"
          }
        },
        {
          "href" : "http://your.ambari.server/api/v1/clusters/c1/services/NAGIOS",
          "ServiceInfo" : {
            "cluster_name" : "c1",
            "service_name" : "NAGIOS"
          }
        }
      ],
      "workflows" : [
        {
          "href" : "http://your.ambari.server/api/v1/clusters/c1/workflows/mr_201305291642_0001",
          "Workflow" : {
            "cluster_name" : "c1",
            "workflow_id" : "mr_201305291642_0001"
          }
        }
      ],
      "hosts" : [
        {
          "href" : "http://your.ambari.server/api/v1/clusters/c1/hosts/ip-10-83-74-200.ec2.internal",
          "Hosts" : {
            "cluster_name" : "c1",
            "host_name" : "ip-10-83-74-200.ec2.internal"
          }
        },
        {
          "href" : "http://your.ambari.server/api/v1/clusters/c1/hosts/ip-10-39-130-141.ec2.internal",
          "Hosts" : {
            "cluster_name" : "c1",
            "host_name" : "ip-10-39-130-141.ec2.internal"
          }
        }
      ],
      "configurations" : [
        {
          "href" : "http://your.ambari.server/api/v1/clusters/c1/configurations?type=core-site&tag=version1369851987025",
          "tag" : "version1369851987025",
          "type" : "core-site",
          "Config" : {
            "cluster_name" : "c1"
          }
        },
        {
          "href" : "http://your.ambari.server/api/v1/clusters/c1/configurations?type=mapred-site&tag=version1",
          "tag" : "version1",
          "type" : "mapred-site",
          "Config" : {
            "cluster_name" : "c1"
          }
        },
        {
          "href" : "http://your.ambari.server/api/v1/clusters/c1/configurations?type=hdfs-site&tag=version1",
          "tag" : "version1",
          "type" : "hdfs-site",
          "Config" : {
            "cluster_name" : "c1"
          }
        },
        {
          "href" : "http://your.ambari.server/api/v1/clusters/c1/configurations?type=core-site&tag=version1",
          "tag" : "version1",
          "type" : "core-site",
          "Config" : {
            "cluster_name" : "c1"
          }
        },
        {
          "href" : "http://your.ambari.server/api/v1/clusters/c1/configurations?type=global&tag=version1369851987025",
          "tag" : "version1369851987025",
          "type" : "global",
          "Config" : {
            "cluster_name" : "c1"
          }
        },
        {
          "href" : "http://your.ambari.server/api/v1/clusters/c1/configurations?type=global&tag=version1",
          "tag" : "version1",
          "type" : "global",
          "Config" : {
            "cluster_name" : "c1"
          }
        }
      ]
    }


**Schema**

    {
      "type":"object",
      "$schema":"http://json-schema.org/draft-03/schema",
      "title":"Cluster",
      "required":true,
      "properties":{
        "Clusters":{
          "type":"object",
          "title":"ClusterInfo",
          "description":"Cluster information",
          "required":true,
          "properties":{
            "cluster_id":{
              "type":"number",
              "title":"ClusterId",
              "description":"The unique cluster ID.",
              "required":false
            },
            "cluster_name":{
              "type":"string",
              "title":"ClusterName",
              "description":"The cluster name.",
              "required":true
            },
            "desired_configs":{
              "type":"object",
              "required":false
            },
            "version":{
              "type":"string",
              "title":"Version",
              "description":"The stack version.",
              "required":true
            }
          }
        },
        "configurations":{
          "type":"array",
          "title":"Configuration set",
          "required":false,
          "items":{
            "type":"object",
            "title":"Configuration",
            "required":false,
            "properties":{
              "Config":{
                "type":"object",
                "title":"ConfigInfo",
                "description":"Configuration information.",
                "required":true,
                "properties":{
                  "cluster_name":{
                    "type":"string",
                    "title":"ClusterName",
                    "description":"The associated cluster name.",
                    "required":true
                  }
                }
              },
              "href":{
                "type":"string",
                "description":"The configuration API href.",
                "required":true
              },
              "tag":{
                "type":"string",
                "title":"Tag",
                "required":true
              },
              "type":{
                "type":"string",
                "title":"Type",
                "required":true
              }
            }
          }
        },
        "hosts":{
          "type":"array",
          "title":"Host set",
          "required":true,
          "items":{
            "type":"object",
            "title":"Host",
            "required":true,
            "properties":{
              "Hosts":{
                "type":"object",
                "title":"HostInfo",
                "description":"Host information.",
                "required":true,
                "properties":{
                  "cluster_name":{
                    "type":"string",
                    "title":"ClusterName",
                    "description":"The associated cluster name.",
                    "required":true
                  },
                  "host_name":{
                    "type":"string",
                    "title":"HostName",
                    "description":"The associated host name.",
                    "required":true
                  }
                }
              },
              "href":{
                "type":"string",
                "description":"This host API href.",
                "required":true
              }
            }
          }
        },
        "href":{
          "type":"string",
          "description":"This cluster API href.",
          "required":true
        },
        "requests":{
          "type":"array",
          "title":"Request set",
          "required":false,
          "items":{
            "type":"object",
            "title":"Request",
            "required":true,
            "properties":{
              "Requests":{
                "type":"object",
                "title":"RequestInfo",
                "description":"Request information",
                "required":true,
                "properties":{
                  "cluster_name":{
                    "type":"string",
                    "title":"ClusterName",
                    "description":"The associated cluster name.",
                    "required":true
                  },
                  "id":{
                    "type":"number",
                    "title":"RequestId",
                    "description":"The request ID.",
                    "required":true
                  }
                }
              },
              "href":{
                "type":"string",
                "description":"This request API href.",
                "required":true
              }
            }
          }
        },
        "services":{
          "type":"array",
          "title":"Service set",
          "required":true,
          "items":{
            "type":"object",
            "title":"Service",
            "required":false,
            "properties":{
              "ServiceInfo":{
                "type":"object",
                "title":"ServiceInfo",
                "description":"Service information.",
                "required":true,
                "properties":{
                  "cluster_name":{
                    "type":"string",
                    "title":"ClusterName",
                    "description":"The associated cluster name.",
                    "required":true
                  },
                  "service_name":{
                    "type":"string",
                    "title":"ServiceName",
                    "description":"The service name.",
                    "required":true
                  }
                }
              },
              "href":{
                "type":"string",
                "description":"This service API href.",
                "required":true
              }
            }
          }
        },
        "workflows":{
          "type":"array",
          "title":"Workflow set",
          "required":false,
          "items":{
            "type":"object",
            "title":"Workflow",
            "required":false,
            "properties":{
              "Workflow":{
                "type":"object",
                "title":"WorkflowInfo",
                "description":"Workflow information.",
                "required":true,
                "properties":{
                  "cluster_name":{
                    "type":"string",
                    "title":"ClusterName",
                    "description":"The associated cluster name.",
                    "required":true
                  },
                  "workflow_id":{
                    "type":"string",
                    "title":"WorkflowId",
                    "description":"The unique workflow id.",
                    "required":true
                  }
                }
              },
              "href":{
                "type":"string",
                "description":"This workflow API href.",
                "required":true
              }
            }
          }
        }
      }
    }



GET services
----

**Example**


    GET api/v1/clusters/c1/services

    200 OK
    {
      "href" : "http://your.ambari.server/api/v1/clusters/c1/services",
      "items" : [
        {
          "href" : "http://your.ambari.server/api/v1/clusters/c1/services/MAPREDUCE",
          "ServiceInfo" : {
            "cluster_name" : "c1",
            "service_name" : "MAPREDUCE"
          }
        },
        {
          "href" : "http://your.ambari.server/api/v1/clusters/c1/services/NAGIOS",
          "ServiceInfo" : {
            "cluster_name" : "c1",
            "service_name" : "NAGIOS"
          }
        },
        {
          "href" : "http://your.ambari.server/api/v1/clusters/c1/services/HDFS",
          "ServiceInfo" : {
            "cluster_name" : "c1",
            "service_name" : "HDFS"
          }
        },
        {
          "href" : "http://your.ambari.server/api/v1/clusters/c1/services/GANGLIA",
          "ServiceInfo" : {
            "cluster_name" : "c1",
            "service_name" : "GANGLIA"
          }
        }
      ]
    }


**Schema**

    {
      "type":"object",
      "$schema":"http://json-schema.org/draft-03/schema",
      "title":"Services",
      "required":true,
      "properties":{
        "href":{
          "type":"string",
          "title":"href",
          "description":"This services API href.",
          "required":true
        },
        "items":{
          "type":"array",
          "title":"Service set",
          "required":true,
          "items":{
            "type":"object",
            "title":"Service",
            "description":"A Hadoop service.",
            "required":false,
            "properties":{
              "ServiceInfo":{
                "type":"object",
                "title":"Service information.",
                "name":"ServiceInfo",
                "required":true,
                "properties":{
                  "cluster_name":{
                    "type":"string",
                    "title":"ClusterName",
                    "description":"The associated cluster name.",
                    "required":true
                  },
                  "service_name":{
                    "type":"string",
                    "title":"ServiceName",
                    "description":"The service name.",
                    "required":true
                  }
                }
              },
              "href":{
                "type":"string",
                "description":"This service API href.",
                "required":true
              }
            }
          }
        }
      }
    }


GET service
----

**Example**


    GET api/v1/clusters/c1/services/MAPREDUCE

    200 OK
    {
      "href" : "http://your.ambari.server/api/v1/clusters/c1/services/MAPREDUCE",
      "ServiceInfo" : {
        "cluster_name" : "c1",
        "state" : "STARTED",
        "service_name" : "MAPREDUCE",
        "desired_configs" : { }
      },
      "components" : [
        {
          "href" : "http://your.ambari.server/api/v1/clusters/c1/services/MAPREDUCE/components/MAPREDUCE_CLIENT",
          "ServiceComponentInfo" : {
            "cluster_name" : "c1",
            "component_name" : "MAPREDUCE_CLIENT",
            "service_name" : "MAPREDUCE"
          }
        },
        {
          "href" : "http://your.ambari.server/api/v1/clusters/c1/services/MAPREDUCE/components/TASKTRACKER",
          "ServiceComponentInfo" : {
            "cluster_name" : "c1",
            "component_name" : "TASKTRACKER",
            "service_name" : "MAPREDUCE"
          }
        },
        {
          "href" : "http://your.ambari.server/api/v1/clusters/c1/services/MAPREDUCE/components/JOBTRACKER",
          "ServiceComponentInfo" : {
            "cluster_name" : "c1",
            "component_name" : "JOBTRACKER",
            "service_name" : "MAPREDUCE"
          }
        }
      ]
    }

**Schema**

    {
      "type":"object",
      "$schema": "http://json-schema.org/draft-03/schema",
      "title": "Service",
      "required":true,
      "properties":{
        "ServiceInfo": {
          "type":"object",
          "title": "Service information",
          "required":true,
          "properties":{
            "cluster_name": {
              "type":"string",
              "title": "ClusterName",
              "description": "The associated cluster name.",
              "required":true
            },
            "desired_configs":{
              "type":"object",
              "required":false
            },
            "service_name": {
              "type":"string",
              "title": "ServiceName",
              "description": "The name of the service.",
              "required":true
            },
            "state": {
              "type":"string",
              "title": "State",
              "description": "The state of the service.",
              "required":true
            }
          }
        },
        "components": {
          "type":"array",
          "title": "Component set",
          "description": "The service components.",
          "required":true,
          "items":
          {
            "type":"object",
            "title": "ServiceComponent",
            "required":false,
            "properties":{
              "ServiceComponentInfo": {
                "type":"object",
                "title": "ServiceComponentInfo",
                "description": "The component information.",
                "required":true,
                "properties":{
                  "cluster_name": {
                    "type":"string",
                    "title": "ClusterName",
                    "description": "The associated cluster name.",
                    "required":true
                  },
                  "component_name": {
                    "type":"string",
                    "title": "ComponentName",
                    "description": "The component name.",
                    "required":true
                  },
                  "service_name": {
                    "type":"string",
                    "title": "ServiceName",
                    "description": "The associated service name.",
                    "required":true
                  }
                }
              },
              "href": {
                "type":"string",
                "description": "The component API href.",
                "required":true
              }
            }
          }
        },
        "href": {
          "type":"string",
          "description": "This service API href.",
          "required":true
        }
      }
    }


GET components
----

**Example**


    GET api/v1/clusters/c1/services/HDFS/components

    200 OK
    {
      "href" : "http://your.ambari.server/api/v1/clusters/c1/services/HDFS/components",
      "items" : [
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
          "href" : "http://your.ambari.server/api/v1/clusters/c1/services/HDFS/components/DATANODE",
          "ServiceComponentInfo" : {
            "cluster_name" : "c1",
            "component_name" : "DATANODE",
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

**Schema**

    {
      "type":"object",
      "$schema": "http://json-schema.org/draft-03/schema",
      "title": "Components",
      "required":true,
      "properties":{
        "href": {
          "type":"string",
          "description": "This components API href.",
          "required":true
        },
        "items": {
          "type":"array",
          "title": "Component set",
          "required":true,
          "items":
          {
            "type":"object",
            "title": "Component",
            "required":false,
            "properties":{
              "ServiceComponentInfo": {
                "type":"object",
                "title": "ServiceComponentInfo",
                "description": "Service component information.",
                "required":true,
                "properties":{
                  "cluster_name": {
                    "type":"string",
                    "title": "ClusterName",
                    "description": "The associated cluster name.",
                    "required":true
                  },
                  "component_name": {
                    "type":"string",
                    "title": "Component name",
                    "description": "The component name.",
                    "required":true
                  },
                  "service_name": {
                    "type":"string",
                    "title": "ServiceName",
                    "description": "The associated service name.",
                    "required":true
                  }
                }
              },
              "href": {
                "type":"string",
                "description": "This component API href.",
                "required":true
              }
            }
          }
        }
      }
    }


GET component
----

**Example**


    GET api/v1/clusters/c1/services/HDFS/components/NAMENODE

    200 OK
    {
      "href" : "http://your.ambari.server/api/v1/clusters/c1/services/HDFS/components/NAMENODE",
      "ServiceComponentInfo" : {
        "NonHeapMemoryUsed" : 23582424,
        "PercentRemaining" : 94.873405,
        "CapacityUsed" : 184320,
        "state" : "STARTED",
        "HeapMemoryUsed" : 364351400,
        "service_name" : "HDFS",
        "HeapMemoryMax" : 1006632960,
        "UpgradeFinalized" : true,
        "DecomNodes" : "{}",
        "Safemode" : "",
        "CapacityRemaining" : 842208030720,
        "StartTime" : 1369852360564,
        "Version" : "1.2.0.1.3.0.0-107, rd4625cb994e0143f5f4b538f0f2f4a41ad6464a2",
        "BlocksTotal" : 7,
        "LiveNodes" : "{\"ip-10-39-130-141.ec2.internal\":{\"usedSpace\":184320,\"lastContact\":1}}",
        "component_name" : "NAMENODE",
        "PercentUsed" : 2.0763358E-5,
        "TotalFiles" : 23,
        "NonDfsUsedSpace" : 45509476352,
        "MissingBlocks" : 0,
        "cluster_name" : "c1",
        "NonHeapMemoryMax" : 136314880,
        "UnderReplicatedBlocks" : 7,
        "CapacityTotal" : 887717691392,
        "CorruptBlocks" : 0,
        "DeadNodes" : "{}",
        "desired_configs" : { }
      },
      "metrics" : {
        "boottime" : 1.369844638E9,
        "process" : {
          "proc_total" : 336.0,
          "proc_run" : 0.0
        },
        "rpc" : {
          "rpcAuthorizationSuccesses" : 31,
          "rpcAuthorizationFailures" : 0,
          "SentBytes" : 542666,
          "ReceivedBytes" : 1169896,
          "NumOpenConnections" : 0,
          "callQueueLen" : 0,
          "rpcAuthenticationSuccesses" : 0,
          "RpcQueueTime_num_ops" : 3789,
          "RpcProcessingTime_num_ops" : 3789,
          "RpcProcessingTime_avg_time" : 0.0,
          "rpcAuthenticationFailures" : 0,
          "RpcQueueTime_avg_time" : 0.33333333333333337
        },
        "dfs" : {
          "namenode" : {
            "Threads" : 122,
            "PercentRemaining" : 94.873405,
            "JournalTransactionsBatchedInSync" : 0.0,
            "CreateFileOps" : 2,
            "GetListingOps" : 17,
            "UpgradeFinalized" : true,
            "Transactions_num_ops" : 15,
            "Free" : 842208030720,
            "GetBlockLocations" : 0,
            "NameDirStatuses" : "{\"failed\":{},\"active\":{\"/grid/1/hadoop/hdfs/namenode\":\"IMAGE_AND_EDITS\",\"/grid/0/hadoop/hdfs/namenode\":\"IMAGE_AND_EDITS\"}}",
            "DecomNodes" : "{}",
            "blockReport_num_ops" : 2,
            "Safemode" : "",
            "SafemodeTime" : 65061.0,
            "FilesInGetListingOps" : 32,
            "Transactions_avg_time" : 0.0,
            "TotalBlocks" : 7,
            "DeleteFileOps" : 0.0,
            "FilesCreated" : 3,
            "Version" : "1.2.0.1.3.0.0-107, rd4625cb994e0143f5f4b538f0f2f4a41ad6464a2",
            "AddBlockOps" : 2,
            "fsImageLoadTime" : 1141.0,
            "FilesRenamed" : 0.0,
            "LiveNodes" : "{\"ip-10-39-130-141.ec2.internal\":{\"usedSpace\":184320,\"lastContact\":1}}",
            "TotalFiles" : 23,
            "PercentUsed" : 2.0763358E-5,
            "FileInfoOps" : 40,
            "NonDfsUsedSpace" : 45509476352,
            "Syncs_avg_time" : 3.0,
            "HostName" : "ip-10-39-130-141.ec2.internal",
            "Syncs_num_ops" : 11,
            "Used" : 184320,
            "FilesDeleted" : 0.0,
            "FilesAppended" : 0.0,
            "blockReport_avg_time" : 0.0,
            "Total" : 887717691392,
            "DeadNodes" : "{}"
          },
          "FSNamesystem" : {
            "BlocksTotal" : 7,
            "ScheduledReplicationBlocks" : 0,
            "CapacityTotalGB" : 827,
            "CapacityUsedGB" : 0,
            "CapacityUsed" : 184320,
            "ExcessBlocks" : 0,
            "MissingBlocks" : 0,
            "PendingReplicationBlocks" : 0,
            "FilesTotal" : 23,
            "CapacityRemainingGB" : 784,
            "CapacityRemaining" : 842208030720,
            "UnderReplicatedBlocks" : 7,
            "TotalLoad" : 1,
            "CapacityTotal" : 887717691392,
            "PendingDeletionBlocks" : 0,
            "CorruptBlocks" : 0,
            "BlockCapacity" : 2097152
          }
        },
        "ugi" : {
          "loginSuccess_num_ops" : 0,
          "loginFailure_num_ops" : 0,
          "loginSuccess_avg_time" : 0.0,
          "loginFailure_avg_time" : 0.0
        },
        "disk" : {
          "disk_total" : 896.17,
          "disk_free" : 847.705,
          "part_max_used" : 35.0
        },
        "cpu" : {
          "cpu_speed" : 2266.0,
          "cpu_num" : 2.0,
          "cpu_wio" : 0.103611111111,
          "cpu_idle" : 98.8122222222,
          "cpu_nice" : 0.0,
          "cpu_aidle" : 0.0,
          "cpu_system" : 0.704166666667,
          "cpu_user" : 0.404722222222
        },
        "rpcdetailed" : {
          "addBlock_avg_time" : 1.0,
          "versionRequest_num_ops" : 0.0,
          "register_num_ops" : 0.0,
          "getListing_num_ops" : 17,
          "sendHeartbeat_num_ops" : 3524,
          "blocksBeingWrittenReport_avg_time" : 1.0,
          "rename_num_ops" : 0.0,
          "create_avg_time" : 6.0,
          "mkdirs_avg_time" : 16.0,
          "delete_num_ops" : 0.0,
          "create_num_ops" : 2,
          "mkdirs_num_ops" : 0.0,
          "delete_avg_time" : 18.0,
          "addBlock_num_ops" : 2,
          "getFileInfo_avg_time" : 1.0,
          "rename_avg_time" : 2.0,
          "getProtocolVersion_avg_time" : 0.0,
          "getListing_avg_time" : 1.0,
          "blockReceived_avg_time" : 0.0,
          "getFileInfo_num_ops" : 40,
          "register_avg_time" : 4.0,
          "setPermission_num_ops" : 0.0,
          "sendHeartbeat_avg_time" : 0.0,
          "complete_avg_time" : 2.0,
          "versionRequest_avg_time" : 1.0,
          "complete_num_ops" : 2,
          "setOwner_num_ops" : 0.0,
          "blockReceived_num_ops" : 2,
          "setSafeMode_avg_time" : 0.0,
          "getProtocolVersion_num_ops" : 89,
          "setOwner_avg_time" : 2.0,
          "blocksBeingWrittenReport_num_ops" : 0.0,
          "setSafeMode_num_ops" : 0.0,
          "setReplication_num_ops" : 0.0,
          "setPermission_avg_time" : 8.3,
          "setReplication_avg_time" : 6.5
        },
        "load" : {
          "load_fifteen" : 4.44444444444E-4,
          "load_one" : 0.0238333333333,
          "load_five" : 0.0313333333333
        },
        "jvm" : {
          "memHeapCommittedM" : 960.0,
          "logFatal" : 0,
          "threadsWaiting" : 104,
          "threadsBlocked" : 0,
          "gcCount" : 2,
          "logError" : 0,
          "logWarn" : 1,
          "memNonHeapCommittedM" : 23.375,
          "memNonHeapUsedM" : 22.489952,
          "gcTimeMillis" : 344,
          "logInfo" : 3,
          "memHeapUsedM" : 347.47256,
          "threadsNew" : 0,
          "threadsTerminated" : 0,
          "threadsTimedWaiting" : 8,
          "threadsRunnable" : 10
        },
        "memory" : {
          "mem_total" : 7514116.0,
          "swap_free" : 0.0,
          "mem_buffers" : 99546.4555556,
          "mem_shared" : 0.0,
          "mem_free" : 5698209.91111,
          "swap_total" : 0.0,
          "mem_cached" : 808452.411111
        },
        "network" : {
          "pkts_out" : 11.1144444444,
          "bytes_in" : 3120.62555556,
          "bytes_out" : 1943.344,
          "pkts_in" : 20.0075555556
        }
      },
      "host_components" : [
        {
          "href" : "http://your.ambari.server/api/v1/clusters/c1/hosts/ip-10-39-130-141.ec2.internal/host_components/NAMENODE",
          "HostRoles" : {
            "cluster_name" : "c1",
            "component_name" : "NAMENODE",
            "host_name" : "ip-10-39-130-141.ec2.internal"
          }
        }
      ]
    }


**Schema**

    {
      "type":"object",
      "$schema":"http://json-schema.org/draft-03/schema",
      "title":"Component",
      "required":true,
      "properties":{
        "ServiceComponentInfo":{
          "type":"object",
          "title":"ServiceComponentInfo",
          "description":"Service component information.",
          "required":true,
          "properties":{
            "BlocksTotal":{
              "type":"number",
              "required":false
            },
            "CapacityRemaining":{
              "type":"number",
              "required":false
            },
            "CapacityTotal":{
              "type":"number",
              "required":false
            },
            "CapacityUsed":{
              "type":"number",
              "required":false
            },
            "CorruptBlocks":{
              "type":"number",
              "required":false
            },
            "DeadNodes":{
              "type":"string",
              "required":false
            },
            "DecomNodes":{
              "type":"string",
              "required":false
            },
            "HeapMemoryMax":{
              "type":"number",
              "required":false
            },
            "HeapMemoryUsed":{
              "type":"number",
              "required":false
            },
            "LiveNodes":{
              "type":"string",
              "required":false
            },
            "MissingBlocks":{
              "type":"number",
              "required":false
            },
            "NonDfsUsedSpace":{
              "type":"number",
              "required":false
            },
            "NonHeapMemoryMax":{
              "type":"number",
              "required":false
            },
            "NonHeapMemoryUsed":{
              "type":"number",
              "required":false
            },
            "PercentRemaining":{
              "type":"number",
              "required":false
            },
            "PercentUsed":{
              "type":"number",
              "required":false
            },
            "Safemode":{
              "type":"string",
              "required":false
            },
            "StartTime":{
              "type":"number",
              "required":false
            },
            "TotalFiles":{
              "type":"number",
              "required":false
            },
            "UnderReplicatedBlocks":{
              "type":"number",
              "required":false
            },
            "UpgradeFinalized":{
              "type":"boolean",
              "required":false
            },
            "Version":{
              "type":"string",
              "required":false
            },
            "cluster_name":{
              "type":"string",
              "description":"The associated cluster name.",
              "required":true
            },
            "component_name":{
              "type":"string",
              "description":"The component name.",
              "required":true
            },
            "desired_configs":{
              "type":"object",
              "required":false
            },
            "service_name":{
              "type":"string",
              "description":"The associated service name.",
              "required":true
            },
            "state":{
              "type":"string",
              "description":"The component state.",
              "required":true
            }
          }
        },
        "host_components":{
          "type":"array",
          "title":"Host Component set",
          "required":true,
          "items":{
            "type":"object",
            "title":"Host Component",
            "required":false,
            "properties":{
              "HostRoles":{
                "type":"object",
                "title":"HostComponentInfo",
                "description":"Host component information.",
                "required":true,
                "properties":{
                  "cluster_name":{
                    "type":"string",
                    "title":"ClusterName",
                    "description":"Associated cluster name.",
                    "required":true
                  },
                  "component_name":{
                    "type":"string",
                    "title":"ComponentName",
                    "description":"Associated component name.",
                    "required":true
                  },
                  "host_name":{
                    "type":"string",
                    "title":"HostName",
                    "description":"Associated host name.",
                    "required":true
                  }
                }
              },
              "href":{
                "type":"string",
                "description":"This host component API href.",
                "required":true
              }
            }
          }
        },
        "href":{
          "type":"string",
          "description":"This component API href.",
          "required":true
        },
        "metrics":{
          "type":"object",
          "required":false
        }
      }
    }


GET hosts
----

**Example**


    GET api/v1/clusters/c1/hosts

    200 OK
    {
      "href" : "http://your.ambari.server/api/v1/clusters/c1/hosts",
      "items" : [
        {
          "href" : "http://your.ambari.server/api/v1/clusters/c1/hosts/ip-10-39-130-141.ec2.internal",
          "Hosts" : {
            "cluster_name" : "c1",
            "host_name" : "ip-10-39-130-141.ec2.internal"
          }
        },
        {
          "href" : "http://your.ambari.server/api/v1/clusters/c1/hosts/ip-10-83-74-200.ec2.internal",
          "Hosts" : {
            "cluster_name" : "c1",
            "host_name" : "ip-10-83-74-200.ec2.internal"
          }
        }
      ]
    }


**Schema**

    {
      "type":"object",
      "$schema":"http://json-schema.org/draft-03/schema",
      "title":"Hosts",
      "required":true,
      "properties":{
        "href":{
          "type":"string",
          "description":"This hosts API href.",
          "required":true
        },
        "items":{
          "type":"array",
          "title":"Host set",
          "required":true,
          "items":{
            "type":"object",
            "title":"Host",
            "required":false,
            "properties":{
              "Hosts":{
                "type":"object",
                "title":"HostInfo",
                "description":"Host information.",
                "required":true,
                "properties":{
                  "cluster_name":{
                    "type":"string",
                    "title":"ClusterName",
                    "description":"The associated cluster name.",
                    "required":true
                  },
                  "host_name":{
                    "type":"string",
                    "title":"HostName",
                    "description":"The host name.",
                    "required":true
                  }
                }
              },
              "href":{
                "type":"string",
                "description":"This host API href.",
                "required":true
              }
            }
          }
        }
      }
    }


GET host
----

**Example**


    GET api/v1/clusters/c1/hosts/ip-10-39-130-141.ec2.internal

    200 OK
    {
      "href" : "http://your.ambari.server/api/v1/clusters/c1/hosts/ip-10-39-130-141.ec2.internal",
      "metrics" : {
        "boottime" : 1.369844638E9,
        "part_max_used" : 35.0,
        "process" : {
          "proc_total" : 342.0,
          "proc_run" : 0.0
        },
        "rpc" : {
          "rpcAuthorizationFailures" : 0.0,
          "SentBytes" : 49.62,
          "ReceivedBytes" : 108.592222222,
          "RpcQueueTime_num_ops" : 0.34,
          "RpcProcessingTime_num_ops" : 0.34,
          "RpcProcessingTime_avg_time" : 0.0
        },
        "ugi" : {
          "loginFailure_num_ops" : 0.0,
          "loginSuccess_num_ops" : 0.0,
          "loginSuccess_avg_time" : 0.0,
          "loginFailure_avg_time" : 0.0
        },
        "disk" : {
          "disk_total" : 896.17,
          "disk_free" : 847.702
        },
        "cpu" : {
          "cpu_speed" : 2266.0,
          "cpu_num" : 2.0,
          "cpu_wio" : 0.223888888889,
          "cpu_idle" : 98.7116666667,
          "cpu_nice" : 0.0,
          "cpu_aidle" : 0.0,
          "cpu_system" : 0.709444444444,
          "cpu_user" : 0.383333333333
        },
        "rpcdetailed" : {
          "getTask_avg_time" : 1.0,
          "ping_avg_time" : 1.0,
          "done_avg_time" : 0.0,
          "getProtocolVersion_avg_time" : 0.0,
          "canCommit_num_ops" : 0.0,
          "done_num_ops" : 0.0,
          "ping_num_ops" : 0.0,
          "commitPending_avg_time" : 1.0,
          "statusUpdate_num_ops" : 0.0,
          "statusUpdate_avg_time" : 0.5,
          "getTask_num_ops" : 0.0,
          "getProtocolVersion_num_ops" : 0.0,
          "commitPending_num_ops" : 0.0,
          "canCommit_avg_time" : 0.0
        },
        "load" : {
          "load_fifteen" : 0.018,
          "load_one" : 0.0478888888889,
          "load_five" : 0.0584722222222
        },
        "jvm" : {
          "memHeapCommittedM" : 960.0,
          "logFatal" : 0.0,
          "threadsBlocked" : 0.0,
          "gcCount" : 0.0,
          "threadsWaiting" : 104.0,
          "logWarn" : 0.0,
          "logError" : 0.0,
          "memNonHeapCommittedM" : 23.375,
          "memNonHeapUsedM" : 22.886139,
          "gcTimeMillis" : 0.0,
          "logInfo" : 0.0,
          "memHeapUsedM" : 166.48633975,
          "threadsNew" : 0.0,
          "threadsTerminated" : 0.0,
          "threadsTimedWaiting" : 8.0,
          "threadsRunnable" : 10.0
        },
        "memory" : {
          "mem_total" : 7514116.0,
          "swap_free" : 0.0,
          "mem_buffers" : 150268.911111,
          "mem_shared" : 0.0,
          "mem_free" : 5434371.77778,
          "swap_total" : 0.0,
          "mem_cached" : 811750.477778
        },
        "network" : {
          "pkts_out" : 11.1887222222,
          "bytes_in" : 3185.43230556,
          "bytes_out" : 2271.82275,
          "pkts_in" : 19.62675
        }
      },
      "Hosts" : {
        "host_status" : "HEALTHY",
        "public_host_name" : "ec2-54-242-67-102.compute-1.amazonaws.com",
        "cpu_count" : 2,
        "rack_info" : "/default-rack",
        "host_health_report" : "",
        "os_arch" : "x86_64",
        "host_name" : "ip-10-39-130-141.ec2.internal",
        "disk_info" : [
          {
            "available" : "5882492",
            "used" : "1952456",
            "percent" : "25%",
            "size" : "8254240",
            "type" : "ext4",
            "mountpoint" : "/"
          },
          {
            "available" : "3757056",
            "used" : "0",
            "percent" : "0%",
            "size" : "3757056",
            "type" : "tmpfs",
            "mountpoint" : "/dev/shm"
          },
          {
            "available" : "411234588",
            "used" : "203012",
            "percent" : "1%",
            "size" : "433455904",
            "type" : "ext3",
            "mountpoint" : "/grid/0"
          },
          {
            "available" : "411234588",
            "used" : "203012",
            "percent" : "1%",
            "size" : "433455904",
            "type" : "ext3",
            "mountpoint" : "/grid/1"
          }
        ],
        "ip" : "10.39.130.141",
        "os_type" : "centos6",
        "last_heartbeat_time" : 1369920956327,
        "ph_cpu_count" : 1,
        "host_state" : "HEALTHY",
        "cluster_name" : "c1",
        "last_registration_time" : 1369845198478,
        "last_agent_env" : {
          "paths" : [
            {
              "name" : "/etc/hadoop",
              "type" : "directory"
            },
            {
              "name" : "/etc/hadoop/conf",
              "type" : "sym_link"
            },
            {
              "name" : "/etc/hbase",
              "type" : "not_exist"
            },
            {
              "name" : "/etc/hcatalog",
              "type" : "not_exist"
            },
            {
              "name" : "/etc/hive",
              "type" : "not_exist"
            },
            {
              "name" : "/etc/oozie",
              "type" : "not_exist"
            },
            {
              "name" : "/etc/sqoop",
              "type" : "not_exist"
            },
            {
              "name" : "/etc/ganglia",
              "type" : "directory"
            },
            {
              "name" : "/etc/nagios",
              "type" : "directory"
            },
            {
              "name" : "/var/run/hadoop",
              "type" : "directory"
            },
            {
              "name" : "/var/run/zookeeper",
              "type" : "not_exist"
            },
            {
              "name" : "/var/run/hbase",
              "type" : "not_exist"
            },
            {
              "name" : "/var/run/templeton",
              "type" : "not_exist"
            },
            {
              "name" : "/var/run/oozie",
              "type" : "not_exist"
            },
            {
              "name" : "/var/log/hadoop",
              "type" : "directory"
            },
            {
              "name" : "/var/log/zookeeper",
              "type" : "not_exist"
            },
            {
              "name" : "/var/log/hbase",
              "type" : "not_exist"
            },
            {
              "name" : "/var/run/templeton",
              "type" : "not_exist"
            },
            {
              "name" : "/var/log/hive",
              "type" : "not_exist"
            },
            {
              "name" : "/var/log/nagios",
              "type" : "directory"
            }
          ],
          "javaProcs" : [
            {
              "user" : "mapred",
              "pid" : 5706,
              "command" : "/usr/jdk/jdk1.6.0_31/bin/java -Dproc_tasktracker -Xmx1024m -Djava.net.preferIPv4Stack=true -Djava.net.preferIPv4Stack=true -Djava.net.preferIPv4Stack=true -server -Xmx1024m -Dhadoop.security.logger=ERROR,console -Dmapred.audit.logger=ERROR,console -Dhadoop.log.dir=/var/log/hadoop/mapred -Dhadoop.log.file=hadoop-mapred-tasktracker-ip-10-39-130-141.log -Dhadoop.home.dir=/usr/lib/hadoop/libexec/.. -Dhadoop.id.str=mapred -Dhadoop.root.logger=INFO,DRFA -Dhadoop.security.logger=INFO,NullAppender -Djava.library.path=/usr/lib/hadoop/libexec/../lib/native/Linux-amd64-64 -Dhadoop.policy.file=hadoop-policy.xml -classpath /etc/hadoop/conf:/usr/jdk/jdk1.6.0_31/lib/tools.jar:/usr/lib/hadoop/libexec/..:/usr/lib/hadoop/libexec/../hadoop-core-1.2.0.1.3.0.0-107.jar:/usr/lib/hadoop/libexec/../lib/ambari-log4j-1.2.5.9.jar:/usr/lib/hadoop/libexec/../lib/asm-3.2.jar:/usr/lib/hadoop/libexec/../lib/aspectjrt-1.6.11.jar:/usr/lib/hadoop/libexec/../lib/aspectjtools-1.6.11.jar:/usr/lib/hadoop/libexec/../lib/commons-beanutils-1.7.0.jar:/usr/lib/hadoop/libexec/../lib/commons-beanutils-core-1.8.0.jar:/usr/lib/hadoop/libexec/../lib/commons-cli-1.2.jar:/usr/lib/hadoop/libexec/../lib/commons-codec-1.4.jar:/usr/lib/hadoop/libexec/../lib/commons-collections-3.2.1.jar:/usr/lib/hadoop/libexec/../lib/commons-configuration-1.6.jar:/usr/lib/hadoop/libexec/../lib/commons-daemon-1.0.1.jar:/usr/lib/hadoop/libexec/../lib/commons-digester-1.8.jar:/usr/lib/hadoop/libexec/../lib/commons-el-1.0.jar:/usr/lib/hadoop/libexec/../lib/commons-httpclient-3.0.1.jar:/usr/lib/hadoop/libexec/../lib/commons-io-2.1.jar:/usr/lib/hadoop/libexec/../lib/commons-lang-2.4.jar:/usr/lib/hadoop/libexec/../lib/commons-logging-1.1.1.jar:/usr/lib/hadoop/libexec/../lib/commons-logging-api-1.0.4.jar:/usr/lib/hadoop/libexec/../lib/commons-math-2.1.jar:/usr/lib/hadoop/libexec/../lib/commons-net-3.1.jar:/usr/lib/hadoop/libexec/../lib/core-3.1.1.jar:/usr/lib/hadoop/libexec/../lib/guava-11.0.2.jar:/usr/lib/hadoop/libexec/../lib/hadoop-capacity-scheduler-1.2.0.1.3.0.0-107.jar:/usr/lib/hadoop/libexec/../lib/hadoop-fairscheduler-1.2.0.1.3.0.0-107.jar:/usr/lib/hadoop/libexec/../lib/hadoop-lzo-0.5.0.jar:/usr/lib/hadoop/libexec/../lib/hadoop-thriftfs-1.2.0.1.3.0.0-107.jar:/usr/lib/hadoop/libexec/../lib/hadoop-tools.jar:/usr/lib/hadoop/libexec/../lib/hsqldb-1.8.0.10.jar:/usr/lib/hadoop/libexec/../lib/jackson-core-asl-1.8.8.jar:/usr/lib/hadoop/libexec/../lib/jackson-mapper-asl-1.8.8.jar:/usr/lib/hadoop/libexec/../lib/jasper-compiler-5.5.12.jar:/usr/lib/hadoop/libexec/../lib/jasper-runtime-5.5.12.jar:/usr/lib/hadoop/libexec/../lib/jdeb-0.8.jar:/usr/lib/hadoop/libexec/../lib/jersey-core-1.8.jar:/usr/lib/hadoop/libexec/../lib/jersey-json-1.8.jar:/usr/lib/hadoop/libexec/../lib/jersey-server-1.8.jar:/usr/lib/hadoop/libexec/../lib/jets3t-0.6.1.jar:/usr/lib/hadoop/libexec/../lib/jetty-6.1.26.jar:/usr/lib/hadoop/libexec/../lib/jetty-util-6.1.26.jar:/usr/lib/hadoop/libexec/../lib/jsch-0.1.42.jar:/usr/lib/hadoop/libexec/../lib/junit-4.5.jar:/usr/lib/hadoop/libexec/../lib/kfs-0.2.2.jar:/usr/lib/hadoop/libexec/../lib/log4j-1.2.15.jar:/usr/lib/hadoop/libexec/../lib/mockito-all-1.8.5.jar:/usr/lib/hadoop/libexec/../lib/netty-3.6.2.Final.jar:/usr/lib/hadoop/libexec/../lib/oro-2.0.8.jar:/usr/lib/hadoop/libexec/../lib/postgresql-9.1-901-1.jdbc4.jar:/usr/lib/hadoop/libexec/../lib/servlet-api-2.5-20081211.jar:/usr/lib/hadoop/libexec/../lib/slf4j-api-1.4.3.jar:/usr/lib/hadoop/libexec/../lib/slf4j-log4j12-1.4.3.jar:/usr/lib/hadoop/libexec/../lib/xmlenc-0.52.jar:/usr/lib/hadoop/libexec/../lib/jsp-2.1/jsp-2.1.jar:/usr/lib/hadoop/libexec/../lib/jsp-2.1/jsp-api-2.1.jar::/usr/lib/hadoop-mapreduce/*:/usr/lib/hadoop-mapreduce/*:/usr/lib/hadoop-mapreduce/* org.apache.hadoop.mapred.TaskTracker",
              "hadoop" : true
            },
            {
              "user" : "hdfs",
              "pid" : 7655,
              "command" : "/usr/jdk/jdk1.6.0_31/bin/java -Dproc_datanode -Xmx1024m -Djava.net.preferIPv4Stack=true -Djava.net.preferIPv4Stack=true -Djava.net.preferIPv4Stack=true -server -Xmx1024m -Dhadoop.security.logger=ERROR,DRFAS -Dhadoop.log.dir=/var/log/hadoop/hdfs -Dhadoop.log.file=hadoop-hdfs-datanode-ip-10-39-130-141.log -Dhadoop.home.dir=/usr/lib/hadoop/libexec/.. -Dhadoop.id.str=hdfs -Dhadoop.root.logger=INFO,DRFA -Dhadoop.security.logger=INFO,NullAppender -Djava.library.path=/usr/lib/hadoop/libexec/../lib/native/Linux-amd64-64 -Dhadoop.policy.file=hadoop-policy.xml -classpath /etc/hadoop/conf:/usr/jdk/jdk1.6.0_31/lib/tools.jar:/usr/lib/hadoop/libexec/..:/usr/lib/hadoop/libexec/../hadoop-core-1.2.0.1.3.0.0-107.jar:/usr/lib/hadoop/libexec/../lib/ambari-log4j-1.2.5.9.jar:/usr/lib/hadoop/libexec/../lib/asm-3.2.jar:/usr/lib/hadoop/libexec/../lib/aspectjrt-1.6.11.jar:/usr/lib/hadoop/libexec/../lib/aspectjtools-1.6.11.jar:/usr/lib/hadoop/libexec/../lib/commons-beanutils-1.7.0.jar:/usr/lib/hadoop/libexec/../lib/commons-beanutils-core-1.8.0.jar:/usr/lib/hadoop/libexec/../lib/commons-cli-1.2.jar:/usr/lib/hadoop/libexec/../lib/commons-codec-1.4.jar:/usr/lib/hadoop/libexec/../lib/commons-collections-3.2.1.jar:/usr/lib/hadoop/libexec/../lib/commons-configuration-1.6.jar:/usr/lib/hadoop/libexec/../lib/commons-daemon-1.0.1.jar:/usr/lib/hadoop/libexec/../lib/commons-digester-1.8.jar:/usr/lib/hadoop/libexec/../lib/commons-el-1.0.jar:/usr/lib/hadoop/libexec/../lib/commons-httpclient-3.0.1.jar:/usr/lib/hadoop/libexec/../lib/commons-io-2.1.jar:/usr/lib/hadoop/libexec/../lib/commons-lang-2.4.jar:/usr/lib/hadoop/libexec/../lib/commons-logging-1.1.1.jar:/usr/lib/hadoop/libexec/../lib/commons-logging-api-1.0.4.jar:/usr/lib/hadoop/libexec/../lib/commons-math-2.1.jar:/usr/lib/hadoop/libexec/../lib/commons-net-3.1.jar:/usr/lib/hadoop/libexec/../lib/core-3.1.1.jar:/usr/lib/hadoop/libexec/../lib/guava-11.0.2.jar:/usr/lib/hadoop/libexec/../lib/hadoop-capacity-scheduler-1.2.0.1.3.0.0-107.jar:/usr/lib/hadoop/libexec/../lib/hadoop-fairscheduler-1.2.0.1.3.0.0-107.jar:/usr/lib/hadoop/libexec/../lib/hadoop-lzo-0.5.0.jar:/usr/lib/hadoop/libexec/../lib/hadoop-thriftfs-1.2.0.1.3.0.0-107.jar:/usr/lib/hadoop/libexec/../lib/hadoop-tools.jar:/usr/lib/hadoop/libexec/../lib/hsqldb-1.8.0.10.jar:/usr/lib/hadoop/libexec/../lib/jackson-core-asl-1.8.8.jar:/usr/lib/hadoop/libexec/../lib/jackson-mapper-asl-1.8.8.jar:/usr/lib/hadoop/libexec/../lib/jasper-compiler-5.5.12.jar:/usr/lib/hadoop/libexec/../lib/jasper-runtime-5.5.12.jar:/usr/lib/hadoop/libexec/../lib/jdeb-0.8.jar:/usr/lib/hadoop/libexec/../lib/jersey-core-1.8.jar:/usr/lib/hadoop/libexec/../lib/jersey-json-1.8.jar:/usr/lib/hadoop/libexec/../lib/jersey-server-1.8.jar:/usr/lib/hadoop/libexec/../lib/jets3t-0.6.1.jar:/usr/lib/hadoop/libexec/../lib/jetty-6.1.26.jar:/usr/lib/hadoop/libexec/../lib/jetty-util-6.1.26.jar:/usr/lib/hadoop/libexec/../lib/jsch-0.1.42.jar:/usr/lib/hadoop/libexec/../lib/junit-4.5.jar:/usr/lib/hadoop/libexec/../lib/kfs-0.2.2.jar:/usr/lib/hadoop/libexec/../lib/log4j-1.2.15.jar:/usr/lib/hadoop/libexec/../lib/mockito-all-1.8.5.jar:/usr/lib/hadoop/libexec/../lib/netty-3.6.2.Final.jar:/usr/lib/hadoop/libexec/../lib/oro-2.0.8.jar:/usr/lib/hadoop/libexec/../lib/postgresql-9.1-901-1.jdbc4.jar:/usr/lib/hadoop/libexec/../lib/servlet-api-2.5-20081211.jar:/usr/lib/hadoop/libexec/../lib/slf4j-api-1.4.3.jar:/usr/lib/hadoop/libexec/../lib/slf4j-log4j12-1.4.3.jar:/usr/lib/hadoop/libexec/../lib/xmlenc-0.52.jar:/usr/lib/hadoop/libexec/../lib/jsp-2.1/jsp-2.1.jar:/usr/lib/hadoop/libexec/../lib/jsp-2.1/jsp-api-2.1.jar::/usr/lib/hadoop-mapreduce/*:/usr/lib/hadoop-mapreduce/*:/usr/lib/hadoop-mapreduce/* org.apache.hadoop.hdfs.server.datanode.DataNode",
              "hadoop" : true
            },
            {
              "user" : "hdfs",
              "pid" : 8053,
              "command" : "/usr/jdk/jdk1.6.0_31/bin/java -Dproc_namenode -Xmx1024m -Djava.net.preferIPv4Stack=true -Djava.net.preferIPv4Stack=true -Djava.net.preferIPv4Stack=true -server -XX:ParallelGCThreads=8 -XX:+UseConcMarkSweepGC -XX:ErrorFile=/var/log/hadoop/hdfs/hs_err_pid%p.log -XX:NewSize=200m -XX:MaxNewSize=640m -Xloggc:/var/log/hadoop/hdfs/gc.log-201305291832 -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+PrintGCDateStamps -Xms1024m -Xmx1024m -Dhadoop.security.logger=INFO,DRFAS -Dhdfs.audit.logger=INFO,DRFAAUDIT -server -XX:ParallelGCThreads=8 -XX:+UseConcMarkSweepGC -XX:ErrorFile=/var/log/hadoop/hdfs/hs_err_pid%p.log -XX:NewSize=200m -XX:MaxNewSize=640m -Xloggc:/var/log/hadoop/hdfs/gc.log-201305291832 -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+PrintGCDateStamps -Xms1024m -Xmx1024m -Dhadoop.security.logger=INFO,DRFAS -Dhdfs.audit.logger=INFO,DRFAAUDIT -server -XX:ParallelGCThreads=8 -XX:+UseConcMarkSweepGC -XX:ErrorFile=/var/log/hadoop/hdfs/hs_err_pid%p.log -XX:NewSize=200m -XX:MaxNewSize=640m -Xloggc:/var/log/hadoop/hdfs/gc.log-201305291832 -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+PrintGCDateStamps -Xms1024m -Xmx1024m -Dhadoop.security.logger=INFO,DRFAS -Dhdfs.audit.logger=INFO,DRFAAUDIT -Dhadoop.log.dir=/var/log/hadoop/hdfs -Dhadoop.log.file=hadoop-hdfs-namenode-ip-10-39-130-141.log -Dhadoop.home.dir=/usr/lib/hadoop/libexec/.. -Dhadoop.id.str=hdfs -Dhadoop.root.logger=INFO,DRFA -Dhadoop.security.logger=INFO,DRFAS -Djava.library.path=/usr/lib/hadoop/libexec/../lib/native/Linux-amd64-64 -Dhadoop.policy.file=hadoop-policy.xml -classpath /etc/hadoop/conf:/usr/jdk/jdk1.6.0_31/lib/tools.jar:/usr/lib/hadoop/libexec/..:/usr/lib/hadoop/libexec/../hadoop-core-1.2.0.1.3.0.0-107.jar:/usr/lib/hadoop/libexec/../lib/ambari-log4j-1.2.5.9.jar:/usr/lib/hadoop/libexec/../lib/asm-3.2.jar:/usr/lib/hadoop/libexec/../lib/aspectjrt-1.6.11.jar:/usr/lib/hadoop/libexec/../lib/aspectjtools-1.6.11.jar:/usr/lib/hadoop/libexec/../lib/commons-beanutils-1.7.0.jar:/usr/lib/hadoop/libexec/../lib/commons-beanutils-core-1.8.0.jar:/usr/lib/hadoop/libexec/../lib/commons-cli-1.2.jar:/usr/lib/hadoop/libexec/../lib/commons-codec-1.4.jar:/usr/lib/hadoop/libexec/../lib/commons-collections-3.2.1.jar:/usr/lib/hadoop/libexec/../lib/commons-configuration-1.6.jar:/usr/lib/hadoop/libexec/../lib/commons-daemon-1.0.1.jar:/usr/lib/hadoop/libexec/../lib/commons-digester-1.8.jar:/usr/lib/hadoop/libexec/../lib/commons-el-1.0.jar:/usr/lib/hadoop/libexec/../lib/commons-httpclient-3.0.1.jar:/usr/lib/hadoop/libexec/../lib/commons-io-2.1.jar:/usr/lib/hadoop/libexec/../lib/commons-lang-2.4.jar:/usr/lib/hadoop/libexec/../lib/commons-logging-1.1.1.jar:/usr/lib/hadoop/libexec/../lib/commons-logging-api-1.0.4.jar:/usr/lib/hadoop/libexec/../lib/commons-math-2.1.jar:/usr/lib/hadoop/libexec/../lib/commons-net-3.1.jar:/usr/lib/hadoop/libexec/../lib/core-3.1.1.jar:/usr/lib/hadoop/libexec/../lib/guava-11.0.2.jar:/usr/lib/hadoop/libexec/../lib/hadoop-capacity-scheduler-1.2.0.1.3.0.0-107.jar:/usr/lib/hadoop/libexec/../lib/hadoop-fairscheduler-1.2.0.1.3.0.0-107.jar:/usr/lib/hadoop/libexec/../lib/hadoop-lzo-0.5.0.jar:/usr/lib/hadoop/libexec/../lib/hadoop-thriftfs-1.2.0.1.3.0.0-107.jar:/usr/lib/hadoop/libexec/../lib/hadoop-tools.jar:/usr/lib/hadoop/libexec/../lib/hsqldb-1.8.0.10.jar:/usr/lib/hadoop/libexec/../lib/jackson-core-asl-1.8.8.jar:/usr/lib/hadoop/libexec/../lib/jackson-mapper-asl-1.8.8.jar:/usr/lib/hadoop/libexec/../lib/jasper-compiler-5.5.12.jar:/usr/lib/hadoop/libexec/../lib/jasper-runtime-5.5.12.jar:/usr/lib/hadoop/libexec/../lib/jdeb-0.8.jar:/usr/lib/hadoop/libexec/../lib/jersey-core-1.8.jar:/usr/lib/hadoop/libexec/../lib/jersey-json-1.8.jar:/usr/lib/hadoop/libexec/../lib/jersey-server-1.8.jar:/usr/lib/hadoop/libexec/../lib/jets3t-0.6.1.jar:/usr/lib/hadoop/libexec/../lib/jetty-6.1.26.jar:/usr/lib/hadoop/libexec/../lib/jetty-util-6.1.26.jar:/usr/lib/hadoop/libexec/../lib/jsch-0.1.42.jar:/usr/lib/hadoop/libexec/../lib/junit-4.5.jar:/usr/lib/hadoop/libexec/../lib/kfs-0.2.2.jar:/usr/lib/hadoop/libexec/../lib/log4j-1.2.15.jar:/usr/lib/hadoop/libexec/../l",
              "hadoop" : true
            }
          ],
          "rpms" : [
            {
              "name" : "nagios",
              "installed" : true,
              "version" : "nagios-3.2.3-2.el6.x86_64"
            },
            {
              "name" : "ganglia",
              "installed" : false
            },
            {
              "name" : "hadoop",
              "installed" : true,
              "version" : "hadoop-1.2.0.1.3.0.0-107.el6.x86_64"
            },
            {
              "name" : "hadoop-lzo",
              "installed" : true,
              "version" : "hadoop-lzo-0.5.0-1.x86_64"
            },
            {
              "name" : "hbase",
              "installed" : false
            },
            {
              "name" : "oozie",
              "installed" : false
            },
            {
              "name" : "sqoop",
              "installed" : false
            },
            {
              "name" : "pig",
              "installed" : false
            },
            {
              "name" : "zookeeper",
              "installed" : false
            },
            {
              "name" : "hive",
              "installed" : false
            },
            {
              "name" : "libconfuse",
              "installed" : true,
              "version" : "libconfuse-2.7-4.el6.x86_64"
            },
            {
              "name" : "ambari-log4j",
              "installed" : true,
              "version" : "ambari-log4j-1.2.5.9-1.noarch"
            }
          ],
          "varRunHadoopPidCount" : 3,
          "varLogHadoopLogCount" : 4,
          "etcAlternativesConf" : [
            {
              "name" : "/etc/alternatives/hadoop-conf",
              "target" : "/etc/hadoop/conf.empty"
            }
          ],
          "repoInfo" : "could_not_determine"
        },
        "total_mem" : 7518289,
        "desired_configs" : { }
      },
      "host_components" : [
        {
          "href" : "http://your.ambari.server/api/v1/clusters/c1/hosts/ip-10-39-130-141.ec2.internal/host_components/GANGLIA_SERVER",
          "HostRoles" : {
            "cluster_name" : "c1",
            "component_name" : "GANGLIA_SERVER",
            "host_name" : "ip-10-39-130-141.ec2.internal"
          }
        },
        {
          "href" : "http://your.ambari.server/api/v1/clusters/c1/hosts/ip-10-39-130-141.ec2.internal/host_components/DATANODE",
          "HostRoles" : {
            "cluster_name" : "c1",
            "component_name" : "DATANODE",
            "host_name" : "ip-10-39-130-141.ec2.internal"
          }
        },
        {
          "href" : "http://your.ambari.server/api/v1/clusters/c1/hosts/ip-10-39-130-141.ec2.internal/host_components/MAPREDUCE_CLIENT",
          "HostRoles" : {
            "cluster_name" : "c1",
            "component_name" : "MAPREDUCE_CLIENT",
            "host_name" : "ip-10-39-130-141.ec2.internal"
          }
        },
        {
          "href" : "http://your.ambari.server/api/v1/clusters/c1/hosts/ip-10-39-130-141.ec2.internal/host_components/NAMENODE",
          "HostRoles" : {
            "cluster_name" : "c1",
            "component_name" : "NAMENODE",
            "host_name" : "ip-10-39-130-141.ec2.internal"
          }
        },
        {
          "href" : "http://your.ambari.server/api/v1/clusters/c1/hosts/ip-10-39-130-141.ec2.internal/host_components/HDFS_CLIENT",
          "HostRoles" : {
            "cluster_name" : "c1",
            "component_name" : "HDFS_CLIENT",
            "host_name" : "ip-10-39-130-141.ec2.internal"
          }
        },
        {
          "href" : "http://your.ambari.server/api/v1/clusters/c1/hosts/ip-10-39-130-141.ec2.internal/host_components/TASKTRACKER",
          "HostRoles" : {
            "cluster_name" : "c1",
            "component_name" : "TASKTRACKER",
            "host_name" : "ip-10-39-130-141.ec2.internal"
          }
        },
        {
          "href" : "http://your.ambari.server/api/v1/clusters/c1/hosts/ip-10-39-130-141.ec2.internal/host_components/NAGIOS_SERVER",
          "HostRoles" : {
            "cluster_name" : "c1",
            "component_name" : "NAGIOS_SERVER",
            "host_name" : "ip-10-39-130-141.ec2.internal"
          }
        },
        {
          "href" : "http://your.ambari.server/api/v1/clusters/c1/hosts/ip-10-39-130-141.ec2.internal/host_components/GANGLIA_MONITOR",
          "HostRoles" : {
            "cluster_name" : "c1",
            "component_name" : "GANGLIA_MONITOR",
            "host_name" : "ip-10-39-130-141.ec2.internal"
          }
        }
      ]
    }


**Schema**

    {
      "type":"object",
      "$schema": "http://json-schema.org/draft-03/schema",
      "title": "Host",
      "required":true,
      "properties":{
        "Hosts": {
          "type":"object",
          "title": "HostInfo",
          "description": "Host information.",
          "required":true,
          "properties":{
            "cluster_name": {
              "type":"string",
              "title": "ClusterName",
              "description": "The associated cluster name.",
              "required":true
            },
            "cpu_count": {
              "type":"number",
              "required":false
            },
            "desired_configs":{
              "type":"object",
              "required":false
            },
            "disk_info": {
              "type":"array",
              "title": "DiskInfo set",
              "required":false,
              "items":
              {
                "type":"object",
                "title": "DiskInfo",
                "description": "Disk information.",
                "required":false
              }
            },
            "host_health_report": {
              "type":"string",
              "required":false
            },
            "host_name": {
              "type":"string",
              "title": "HostName",
              "description": "The host name.",
              "required":true
            },
            "host_state": {
              "type":"string",
              "title": "HostState",
              "description": "The state of the host.",
              "required":false
            },
            "host_status": {
              "type":"string",
              "title": "HostStatus",
              "required":false
            },
            "ip": {
              "type":"string",
              "required":true
            },
            "last_agent_env": {
              "type":"object",
              "required":false
            },
            "last_heartbeat_time": {
              "type":"number",
              "required":false
            },
            "last_registration_time": {
              "type":"number",
              "required":false
            },
            "os_arch": {
              "type":"string",
              "required":true
            },
            "os_type": {
              "type":"string",
              "required":true
            },
            "ph_cpu_count": {
              "type":"number",
              "required":false
            },
            "public_host_name": {
              "type":"string",
              "required":false
            },
            "rack_info": {
              "type":"string",
              "required":false
            },
            "total_mem": {
              "type":"number",
              "required":false
            }
          }
        },
        "host_components": {
          "type":"array",
          "title": "Host Component set",
          "required":true,
          "items":
          {
            "type":"object",
            "title": "HostComponent",
            "required":false,
            "properties":{
              "HostRoles": {
                "type":"object",
                "title": "HostComponentInfo",
                "description": "Host component information.",
                "required":true,
                "properties":{
                  "cluster_name": {
                    "type":"string",
                    "title": "ClusterName",
                    "description": "The associated cluster name.",
                    "required":true
                  },
                  "component_name": {
                    "type":"string",
                    "title": "ComponentName",
                    "description": "The associated component name.",
                    "required":true
                  },
                  "host_name": {
                    "type":"string",
                    "title": "HostName",
                    "description": "The associated host name.",
                    "required":true
                  }
                }
              },
              "href": {
                "type":"string",
                "description": "This host component API href.",
                "required":true
              }
            }
          }


        },
        "href": {
          "type":"string",
          "description": "This host API href.",
          "required":true
        },
        "metrics":{
          "type":"object",
          "required":false
        }
      }
    }



GET host_component
----

**Example**


    GET api/v1/clusters/c1/hosts/ip-10-39-130-141.ec2.internal/host_components

    200 OK
    {
      "href" : "http://your.ambari.server/api/v1/clusters/c1/hosts/ip-10-39-130-141.ec2.internal/host_components",
      "items" : [
        {
          "href" : "http://your.ambari.server/api/v1/clusters/c1/hosts/ip-10-39-130-141.ec2.internal/host_components/MAPREDUCE_CLIENT",
          "HostRoles" : {
            "cluster_name" : "c1",
            "component_name" : "MAPREDUCE_CLIENT",
            "host_name" : "ip-10-39-130-141.ec2.internal"
          },
          "host" : {
            "href" : "http://your.ambari.server/api/v1/clusters/c1/hosts/ip-10-39-130-141.ec2.internal"
          }
        },
        {
          "href" : "http://your.ambari.server/api/v1/clusters/c1/hosts/ip-10-39-130-141.ec2.internal/host_components/NAGIOS_SERVER",
          "HostRoles" : {
            "cluster_name" : "c1",
            "component_name" : "NAGIOS_SERVER",
            "host_name" : "ip-10-39-130-141.ec2.internal"
          },
          "host" : {
            "href" : "http://your.ambari.server/api/v1/clusters/c1/hosts/ip-10-39-130-141.ec2.internal"
          }
        },
        {
          "href" : "http://your.ambari.server/api/v1/clusters/c1/hosts/ip-10-39-130-141.ec2.internal/host_components/TASKTRACKER",
          "HostRoles" : {
            "cluster_name" : "c1",
            "component_name" : "TASKTRACKER",
            "host_name" : "ip-10-39-130-141.ec2.internal"
          },
          "host" : {
            "href" : "http://your.ambari.server/api/v1/clusters/c1/hosts/ip-10-39-130-141.ec2.internal"
          }
        },
        {
          "href" : "http://your.ambari.server/api/v1/clusters/c1/hosts/ip-10-39-130-141.ec2.internal/host_components/GANGLIA_SERVER",
          "HostRoles" : {
            "cluster_name" : "c1",
            "component_name" : "GANGLIA_SERVER",
            "host_name" : "ip-10-39-130-141.ec2.internal"
          },
          "host" : {
            "href" : "http://your.ambari.server/api/v1/clusters/c1/hosts/ip-10-39-130-141.ec2.internal"
          }
        },
        {
          "href" : "http://your.ambari.server/api/v1/clusters/c1/hosts/ip-10-39-130-141.ec2.internal/host_components/DATANODE",
          "HostRoles" : {
            "cluster_name" : "c1",
            "component_name" : "DATANODE",
            "host_name" : "ip-10-39-130-141.ec2.internal"
          },
          "host" : {
            "href" : "http://your.ambari.server/api/v1/clusters/c1/hosts/ip-10-39-130-141.ec2.internal"
          }
        },
        {
          "href" : "http://your.ambari.server/api/v1/clusters/c1/hosts/ip-10-39-130-141.ec2.internal/host_components/GANGLIA_MONITOR",
          "HostRoles" : {
            "cluster_name" : "c1",
            "component_name" : "GANGLIA_MONITOR",
            "host_name" : "ip-10-39-130-141.ec2.internal"
          },
          "host" : {
            "href" : "http://your.ambari.server/api/v1/clusters/c1/hosts/ip-10-39-130-141.ec2.internal"
          }
        },
        {
          "href" : "http://your.ambari.server/api/v1/clusters/c1/hosts/ip-10-39-130-141.ec2.internal/host_components/NAMENODE",
          "HostRoles" : {
            "cluster_name" : "c1",
            "component_name" : "NAMENODE",
            "host_name" : "ip-10-39-130-141.ec2.internal"
          },
          "host" : {
            "href" : "http://your.ambari.server/api/v1/clusters/c1/hosts/ip-10-39-130-141.ec2.internal"
          }
        },
        {
          "href" : "http://your.ambari.server/api/v1/clusters/c1/hosts/ip-10-39-130-141.ec2.internal/host_components/HDFS_CLIENT",
          "HostRoles" : {
            "cluster_name" : "c1",
            "component_name" : "HDFS_CLIENT",
            "host_name" : "ip-10-39-130-141.ec2.internal"
          },
          "host" : {
            "href" : "http://your.ambari.server/api/v1/clusters/c1/hosts/ip-10-39-130-141.ec2.internal"
          }
        }
      ]
    }


**Schema**

    {
      "type":"object",
      "$schema":"http://json-schema.org/draft-03/schema",
      "title":"HostComponents",
      "required":true,
      "properties":{
        "href":{
          "type":"string",
          "description":"This host components API href.",
          "required":true
        },
        "items":{
          "type":"array",
          "title":"Host Component set",
          "required":true,
          "items":{
            "type":"object",
            "title":"HostComponent",
            "required":false,
            "properties":{
              "HostRoles":{
                "type":"object",
                "title":"HostComponentInfo",
                "description":"Host component information.",
                "required":true,
                "properties":{
                  "cluster_name":{
                    "type":"string",
                    "title":"ClusterName",
                    "description":"The associated cluster name.",
                    "required":true
                  },
                  "component_name":{
                    "type":"string",
                    "title":"ComponentName",
                    "description":"The associated component name.",
                    "required":true
                  },
                  "host_name":{
                    "type":"string",
                    "title":"HostName",
                    "description":"The associated host name.",
                    "required":true
                  }
                }
              },
              "host":{
                "type":"object",
                "title":"Host",
                "description":"The associateed host.",
                "required":true,
                "properties":{
                  "href":{
                    "type":"string",
                    "description":"This host API href.",
                    "required":true
                  }
                }
              },
              "href":{
                "type":"string",
                "description":"This host component API href.",
                "required":true
              }
            }
          }
        }
      }
    }


GET host_component
----

**Example**


    GET api/v1/clusters/c1/hosts/ip-10-39-130-141.ec2.internal/host_components/NAMENODE

    200 OK
    {
      "href" : "http://your.ambari.server/api/v1/clusters/c1/hosts/ip-10-39-130-141.ec2.internal/host_components/NAMENODE",
      "HostRoles" : {
        "cluster_name" : "c1",
        "desired_state" : "STARTED",
        "component_name" : "NAMENODE",
        "state" : "STARTED",
        "host_name" : "ip-10-39-130-141.ec2.internal",
        "desired_stack_id" : "HDP-1.3.0",
        "stack_id" : "HDP-1.3.0",
        "configs" : { },
        "desired_configs" : { },
        "actual_configs" : {
          "mapred-site" : {
            "user" : null,
            "tag" : "version1"
          },
          "hdfs-site" : {
            "user" : null,
            "tag" : "version1"
          },
          "global" : {
            "user" : null,
            "tag" : "version1369851987025"
          },
          "core-site" : {
            "user" : null,
            "tag" : "version1369851987025"
          }
        }
      },
      "host" : {
        "href" : "http://your.ambari.server/api/v1/clusters/c1/hosts/ip-10-39-130-141.ec2.internal"
      },
      "metrics" : {
        "boottime" : 1.369844638E9,
        "process" : {
          "proc_total" : 336.0,
          "proc_run" : 0.0
        },
        "rpc" : {
          "rpcAuthorizationSuccesses" : 274,
          "rpcAuthorizationFailures" : 0,
          "SentBytes" : 3586747,
          "ReceivedBytes" : 7824880,
          "NumOpenConnections" : 0,
          "callQueueLen" : 0,
          "rpcAuthenticationSuccesses" : 0,
          "RpcQueueTime_num_ops" : 24533,
          "RpcProcessingTime_num_ops" : 24533,
          "RpcProcessingTime_avg_time" : 0.0,
          "rpcAuthenticationFailures" : 0,
          "RpcQueueTime_avg_time" : 0.0
        },
        "dfs" : {
          "namenode" : {
            "Threads" : 122,
            "PercentRemaining" : 94.873405,
            "JournalTransactionsBatchedInSync" : 0.0,
            "CreateFileOps" : 2,
            "GetListingOps" : 17,
            "UpgradeFinalized" : true,
            "Transactions_num_ops" : 15,
            "Free" : 842208030720,
            "GetBlockLocations" : 0,
            "NameDirStatuses" : "{\"failed\":{},\"active\":{\"/grid/1/hadoop/hdfs/namenode\":\"IMAGE_AND_EDITS\",\"/grid/0/hadoop/hdfs/namenode\":\"IMAGE_AND_EDITS\"}}",
            "DecomNodes" : "{}",
            "blockReport_num_ops" : 2,
            "Safemode" : "",
            "SafemodeTime" : 65061.0,
            "FilesInGetListingOps" : 32,
            "Transactions_avg_time" : 0.0,
            "TotalBlocks" : 7,
            "DeleteFileOps" : 0.0,
            "FilesCreated" : 3,
            "Version" : "1.2.0.1.3.0.0-107, rd4625cb994e0143f5f4b538f0f2f4a41ad6464a2",
            "AddBlockOps" : 2,
            "fsImageLoadTime" : 1141.0,
            "FilesRenamed" : 0.0,
            "LiveNodes" : "{\"ip-10-39-130-141.ec2.internal\":{\"usedSpace\":184320,\"lastContact\":1}}",
            "TotalFiles" : 23,
            "PercentUsed" : 2.0763358E-5,
            "FileInfoOps" : 40,
            "NonDfsUsedSpace" : 45509476352,
            "Syncs_avg_time" : 3.0,
            "HostName" : "ip-10-39-130-141.ec2.internal",
            "Syncs_num_ops" : 11,
            "Used" : 184320,
            "FilesDeleted" : 0.0,
            "FilesAppended" : 0.0,
            "blockReport_avg_time" : 0.0,
            "Total" : 887717691392,
            "DeadNodes" : "{}"
          },
          "FSNamesystem" : {
            "BlocksTotal" : 7,
            "ScheduledReplicationBlocks" : 0,
            "CapacityTotalGB" : 827,
            "CapacityUsedGB" : 0,
            "CapacityUsed" : 184320,
            "ExcessBlocks" : 0,
            "MissingBlocks" : 0,
            "PendingReplicationBlocks" : 0,
            "FilesTotal" : 23,
            "CapacityRemainingGB" : 784,
            "CapacityRemaining" : 842208030720,
            "UnderReplicatedBlocks" : 7,
            "TotalLoad" : 1,
            "CapacityTotal" : 887717691392,
            "PendingDeletionBlocks" : 0,
            "CorruptBlocks" : 0,
            "BlockCapacity" : 2097152
          }
        },
        "ugi" : {
          "loginSuccess_num_ops" : 0,
          "loginFailure_num_ops" : 0,
          "loginSuccess_avg_time" : 0.0,
          "loginFailure_avg_time" : 0.0
        },
        "disk" : {
          "disk_total" : 896.17,
          "disk_free" : 847.702,
          "part_max_used" : 35.0
        },
        "cpu" : {
          "cpu_speed" : 2266.0,
          "cpu_num" : 2.0,
          "cpu_wio" : 1.90194444444,
          "cpu_idle" : 96.9752777778,
          "cpu_nice" : 0.0,
          "cpu_aidle" : 0.0,
          "cpu_system" : 0.695555555556,
          "cpu_user" : 0.415833333333
        },
        "rpcdetailed" : {
          "addBlock_avg_time" : 1.0,
          "versionRequest_num_ops" : 0.0,
          "register_num_ops" : 0.0,
          "getListing_num_ops" : 17,
          "sendHeartbeat_num_ops" : 24030,
          "blocksBeingWrittenReport_avg_time" : 1.0,
          "rename_num_ops" : 0.0,
          "create_avg_time" : 6.0,
          "mkdirs_avg_time" : 16.0,
          "delete_num_ops" : 0.0,
          "create_num_ops" : 2,
          "mkdirs_num_ops" : 0.0,
          "delete_avg_time" : 18.0,
          "addBlock_num_ops" : 2,
          "getFileInfo_avg_time" : 1.0,
          "rename_avg_time" : 2.0,
          "getProtocolVersion_avg_time" : 0.0,
          "getListing_avg_time" : 1.0,
          "blockReceived_avg_time" : 0.0,
          "getFileInfo_num_ops" : 40,
          "register_avg_time" : 4.0,
          "setPermission_num_ops" : 0.0,
          "sendHeartbeat_avg_time" : 0.0,
          "complete_avg_time" : 2.0,
          "versionRequest_avg_time" : 1.0,
          "complete_num_ops" : 2,
          "setOwner_num_ops" : 0.0,
          "blockReceived_num_ops" : 2,
          "setSafeMode_avg_time" : 0.0,
          "getProtocolVersion_num_ops" : 89,
          "setOwner_avg_time" : 2.0,
          "blocksBeingWrittenReport_num_ops" : 0.0,
          "setSafeMode_num_ops" : 0.0,
          "setReplication_num_ops" : 0.0,
          "setPermission_avg_time" : 8.33333333333,
          "setReplication_avg_time" : 6.5
        },
        "load" : {
          "load_fifteen" : 0.0233611111111,
          "load_one" : 0.155111111111,
          "load_five" : 0.106666666667
        },
        "jvm" : {
          "memHeapCommittedM" : 960.0,
          "NonHeapMemoryUsed" : 24003104,
          "logFatal" : 0,
          "threadsWaiting" : 104,
          "gcCount" : 6,
          "threadsBlocked" : 0,
          "HeapMemoryUsed" : 296185672,
          "logWarn" : 1,
          "logError" : 0,
          "HeapMemoryMax" : 1006632960,
          "memNonHeapCommittedM" : 23.375,
          "memNonHeapUsedM" : 22.891144,
          "gcTimeMillis" : 608,
          "NonHeapMemoryMax" : 136314880,
          "logInfo" : 3,
          "threadsNew" : 0,
          "memHeapUsedM" : 282.316,
          "threadsTerminated" : 0,
          "threadsTimedWaiting" : 8,
          "threadsRunnable" : 10
        },
        "memory" : {
          "mem_total" : 7514116.0,
          "swap_free" : 0.0,
          "mem_buffers" : 152297.411111,
          "mem_shared" : 0.0,
          "mem_free" : 5430989.3,
          "swap_total" : 0.0,
          "mem_cached" : 811919.0
        },
        "network" : {
          "pkts_out" : 11.18,
          "bytes_in" : 3083.69333333,
          "bytes_out" : 2018.94738889,
          "pkts_in" : 19.6518333333
        }
      },
      "component" : [
        {
          "href" : "http://your.ambari.server/api/v1/clusters/c1/services/HDFS/components/NAMENODE",
          "ServiceComponentInfo" : {
            "cluster_name" : "c1",
            "component_name" : "NAMENODE",
            "service_name" : "HDFS"
          }
        }
      ]
    }


**Schema**

    {
      "type":"object",
      "$schema": "http://json-schema.org/draft-03/schema",
      "title": "HostComponent",
      "required":true,
      "properties":{
        "HostRoles": {
          "type":"object",
          "title": "HostComponentInfo",
          "description": "Host component information.",
          "required":true,
          "properties":{
            "actual_configs": {
              "type":"object",
              "required":false
            },
            "cluster_name": {
              "type":"string",
              "title": "ClusterName",
              "description": "The associated cluster name.",
              "required":true
            },
            "component_name": {
              "type":"string",
              "title": "ComponentName",
              "description": "The associated component name.",
              "required":true
            },
            "configs": {
              "type":"object",
              "required":false
            },
            "desired_configs":{
              "type":"object",
              "title":"DesiredConfiguration",
              "required":false
            },
            "desired_stack_id": {
              "type":"string",
              "required":false
            },
            "desired_state": {
              "type":"string",
              "required":false
            },
            "host_name": {
              "type":"string",
              "title": "HostName",
              "description": "The associated host name.",
              "required":true
            },
            "stack_id": {
              "type":"string",
              "required":false
            },
            "state": {
              "type":"string",
              "title": "State",
              "description": "The host component state.",
              "required":true
            }
          }
        },
        "component": {
          "type":"array",
          "title": "Component set",
          "description": "The associated component set.",
          "minitems": "1",
          "maxitems": "1",
          "required":true,
          "items":
          {
            "type":"object",
            "title": "Component",
            "description": "The associated component.",
            "required":true,
            "properties":{
              "ServiceComponentInfo": {
                "type":"object",
                "title": "ServiceComponentInfo",
                "description": "Service component information.",
                "required":true,
                "properties":{
                  "cluster_name": {
                    "type":"string",
                    "title": "ClusterName",
                    "description": "The associated cluster name.",
                    "required":true
                  },
                  "component_name": {
                    "type":"string",
                    "title": "ComponentName",
                    "description": "The component name.",
                    "required":true
                  },
                  "service_name": {
                    "type":"string",
                    "title": "ServiceName",
                    "description": "The service name.",
                    "required":true
                  }
                }
              },
              "href": {
                "type":"string",
                "description": "This component API href.",
                "required":true
              }
            }
          }


        },
        "host": {
          "type":"object",
          "title": "Host",
          "description": "Associated host.",
          "required":true,
          "properties":{
            "href": {
              "type":"string",
              "description": "This host API href.",
              "required":true
            }
          }
        },
        "href": {
          "type":"string",
          "description": "This host component API href.",
          "required":true
        },
        "metrics":{
          "type":"object",
          "required":false
        }
      }
    }


GET configurations
----

**Example**


    GET api/v1/clusters/c1/configurations

    200 OK
    {
      "href" : "http://your.ambari.server/api/v1/clusters/c1/configurations",
      "items" : [
        {
          "href" : "http://your.ambari.server/api/v1/clusters/c1/configurations?type=mapred-site&tag=version1",
          "tag" : "version1",
          "type" : "mapred-site",
          "Config" : {
            "cluster_name" : "c1"
          }
        },
        {
          "href" : "http://your.ambari.server/api/v1/clusters/c1/configurations?type=hdfs-site&tag=version1",
          "tag" : "version1",
          "type" : "hdfs-site",
          "Config" : {
            "cluster_name" : "c1"
          }
        },
        {
          "href" : "http://your.ambari.server/api/v1/clusters/c1/configurations?type=core-site&tag=version1369851987025",
          "tag" : "version1369851987025",
          "type" : "core-site",
          "Config" : {
            "cluster_name" : "c1"
          }
        },
        {
          "href" : "http://your.ambari.server/api/v1/clusters/c1/configurations?type=global&tag=version1369851987025",
          "tag" : "version1369851987025",
          "type" : "global",
          "Config" : {
            "cluster_name" : "c1"
          }
        },
        {
          "href" : "http://your.ambari.server/api/v1/clusters/c1/configurations?type=global&tag=version1",
          "tag" : "version1",
          "type" : "global",
          "Config" : {
            "cluster_name" : "c1"
          }
        },
        {
          "href" : "http://your.ambari.server/api/v1/clusters/c1/configurations?type=core-site&tag=version1",
          "tag" : "version1",
          "type" : "core-site",
          "Config" : {
            "cluster_name" : "c1"
          }
        }
      ]
    }


**Schema**

    {
      "type":"object",
      "$schema":"http://json-schema.org/draft-03/schema",
      "title":"Configurations",
      "required":true,
      "properties":{
        "href":{
          "type":"string",
          "description":"This configurations API href.",
          "required":true
        },
        "items":{
          "type":"array",
          "title":"Configuration set",
          "required":true,
          "items":{
            "type":"object",
            "title":"Configuration",
            "required":false,
            "properties":{
              "Config":{
                "type":"object",
                "title":"ConfigInfo",
                "description":"Configuration information.",
                "required":true,
                "properties":{
                  "cluster_name":{
                    "type":"string",
                    "title":"ClusterName",
                    "description":"The associated cluster name.",
                    "required":true
                  }
                }
              },
              "href":{
                "type":"string",
                "description":"This configuration API href.",
                "required":true
              },
              "tag":{
                "type":"string",
                "required":true
              },
              "type":{
                "type":"string",
                "required":true
              }
            }
          }
        }
      }
    }

GET configuration
----

**Example**


    GET api/v1/clusters/c1/configurations?type=mapred-site&tag=version1

    200 OK
    {
      "href" : "http://your.ambari.server/api/v1/clusters/c1/configurations?type=mapred-site&tag=version1",
      "items" : [
        {
          "href" : "http://your.ambari.server/api/v1/clusters/c1/configurations?type=mapred-site&tag=version1",
          "tag" : "version1",
          "type" : "mapred-site",
          "Config" : {
            "cluster_name" : "c1"
          },
          "properties" : {
            "mapred.reduce.tasks.speculative.execution" : "false",
            "mapred.tasktracker.map.tasks.maximum" : "4",
            "mapred.hosts.exclude" : "/etc/hadoop/mapred.exclude",
            "mapreduce.tasktracker.group" : "hadoop",
            "mapred.job.reduce.input.buffer.percent" : "0.0",
            "mapreduce.reduce.input.limit" : "10737418240",
            "mapred.map.tasks.speculative.execution" : "false",
            "mapreduce.fileoutputcommitter.marksuccessfuljobs" : "false",
            "mapreduce.jobtracker.kerberos.principal" : "jt/_HOST@EXAMPLE.COM",
            "mapred.output.compression.type" : "BLOCK",
            "mapred.userlog.retain.hours" : "24",
            "mapred.job.reuse.jvm.num.tasks" : "1",
            "mapred.system.dir" : "/mapred/system",
            "mapreduce.tasktracker.keytab.file" : "/etc/security/keytabs/tt.service.keytab",
            "mapred.task.tracker.task-controller" : "org.apache.hadoop.mapred.DefaultTaskController",
            "io.sort.factor" : "100",
            "mapreduce.history.server.http.address" : "ip-10-83-74-200.ec2.internal:51111",
            "mapred.jobtracker.maxtasks.per.job" : "-1",
            "mapred.cluster.reduce.memory.mb" : "-1",
            "io.sort.spill.percent" : "0.9",
            "mapred.reduce.parallel.copies" : "30",
            "tasktracker.http.threads" : "50",
            "mapred.healthChecker.script.path" : "file:////mapred/jobstatus",
            "mapreduce.cluster.administrators" : " hadoop",
            "jetty.connector" : "org.mortbay.jetty.nio.SelectChannelConnector",
            "mapred.inmem.merge.threshold" : "1000",
            "mapred.job.reduce.memory.mb" : "-1",
            "mapred.job.map.memory.mb" : "-1",
            "mapreduce.jobhistory.kerberos.principal" : "jt/_HOST@EXAMPLE.COM",
            "mapred.cluster.map.memory.mb" : "-1",
            "mapred.jobtracker.retirejob.interval" : "21600000",
            "mapred.job.tracker.persist.jobstatus.hours" : "1",
            "mapred.cluster.max.map.memory.mb" : "-1",
            "mapred.reduce.slowstart.completed.maps" : "0.05",
            "hadoop.job.history.user.location" : "none",
            "mapred.job.tracker.handler.count" : "50",
            "mapred.healthChecker.interval" : "135000",
            "mapred.jobtracker.blacklist.fault-bucket-width" : "15",
            "mapred.task.timeout" : "600000",
            "mapred.jobtracker.taskScheduler" : "org.apache.hadoop.mapred.CapacityTaskScheduler",
            "mapred.max.tracker.blacklists" : "16",
            "mapreduce.jobhistory.keytab.file" : "/etc/security/keytabs/jt.service.keytab",
            "mapred.map.output.compression.codec" : "org.apache.hadoop.io.compress.SnappyCodec",
            "mapred.jobtracker.retirejob.check" : "10000",
            "mapred.tasktracker.tasks.sleeptime-before-sigkill" : "250",
            "mapreduce.jobtracker.staging.root.dir" : "/user",
            "mapred.job.shuffle.input.buffer.percent" : "0.7",
            "mapred.jobtracker.completeuserjobs.maximum" : "5",
            "mapred.job.tracker.persist.jobstatus.active" : "false",
            "mapred.tasktracker.reduce.tasks.maximum" : "2",
            "mapreduce.history.server.embedded" : "false",
            "mapred.job.tracker.http.address" : "ip-10-83-74-200.ec2.internal:50030",
            "mapred.queue.names" : "default",
            "mapred.job.tracker.history.completed.location" : "/mapred/history/done",
            "mapred.child.java.opts" : "-server -Xmx768m -Djava.net.preferIPv4Stack=true",
            "mapred.jobtracker.blacklist.fault-timeout-window" : "180",
            "mapreduce.jobtracker.split.metainfo.maxsize" : "50000000",
            "mapred.healthChecker.script.timeout" : "60000",
            "mapred.jobtracker.restart.recover" : "false",
            "mapreduce.jobtracker.keytab.file" : "/etc/security/keytabs/jt.service.keytab",
            "mapred.hosts" : "/etc/hadoop/mapred.include",
            "mapred.local.dir" : "/grid/0/hadoop/mapred,/grid/1/hadoop/mapred",
            "mapreduce.tasktracker.kerberos.principal" : "tt/_HOST@EXAMPLE.COM",
            "mapred.job.tracker.persist.jobstatus.dir" : "/etc/hadoop/health_check",
            "mapred.job.tracker" : "ip-10-83-74-200.ec2.internal:50300",
            "io.sort.record.percent" : ".2",
            "mapred.cluster.max.reduce.memory.mb" : "-1",
            "io.sort.mb" : "200",
            "mapred.job.shuffle.merge.percent" : "0.66",
            "mapred.child.root.logger" : "INFO,TLA"
          }
        }
      ]
    }


**Schema**

    {
      "type":"object",
      "$schema":"http://json-schema.org/draft-03/schema",
      "title":"Configuration",
      "required":true,
      "properties":{
        "href":{
          "type":"string",
          "description":"This configurations API href.",
          "required":true
        },
        "items":{
          "type":"array",
          "title":"Configuration set",
          "required":true,
          "items":{
            "type":"object",
            "title":"Configuration",
            "required":false,
            "properties":{
              "Config":{
                "type":"object",
                "title":"ConfigInfo",
                "description":"Configuration information.",
                "required":true,
                "properties":{
                  "cluster_name":{
                    "type":"string",
                    "title":"ClusterName",
                    "description":"The associated cluster name.",
                    "required":true
                  }
                }
              },
              "href":{
                "type":"string",
                "description":"This configuration API href.",
                "required":true
              },
              "properties":{
                "type":"object",
                "required":true
              },
              "tag":{
                "type":"string",
                "required":true
              },
              "type":{
                "type":"string",
                "required":true
              }
            }
          }
        }
      }
    }

    
GET request
----

**Example**

    GET /api/v1/clusters/c1/requests/2

    {
      "href" : "http://your.ambari.server/api/v1/clusters/c1/requests/2",
      "Requests" : {
        "cluster_name" : "c1",
        "id" : 2,
        "request_context" : "Start Services"
      },
      "tasks" : [
        {
          "href" : "http://your.ambari.server/api/v1/clusters/c1/requests/2/tasks/15",
          "Tasks" : {
            "cluster_name" : "c1",
            "id" : 15,
            "request_id" : 2
          }
        },
        {
          "href" : "http://your.ambari.server/api/v1/clusters/c1/requests/2/tasks/16",
          "Tasks" : {
            "cluster_name" : "c1",
            "id" : 16,
            "request_id" : 2
          }
        },
        {
          "href" : "http://your.ambari.server/api/v1/clusters/c1/requests/2/tasks/17",
          "Tasks" : {
            "cluster_name" : "c1",
            "id" : 17,
            "request_id" : 2
          }
        }
      ]
    }

**Schema**

    {
      "type":"object",
      "$schema": "http://json-schema.org/draft-03/schema",
      "title": "Request",
      "required":true,
      "properties":{
        "Requests": {
          "type":"object",
          "title": "RequestInfo",
          "description":"Request information",
          "required":true,
          "properties":{
            "cluster_name": {
              "type":"string",
              "title": "ClusterName",
              "required":true
            },
            "id": {
              "type":"number",
              "Title": "Id",
              "required":true
            },
            "request_context": {
              "type":"string",
              "title": "RequestContext",
              "required":false
            }
          }
        },
        "href": {
          "type":"string",
          "description": "This request API href",
          "required":true
        },
        "tasks": {
          "type":"array",
          "title": "Task set",
          "required":true,
          "items":
          {
            "type":"object",
            "title": "Task",
            "required":true,
            "properties":{
              "Tasks": {
                "type":"object",
                "title": "Task info",
                "required":true,
                "properties":{
                  "cluster_name": {
                    "type":"string",
                    "title": "ClusterName",
                    "required":true
                  },
                  "id": {
                    "type":"number",
                    "title": "Id",
                    "required":true
                  },
                  "request_id": {
                    "type":"number",
                    "title": "RequestId",
                    "required":true
                  }
                }
              },
              "href": {
                "type":"string",
                "title": "This task API href",
                "required":true
              }
            }
          }
        }
      }
    }
    
    
GET task
----

**Example**

    GET api/v1/clusters/c1/requests/2/tasks/15

    {
      "href" : "http://dev01.ambari.apache.org:8080/api/v1/clusters/c1/requests/2/tasks/15",
      "Tasks" : {
        "attempt_cnt" : 1,
        "cluster_name" : "c1",
        "command" : "START",
        "exit_code" : 0,
        "host_name" : "dev01.ambari.apache.org",
        "id" : 15,
        "request_id" : 2,
        "role" : "DATANODE",
        "stage_id" : 1,
        "start_time" : 1375283290257,
        "status" : "COMPLETED",
        "stderr" : "none",
        "stdout" : "notice: /Stage[2]/Hdp-hadoop::Initialize/Configgenerator::Configfile…
      }
    }
    
**Schema**

    {
      "type":"object",
      "$schema": "http://json-schema.org/draft-03/schema",
      "title": "Task",
      "required":true,
      "properties":{
        "Tasks": {
          "type":"object",
          "title": "Task Info",
          "required":true,
          "properties":{
            "attempt_cnt": {
              "type":"number",
              "title": "AttemptCount",
              "required":false
            },
            "cluster_name": {
              "type":"string",
              "title": "ClusterName",
              "required":true
            },
            "command": {
              "type":"string",
              "title": "Command",
              "required":false
            },
            "exit_code": {
              "type":"number",
              "title": "ExitCode",
              "required":false
            },
            "host_name": {
              "type":"string",
              "title": "HostName",
              "required":false
            },
            "id": {
              "type":"number",
              "title": "Id",
              "required":true
            },
            "request_id": {
              "type":"number",
              "title": "RequestId",
              "required":true
            },
            "role": {
              "type":"string",
              "title": "Role",
              "required":false
            },
            "stage_id": {
              "type":"number",
              "title": "StageId",
              "required":false
            },
            "start_time": {
              "type":"number",
              "title": "StartTime",
              "required":false
            },
            "status": {
              "type":"string",
              "title": "Status",
              "required":true
            },
            "stderr": {
              "type":"string",
              "title": "StdErr",
              "required":false
            },
            "stdout": {
              "type":"string",
              "title": "StdOut",
              "required":false
            }
          }
        },
        "href": {
          "type":"string",
          "title": "This task API href",
          "required":true
        }
      }
    }


POST/PUT/DELETE resource
----

**Example**


    PUT /clusters/c1/services/HDFS/
    {
      "ServiceInfo": {
        "state" : "STARTED"
      }
    }


    202 Accepted
    {
      "href" : "http://your.ambari.server/api/v1/clusters/c1/requests/6",
      "Requests" : {
        "id" : 6,
        "status" : "InProgress"
      }
    }

**Schema**

    {
      "type":"object",
      "$schema":"http://json-schema.org/draft-03/schema",
      "title":"AcceptedRequest",
      "required":false,
      "properties":{
        "Requests":{
          "type":"object",
          "title":"Request",
          "required":true,
          "properties":{
            "id":{
              "type":"number",
              "title":"RequestId",
              "description":"The unique id of the request.",
              "required":true
            },
            "status":{
              "type":"string",
              "title":"Status",
              "description":"The request status.",
              "required":true
            }
          }
        },
        "href":{
          "type":"string",
          "description":"This request API href.",
          "required":true
        }
      }
    }
