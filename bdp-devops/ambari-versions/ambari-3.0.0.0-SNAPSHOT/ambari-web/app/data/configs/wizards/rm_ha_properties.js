/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

module.exports =
{
  "haConfig": {
    serviceName: 'MISC',
    displayName: 'MISC',
    configCategories: [
      App.ServiceConfigCategory.create({ name: 'YARN', displayName: 'YARN'}),
      App.ServiceConfigCategory.create({ name: 'HAWQ', displayName: 'HAWQ'}),
      App.ServiceConfigCategory.create({ name: 'HDFS', displayName: 'HDFS'})
    ],
    sites: ['yarn-site', 'hawq-site', 'core-site'],
    configs: [
    /**********************************************HDFS***************************************/
      {
        "name": "yarn.resourcemanager.ha.enabled",
        "displayName": "yarn.resourcemanager.ha.enabled",
        "isReconfigurable": false,
        "recommendedValue": true,
        "isOverridable": false,
        "value": true,
        "displayType": "checkbox",
        "category": "YARN",
        "filename": "yarn-site",
        serviceName: 'MISC'
      },
      {
        "name": "yarn.resourcemanager.ha.rm-ids",
        "displayName": "yarn.resourcemanager.ha.rm-ids",
        "isReconfigurable": false,
        "recommendedValue": "rm1,rm2",
        "isOverridable": false,
        "value": "rm1,rm2",
        "category": "YARN",
        "filename": "yarn-site",
        serviceName: 'MISC'
      },
      {
        "name": "yarn.resourcemanager.hostname.rm1",
        "displayName": "yarn.resourcemanager.hostname.rm1",
        "isReconfigurable": false,
        "recommendedValue": "",
        "isOverridable": false,
        "value": "",
        "category": "YARN",
        "filename": "yarn-site",
        serviceName: 'MISC'
      },
      {
        "name": "yarn.resourcemanager.resource-tracker.address.rm1",
        "displayName": "yarn.resourcemanager.resource-tracker.address.rm1",
        "isReconfigurable": false,
        "recommendedValue": "",
        "isOverridable": false,
        "value": "",
        "category": "YARN",
        "filename": "yarn-site",
        serviceName: 'MISC'
      },
      {
        "name": "yarn.resourcemanager.resource-tracker.address.rm2",
        "displayName": "yarn.resourcemanager.resource-tracker.address.rm2",
        "isReconfigurable": false,
        "recommendedValue": "",
        "isOverridable": false,
        "value": "",
        "category": "YARN",
        "filename": "yarn-site",
        serviceName: 'MISC'
      },
      {
        "name": "yarn.resourcemanager.webapp.address.rm1",
        "displayName": "yarn.resourcemanager.webapp.address.rm1",
        "isReconfigurable": false,
        "recommendedValue": "",
        "isOverridable": false,
        "value": "",
        "category": "YARN",
        "filename": "yarn-site",
        serviceName: 'MISC'
      },
      {
        "name": "yarn.resourcemanager.webapp.address.rm2",
        "displayName": "yarn.resourcemanager.webapp.address.rm2",
        "isReconfigurable": false,
        "recommendedValue": "",
        "isOverridable": false,
        "value": "",
        "category": "YARN",
        "filename": "yarn-site",
        serviceName: 'MISC'
      },
      {
        "name": "yarn.resourcemanager.webapp.https.address.rm1",
        "displayName": "yarn.resourcemanager.webapp.https.address.rm1",
        "isReconfigurable": false,
        "recommendedValue": "",
        "isOverridable": false,
        "value": "",
        "category": "YARN",
        "filename": "yarn-site",
        serviceName: 'MISC'
      },
      {
        "name": "yarn.resourcemanager.webapp.https.address.rm2",
        "displayName": "yarn.resourcemanager.webapp.https.address.rm2",
        "isReconfigurable": false,
        "recommendedValue": "",
        "isOverridable": false,
        "value": "",
        "category": "YARN",
        "filename": "yarn-site",
        serviceName: 'MISC'
      },

      {
        "name": "yarn.resourcemanager.hostname.rm2",
        "displayName": "yarn.resourcemanager.hostname.rm2",
        "isReconfigurable": false,
        "recommendedValue": "",
        "isOverridable": false,
        "value": "",
        "category": "YARN",
        "filename": "yarn-site",
        serviceName: 'MISC'
      },
      {
        "name": "yarn.resourcemanager.recovery.enabled",
        "displayName": "yarn.resourcemanager.recovery.enabled",
        "isReconfigurable": false,
        "recommendedValue": true,
        "isOverridable": false,
        "value": true,
        "displayType": "checkbox",
        "category": "YARN",
        "filename": "yarn-site",
        serviceName: 'MISC'
      },
      {
        "name": "yarn.resourcemanager.store.class",
        "displayName": "yarn.resourcemanager.store.class",
        "isReconfigurable": false,
        "recommendedValue": "org.apache.hadoop.yarn.server.resourcemanager.recovery.ZKRMStateStore",
        "isOverridable": false,
        "value": "org.apache.hadoop.yarn.server.resourcemanager.recovery.ZKRMStateStore",
        "category": "YARN",
        "filename": "yarn-site",
        serviceName: 'MISC'
      },
      {
        "name": "yarn.resourcemanager.zk-address",
        "displayName": "yarn.resourcemanager.zk-address",
        "isReconfigurable": false,
        "recommendedValue": "",
        "isOverridable": false,
        "value": "",
        "category": "YARN",
        "filename": "yarn-site",
        serviceName: 'MISC'
      },
      {
        "name": "yarn.resourcemanager.cluster-id",
        "displayName": "yarn.resourcemanager.cluster-id",
        "isReconfigurable": false,
        "recommendedValue": "yarn-cluster",
        "isOverridable": false,
        "value": "yarn-cluster",
        "category": "YARN",
        "filename": "yarn-site",
        serviceName: 'MISC'
      },
      {
        "name": "yarn.resourcemanager.ha.automatic-failover.zk-base-path",
        "displayName": "yarn.resourcemanager.ha.automatic-failover.zk-base-path",
        "isReconfigurable": false,
        "recommendedValue": "/yarn-leader-election",
        "isOverridable": false,
        "value": "/yarn-leader-election",
        "category": "YARN",
        "filename": "yarn-site",
        serviceName: 'MISC'
      },
    /**********************************************HAWQ***************************************/
      {
        "name": "yarn.resourcemanager.ha",
        "displayName": "yarn.resourcemanager.ha",
        "description": "Comma separated yarn resourcemanager host addresses with port",
        "isReconfigurable": false,
        "recommendedValue": "",
        "isOverridable": false,
        "value": "",
        "category": "HAWQ",
        "filename": "yarn-client",
        serviceName: 'MISC'
      },
      {
        "name": "yarn.resourcemanager.scheduler.ha",
        "displayName": "yarn.resourcemanager.scheduler.ha",
        "description": "Comma separated yarn resourcemanager scheduler addresses with port",
        "isReconfigurable": false,
        "recommendedValue": "",
        "isOverridable": false,
        "value": "",
        "category": "HAWQ",
        "filename": "yarn-client",
        serviceName: 'MISC'
      }
    ]
  }
};
