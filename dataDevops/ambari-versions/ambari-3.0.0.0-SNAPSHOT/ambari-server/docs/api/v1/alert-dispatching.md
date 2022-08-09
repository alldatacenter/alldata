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
# Alert Dispatching

### Concepts

##### Groups
Alert definitions can be combined into groups. There is a default group for each service deployed in Ambari. These default groups contain each of the definitions that exist for that service. When a new definition is created for a given service, it is automatically added to the default group for that service.

To create custom groupings, any number of definitions from any service can be mixed together.

##### Targets
Targets are used to notify an interested party that an alert instance has changed state. There are currently two supported target types:

* Email (SMTP)
* Simple Network Management Protocol (SNMP)
    * SNMPv1
    * SNMPv2c
    * SNMPv3
    
Any number of targets can be associated with an alert group (a grouping of definitions). When a definition in the group changes state, all associated targets will be notified. 

There is also a `global` alert target which can be used as a way to produce notifications for any alert defined in Ambari. These `global` targets are never associated with any groups; they apply to all groups and all definitions.

##### Notices
Once an alert has changed state, Ambari will determine if there are any targets that should receive a notification of the state change. These notifications, or notices, are created with an initial status of `PENDING` and will transition either to `DELIVERED` or `FAILED` once the proper dispatcher has attemped to process the notice. Dispatchers can attempt to aggregate any `PENDING` notices into a single outbound notification.

### API Summary

#### Groups

##### Create Request
A group can be created with just a name and then updated later with any of its valid properties. 

    POST api/v1/clusters/<cluster>/alert_groups
    
    {
      "AlertGroup": {
        "name": "Demo Group"
      }
    }

Or it can be created with all valid properties. In this example, a group is being associated with 2 existing targets and 3 existing definitions.

    POST api/v1/clusters/<cluster>/alert_groups

    {
      "AlertGroup": {
        "name": "Demo Group",
        "targets": [1, 2],
        "definitions": [7, 9, 14]
      }
    }


##### Update Request
    PUT api/v1/clusters/<cluster>/alert_groups/<group-id>

    {
      "AlertGroup": {
        "name": "Demo Group",
        "targets": [1, 2, 3],
        "definitions": [7, 9, 14, 21, 28, 29, 30]
      }
    }
    
##### Delete Request
    DELETE api/v1/clusters/<cluster>/alert_groups/<group-id>
    
##### Query Request
    GET api/v1/clusters/<cluster>/alert_groups

##### Query Response
    {
      "href" : "http://<server>/api/v1/clusters/<cluster>/alert_groups",
      "items" : [
        {
          "href" : "http://<server>/api/v1/clusters/<cluster>/alert_groups/1",
          "AlertGroup" : {
            "cluster_name" : "<cluster>",
            "id" : 1,
            "name" : "TEZ"
          }
        },
        {
          "href" : "http://<server>/api/v1/clusters/<cluster>/alert_groups/2",
          "AlertGroup" : {
            "cluster_name" : "<cluster>",
            "id" : 2,
            "name" : "MAPREDUCE2"
          }
        },
        ...
      ]
    }

##### Query Request
    GET api/v1/clusters/<cluster>/alert_groups/<group-id>

##### Query Response
Each group contains information about the definitions and targets associated with it.

    {
      "href" : "http://<server>/api/v1/clusters/<cluster>/alert_groups/2?fields=*",
      "AlertGroup" : {
        "cluster_name" : "<cluster>",
        "default" : true,
        "definitions" : [
          {
            "name" : "mapreduce_history_server_webui",
            "label" : "History Server Web UI",
            "description" : "This host-level alert is triggered if the History Server Web UI is unreachable.",
            "enabled" : true,
            "service_name" : "MAPREDUCE2",
            "component_name" : "HISTORYSERVER",
            "id" : 1,
            "source_type" : "WEB"
          },
          ...
        ],
        "id" : 3,
        "name" : "MAPREDUCE2",
        "targets" : [
          {
            "name" : "Administrators",
            "id" : 1,
            "description" : "The Admins",
            "global" : false,
            "notification_type" : "EMAIL"
          }
        ]
      }
    }


#### Targets
Each target type has its own set of properties that are used to configure the backend dispatcher. These properties are typically not shared between targets except for the following global properties:

* ambari.dispatch.credential.username - an optional property for specifying the username needed for the dispatcher in order to relay the notification.
* ambari.dispatch.credential.password - an optional property for specifying the password needed for the dispatcher in order to relay the notification.
* ambari.dispatch.recipients - an array of strings representing each recipient to receive an alert notification. This is a free-form string that the backend dispatcher can understand. For example, with an `EMAIL` target, this would be a list of email addresses.

The following is a list of target types and the valid properties that they accept:

* Email - any JavaMail property
* SNMP
    * ambari.dispatch.snmp.oids.body
    * ambari.dispatch.snmp.oids.subject
    * ambari.dispatch.snmp.oids.trap
    * ambari.dispatch.snmp.port
    * ambari.dispatch.snmp.version
    * ambari.dispatch.snmp.community
    * ambari.dispatch.snmp.security.username
    * ambari.dispatch.snmp.security.auth.passphrase
    * ambari.dispatch.snmp.security.priv.passphrase
    * ambari.dispatch.snmp.security.level


##### Create Request
In this example, several JavaMail properties are specified in order for this target to connect and authenticate with the SMTP relay. If the target was not global, any existing groups could be included as well when creating the target.

    POST api/v1/alert_targets
    
    {
      "AlertTarget": {
        "name": "Administrators",
        "description": "The Admins",
        "notification_type": "EMAIL",
        "global": true,
        "properties":{
          "ambari.dispatch.credential.username":"ambari",
          "ambari.dispatch.credential.password":"password",
          "ambari.dispatch.recipients":["ambari@relay.ambari.apache.org"],
          "mail.smtp.host":"relay.ambari.apache.org",
          "mail.smtp.port":"25",
          "mail.smtp.auth":"true",
          "mail.smtp.starttls.enable":"false",
          "mail.smtp.from":"ambari@relay.ambari.apache.org"
        }
      }
    }

##### Update Request
Alert targets can be updated with a partial body. Properties which are not specified are assumed to not change. 

    PUT api/v1/alert_targets/<target-id>
    
    {
      "AlertTarget": {
        "alert_states": ["OK", "WARNING"]
      }
    }

##### Delete Request
    DELETE api/v1/alert_targets/<target-id>
        
##### Query Request
Targets are not associated with any aspect of a cluster. Therefore their endpoint is defined without any cluster scoping. This allows the same target to be reused for other clusters managed by Ambari.

    GET api/v1/alert_targets

##### Query Response
The target from the query has the following attributes:

* It is associated with the HDFS alert group
* It will trigger for any of the alert states
<!-- --> 

    GET api/v1/alert_targets
    
    {
      "href" : "http://<server>/api/v1/alert_targets?fields=*",
      "items" : [
        {
          "href" : "http://<server>/api/v1/alert_targets/1",
          "AlertTarget" : {
            "alert_states" : [
              "OK",
              "CRITICAL",
              "UNKNOWN",
              "WARNING"
            ],
            "description" : "The Admins",
            "global" : false,
            "groups" : [
              {
                "name" : "HDFS",
                "id" : 3,
                "default" : true,
                "cluster_id" : 2
              }
            ],
            "id" : 1,
            "name" : "Administrators",
            "notification_type" : "EMAIL",
            "properties" : {
              "mail.smtp.from" : "ambari@relay.ambari.apache.org",
              "ambari.dispatch.credential.username" : "ambari",
              "mail.smtp.host" : "relay.ambari.apache.org",
              "mail.smtp.port" : "25",
              "mail.smtp.auth" : "true",
              "ambari.dispatch.credential.password" : "password",
              "ambari.dispatch.recipients" : [
                "ambari@relay.ambari.apache.org"
              ],
              "mail.smtp.starttls.enable" : "false"
            }
          }
        }
      ]
    }
    
#### Validation
Before a target is created, it is possible to have Ambari validate whether the properties being specified are correct. Ambari will attempt to execise the target and return a response with the validation information.

###### Request
Use the `validate_config` directive to have Ambari connect to the backend dispatcher in order to verify the the target's properties are valid.

    POST api/v1/alert_targets?validate_config=true
    
    {
      "AlertTarget": {
        "name": "Administrators",
        "description": "The Admins",
        "notification_type": "EMAIL",
        "global": true,
        "properties":{
          "ambari.dispatch.credential.username":"ambari",
          "ambari.dispatch.credential.password":"password",
          "ambari.dispatch.recipients":["ambari@invalid.ambari.apache.org"],
          "mail.smtp.host":"invalid.ambari.apache.org",
          "mail.smtp.port":"25",
          "mail.smtp.auth":"true",
          "mail.smtp.starttls.enable":"false",
          "mail.smtp.from":"ambari@invalid.ambari.apache.org"
        }
      }
    }    
    
###### Response
    HTTP 400 Bad Request
    
    {
      "status" : 400,
      "message" : "Invalid config: Couldn't connect to host, port: invalid.ambari.apache.org, 25; timeout -1"
    }    

#### Notices
Notices are read-only and are produced by Ambari in response to alerting events. They are used to maintain an audit trail of the outbound notifications and whether they were successfully delivered to the backend dispatcher.

##### Request
    GET api/v1/clusters/<cluster>/alert_notices
    
##### Response
    {
      "href" : "http://<server>/api/v1/clusters/<server>/alert_notices?fields=*",
      "items" : [
        {
          "href" : "http://<server>/api/v1/clusters/<cluster>/alert_notices/1",
          "AlertNotice" : {
            "cluster_name" : "<cluster>",
            "history_id" : 227,
            "id" : 1,
            "notification_state" : "DELIVERED",
            "service_name" : "ZOOKEEPER",
            "target_id" : 3,
            "target_name" : "Administrators",
            "uuid" : "fef28294-bb3a-4186-b62c-1a060fa75927"
          }
        }
      ]
    }
