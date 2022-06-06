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

var App = require('app');

App.HighAvailabilityWizardStep7Controller = App.HighAvailabilityProgressPageController.extend({

  name:"highAvailabilityWizardStep7Controller",

  commands: ['startZooKeeperServers', 'startAmbariInfra', 'startMysqlServer', 'startRanger', 'startNameNode'],

  initializeTasks: function () {
    this._super();
    const tasksToRemove = [];

    if (!App.Service.find('AMBARI_INFRA_SOLR').get('isLoaded')) {
      tasksToRemove.push('startAmbariInfra');
    }

    if (App.ClientComponent.getModelByComponentName('RANGER_ADMIN').get('installedCount') === 0) {
      tasksToRemove.push('startRanger');
    }

    if (App.ClientComponent.getModelByComponentName('MYSQL_SERVER').get('installedCount') === 0) {
      tasksToRemove.push('startMysqlServer');
    }

    this.removeTasks(tasksToRemove);
  },

  startAmbariInfra: function () {
    this.startServices(false, ['AMBARI_INFRA_SOLR'], true);
  },

  startRanger: function () {
    var hostNames = this.get('content.masterComponentHosts').filterProperty('component', 'RANGER_ADMIN').mapProperty('hostName');
    if(hostNames.length) {
      this.updateComponent('RANGER_ADMIN', hostNames, "RANGER", "Start");
    }
  },

  startMysqlServer: function () {
    const hostNames = App.MasterComponent.find('MYSQL_SERVER').get('hostNames');
    this.updateComponent('MYSQL_SERVER', hostNames, "HIVE", "Start");
  },

  startZooKeeperServers: function () {
    var hostNames = this.get('content.masterComponentHosts').filterProperty('component', 'ZOOKEEPER_SERVER').mapProperty('hostName');
    this.updateComponent('ZOOKEEPER_SERVER', hostNames, "ZOOKEEPER", "Start");
  },

  startNameNode: function () {
    var hostName = this.get('content.masterComponentHosts').filterProperty('component', 'NAMENODE').findProperty('isInstalled', true).hostName;
    this.updateComponent('NAMENODE', hostName, "HDFS", "Start");
  }
});
