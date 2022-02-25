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
# Alerts
Alert instances are divided into two different endpoints. There's one endpoint for history where all of the state changes for a definition are recorded. Another endpoint is used for the current state of all alert instances in the cluster.

Historical events are created when an alert's state changes. As alerts are collected from Ambari agents, timestamp and text is updated on the most recent alert instance.

## Current Alerts
Alert instances are read-only. They support the full predicate, sorting, and pagination syntax of the Ambari REST API.

#### Querying
Current alerts can be exposed on the following resource endpoints:

##### Cluster Request
    GET api/v1/clusters/<cluster>/alerts
    
##### Service Request
    GET api/v1/clusters/<cluster>/services/<service>/alerts

##### Host Request
    GET api/v1/clusters/<cluster>/hosts/<host>/alerts
    
##### Alert Response
    {
      "href" : "http://<server>/api/v1/clusters/<cluster>/alerts?fields=*",
      "items" : [
        {
          "href" : "http://<server>/api/v1/clusters/<cluster>/alerts/2",
          "Alert" : {
            "cluster_name" : "<cluster>",
            "component_name" : "AMBARI_AGENT",
            "definition_id" : 39,
            "definition_name" : "ambari_agent_disk_usage",
            "host_name" : "<host>",
            "id" : 2,
            "instance" : null,
            "label" : "Host Disk Usage",
            "latest_timestamp" : 1425704842163,
            "maintenance_state" : "OFF",
            "original_timestamp" : 1425600467615,
            "scope" : "HOST",
            "service_name" : "AMBARI",
            "state" : "OK",
            "text" : "Capacity Used: [0.74%, 3.9 GB], Capacity Total: [525.3 GB]"
          }
        },
        ...
      ]
    }
    


#### Advanced Queries

##### Data Formatters
* Summary - provides a count of all alert states, including how many are in maintenance mode. 
* Grouped Summary - provides a count of all alert states grouped by definition, including how many are in maintenance mode. Each definition group will also contain the most recent text for each state.

###### Summary Request
    GET api/v1/clusters/<cluster>/alerts?format=summary

###### Summary Response
    {
      "href" : "http://<server>/api/v1/clusters/<cluster>/alerts?format=summary",
      "alerts_summary" : {
        "OK" : {
          "count" : 66,
          "original_timestamp" : 1425699364098,
          "maintenance_count" : 0
        },
        "WARNING" : {
          "count" : 0,
          "original_timestamp" : 0,
          "maintenance_count" : 0
        },
        "CRITICAL" : {
          "count" : 0,
          "original_timestamp" : 0,
          "maintenance_count" : 0
        },
        "UNKNOWN" : {
          "count" : 0,
          "original_timestamp" : 0,
          "maintenance_count" : 0
        }
      }
    }

###### Grouped Summary Request
    GET api/v1/clusters/<cluster>/alerts?format=groupedSummary

###### Grouped Summary Response
    {
      "href" : "http://<server>/api/v1/clusters/<cluster>/alerts?format=groupedSummary",
      "alerts_summary_grouped" : [
        {
          "definition_id" : 30,
          "definition_name" : "yarn_resourcemanager_webui",
          "summary" : {
            "OK" : {
              "count" : 1,
              "original_timestamp" : 1425603503227,
              "maintenance_count" : 0,
              "latest_text" : "HTTP 200 response in 0.000 seconds"
            },
            "WARNING" : {
              "count" : 0,
              "original_timestamp" : 0,
              "maintenance_count" : 0
            },
            "CRITICAL" : {
              "count" : 0,
              "original_timestamp" : 0,
              "maintenance_count" : 0
            },
            "UNKNOWN" : {
              "count" : 0,
              "original_timestamp" : 0,
              "maintenance_count" : 0
            }
          }
        },
        ...
      ]
    }

##### All `CRITICAL` and `WARNING` Alerts, Sorted by State
    GET api/v1/clusters/<cluster>/alerts?fields=*&Alert/state.in(WARNING,CRITICAL)&sortBy=Alert/state


## Alert History
Historical entries are read-only. They support the full predicate, sorting, and pagination syntax of the Ambari REST API.

#### Querying
History can be exposed on the following resource endpoints:

##### Cluster Request
    GET api/v1/clusters/<cluster>/alert_history
    
##### Service Request
    GET api/v1/clusters/<cluster>/services/<service>/alert_history

##### Host Request
    GET api/v1/clusters/<cluster>/hosts/<host>/alert_history

##### Response
    {
      "href" : "http://<server>/api/v1/clusters/<cluster>/alert_history?fields=*",
      "items" : [
        {
          "href" : "http://<server>/api/v1/clusters/<cluster>/alert_history/159",
          "AlertHistory" : {
            "cluster_name" : "<cluster>",
            "component_name" : "ZOOKEEPER_SERVER",
            "definition_id" : 37,
            "definition_name" : "zookeeper_server_process",
            "host_name" : "c6403.ambari.apache.org",
            "id" : 159,
            "instance" : null,
            "label" : "ZooKeeper Server Process",
            "service_name" : "ZOOKEEPER",
            "state" : "OK",
            "text" : "TCP OK - 0.000s response on port 2181",
            "timestamp" : 1425603185086
          }
        },
        ...
      ]
    }

#### Advanced Queries
##### `WARNING` alerts, 100 per page, starting at 1
    GET api/v1/clusters/<cluster>/alert_history?(AlertHistory/state=WARNING)&fields=*&from=start&page_size=100

##### All alerting events for HDFS and YARN
    GET api/v1/clusters/c1/alert_history?(AlertHistory/service_name.in(HDFS,YARN))
        
