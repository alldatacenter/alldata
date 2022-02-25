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

# Upgrade Resources
Upgrade resources are used to manage and query the state of any rolling
upgrade that is in progress.

### Concept
An upgrade consists of 4 distinct object types.

#### Upgrade
An upgrade is the top-level object that maps to a Request.

#### UpgradeGroup
An upgrade group is a collection of UpgradeItem instances that are related.  For example, "Core Masters" can be a group that contains the master services for HDFS, HBase and YARN.  There are many groups associated with an Upgrade.  Groups do not match an existing Requst/Stage/Task object.

#### UpgradeItem
An upgrade item matches the framework Stage instance.  UpgradeItems must be processed sequentially.  There are several items associated to a group.

#### Task
A task is the same Task created for normal Request/Stage.  It represents a singular command that is run on an agent.  Tasks may run in parallel so long as they are for different hosts.  For example, restarting a Datanode may happen on two different hosts simultaneously.  Many tasks may be associated to a single UpgradeItem.

A sample hierarchy is as follows:

    Upgrade
    |-UpgradeGroup - "Core Masters"
    | |-UpgradeItem - "Prepare Namenode"
    | | |-Task - message task
    | |-UpgradeItem - "Restart Namenode"
    |   |-Task - restart task
    |   |-Task - execution task
    |-UpgradeGroup



### API Summary

##### Create Upgrade
Initiates the upgrade process.  Uses the repository version specfied and creates the groups, items and tasks.  You must have the correct version from [Repository Versions](repository-version-resources.md)
###### Request:

    POST http://<server>:8080/api/v1/clusters/<cluster>/upgrades
    {
      "Upgrade": {
        "repository_version": "2.2.2.0-1234"
      }
    }
    
###### Response
    {
      "resources" : [
        {
          "href" : "http://<server>:8080/api/v1/clusters/<cluster>/upgrades/27",
          "Upgrade" : {
            "request_id" : 27
          }
        }
      ]
    }

##### Get Upgrade
Numerous properties and information can be retrieved from the upgrade id.  Sample output:

    {
      "Upgrade" : {
        "cluster_name" : "<cluster>",
        "request_id": 27
      },
      "upgrade_groups" : [
        {
          "UpgradeGroup" : {
            "group_id": 29,
            "progress_percent" : 0.0,
            "title" : "ZooKeeper"
            },
            "upgrade_items" : [
              {
                "UpgradeItem" : {
                  "stage_id": 1,
                  "context" : "Preparing ZooKeeper Client on 2 hosts",
                  "status" : "HOLDING"
                }
              },
              {
                "UpgradeItem" : {
                  "stage_id": 2,
                  "context" : "Restarting ZooKeeper Client on host3",
                  "status" : "PENDING"
                }
              },...
            ]
          }
        ]
      }

##### Manual Items Processing
In the example above, note the UpgradeItem step that is in a HOLDING status.  This means that in the course of an upgrade, a Manual Task was generated.  Manual Tasks are used to show a message to the end user, and must be continued by issuing the status to the UpgradeItem:

###### Request
    PUT http://<server>:8080/api/v1/clusters/<cluster>/upgrades/27/upgrade_groups/29/upgrade_items/1
    {
      "UpgradeItem": { 
        "status": "COMPLETED" 
      } 
    }
###### Response
The response for the above call does not return a body.  It returns the status code 200 on completion.  After the call is made, the next UpgradeItem status becomes QUEUED and eventually IN_PROGRESS when it scheduled for agent execution.


###### Other HOLDING Statuses
In addition to HOLDING, there are two other statuses that may pause execution

* HOLDING_FAILED is used to indicate that the task has failed.  The UpgradeItem can either be retried with PENDING, or skipped with FAILED.  Most tasks for an Upgrade are retry-able and skippable.  However, in the event that the UpgradeItem is not marked as skippable, marking as FAILED will abort the entire request.
* HOLDING_TIMEDOUT is similar to HOLDING_FAILED, except it can be transition to PENDING (retry) or TIMEDOUT.

Either can transition to ABORTED to abort the entire request.

