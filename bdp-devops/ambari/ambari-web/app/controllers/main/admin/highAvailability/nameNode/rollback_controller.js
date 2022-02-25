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
require('controllers/main/admin/highAvailability/progress_controller');

App.HighAvailabilityRollbackController = App.HighAvailabilityProgressPageController.extend({

  name: "highAvailabilityRollbackController",

  failedTask: null,
  configsSaved: false,
  deletedHdfsClients: 0,
  numOfDelOperations: 0,
  isRollback: true,
  hostsToPerformDel: [],

  commands: [
    'stopAllServices',
    'restoreHBaseConfigs',
    'restoreAccumuloConfigs',
    'restoreHawqConfigs',
    'stopFailoverControllers',
    'deleteFailoverControllers',
    'deletePXF',
    'stopStandbyNameNode',
    'stopNameNode',
    'restoreHDFSConfigs',
    'enableSecondaryNameNode',
    'stopJournalNodes',
    'deleteJournalNodes',
    'deleteAdditionalNameNode',
    'startAllServices'
  ],

  loadStep: function () {
    var self = this;
    this.initData().done(function () {
      self.clearStep();
      self.loadTasks();
      self.addObserver('tasks.@each.status', self, 'onTaskStatusChange');
      self.onTaskStatusChange();
    });
  },

  initData: function () {
    var self = this,
      dfd = $.Deferred();
    this.loadMasterComponentHosts().done(function () {
      self.loadFailedTask();
      self.loadHdfsClientHosts();
      dfd.resolve();
    });
    return dfd.promise();
  },

  setCommandsAndTasks: function(tmpTasks) {
    var fTask = this.get('failedTask');
    var commandsArray = [
      'deleteSNameNode',
      'startAllServices',
      'reconfigureHBase',
      'reconfigureAMS',
      'reconfigureAccumulo',
      'reconfigureHawq',
      'installPXF',
      'startZKFC',
      'installZKFC',
      'startSecondNameNode',
      'startNameNode',
      'startZooKeeperServers',
      'reconfigureHDFS',
      'disableSNameNode',
      'startJournalNodes',
      'installJournalNodes',
      'installNameNode',
      'stopAllServices'
    ];
    var index = commandsArray.indexOf(fTask.command);

    if(index > commandsArray.indexOf('startSecondNameNode')){
      --index;
    }
    var newCommands = this.get('commands').splice(index);
    this.set('commands', newCommands);
    var newTasks = tmpTasks.splice(index);
    for (var i = 0; i < newTasks.length; i++) {
      newTasks[i].id = i;
    }
    this.set('tasks', newTasks);
    var pxfTask = this.get('tasks').findProperty('command', 'deletePXF');
    if (!App.Service.find().someProperty('serviceName', 'PXF') && pxfTask) {
      this.get('tasks').splice(pxfTask.get('id'), 1);
    }
    var hbaseTask = this.get('tasks').findProperty('command', 'restoreHBaseConfigs');
    if (!App.Service.find().someProperty('serviceName', 'HBASE') && hbaseTask) {
      this.get('tasks').splice(hbaseTask.get('id'), 1);
    }
    var accumuloTask = this.get('tasks').findProperty('command', 'restoreAccumuloConfigs');
    if (!App.Service.find().someProperty('serviceName', 'ACCUMULO') && accumuloTask) {
      this.get('tasks').splice(accumuloTask.get('id'), 1);
    }
    var hawqTask = this.get('tasks').findProperty('command', 'restoreHawqConfigs');
    if (!App.Service.find().someProperty('serviceName', 'HAWQ') && hawqTask) {
      this.get('tasks').splice(hawqTask.get('id'), 1);
    }
  },

  clearStep: function () {
    this.set('isSubmitDisabled', true);
    this.set('tasks', []);
    this.set('logs', []);
    this.set('currentRequestIds', []);
    var commands = this.get('commands');
    var tmpTasks = [];
    for (var i = 0; i < commands.length; i++) {
      tmpTasks.pushObject(Ember.Object.create({
        title: Em.I18n.t('admin.highAvailability.rollback.task' + i + '.title'),
        status: 'PENDING',
        id: i,
        command: commands[i],
        showRetry: false,
        showRollback: false,
        showSkip: false,
        name: Em.I18n.t('admin.highAvailability.rollback.task' + i + '.title'),
        displayName: Em.I18n.t('admin.highAvailability.rollback.task' + i + '.title'),
        progress: 0,
        isRunning: false,
        hosts: []
      }));
    }
    this.setCommandsAndTasks(tmpTasks);
  },

  onTaskStatusChange: function () {
    if (!this.get('tasks').someProperty('status', 'IN_PROGRESS') && !this.get('tasks').someProperty('status', 'QUEUED') && !this.get('tasks').someProperty('status', 'FAILED')) {
      var nextTask = this.get('tasks').findProperty('status', 'PENDING');
      if (nextTask) {
        this.set('status', 'IN_PROGRESS');
        this.setTaskStatus(nextTask.get('id'), 'QUEUED');
        this.set('currentTaskId', nextTask.get('id'));
        this.runTask(nextTask.get('id'));
      } else {
        this.set('status', 'COMPLETED');
        this.set('isSubmitDisabled', false);
      }
    } else if (this.get('tasks').someProperty('status', 'FAILED')) {
      this.set('status', 'FAILED');
      this.get('tasks').findProperty('status', 'FAILED').set('showRetry', true);
      this.get('tasks').findProperty('status', 'FAILED').set('showSkip', true);
    }
    this.get('tasks').filterProperty('status','COMPLETED').setEach('showRetry', false);
    this.get('tasks').filterProperty('status','COMPLETED').setEach('showSkip', false);

    var statuses = this.get('tasks').mapProperty('status');
    var logs = this.get('tasks').mapProperty('hosts');
    var requestIds = this.get('currentRequestIds');
    this.saveTasksStatuses(statuses);
    this.saveRequestIds(requestIds);
    this.saveLogs(logs);
    App.clusterStatus.setClusterStatus({
      clusterName: this.get('content.cluster.name'),
      clusterState: 'HIGH_AVAILABILITY_ROLLBACK',
      wizardControllerName: 'highAvailabilityRollbackController',
      localdb: App.db.data
    });
  },

  skipTask: function () {
    var task = this.get('tasks').findProperty('status', 'FAILED');
    task.set('showRetry', false);
    task.set('showSkip', false);
    task.set('status', 'COMPLETED');
  },

  retryTask: function () {
    var task = this.get('tasks').findProperty('status', 'FAILED');
    task.set('showRetry', false);
    task.set('showSkip', false);
    task.set('status', 'PENDING');
  },

  onTaskCompleted: function () {
    var curTaskStatus = this.getTaskStatus(this.get('currentTaskId'));
    if (curTaskStatus != 'FAILED' && curTaskStatus != 'TIMEDOUT' && curTaskStatus != 'ABORTED') {
      this.setTaskStatus(this.get('currentTaskId'), 'COMPLETED');
    }
  },

  getTaskStatus: function (taskId) {
    return this.get('tasks').findProperty('id', taskId).get('status');
  },

  loadFailedTask: function(){
    var failedTask = App.db.getHighAvailabilityWizardFailedTask();
    this.set('failedTask', failedTask);
  },

  done: function () {
    if (!this.get('isSubmitDisabled')) {
      this.removeObserver('tasks.@each.status', this, 'onTaskStatusChange');
      this.popup.proceedOnClose();
    }
  },

  stopAllServices: function(){
    App.ajax.send({
      name: 'common.services.update',
      data: {
        context: "Stop all services",
        "ServiceInfo": {
          "state": "INSTALLED"
        }
      },
      sender: this,
      success: 'startPolling',
      error: 'onTaskError'
    });
  },
  restoreHBaseConfigs: function(){
    this.loadConfigTag("hbaseSiteTag");
    var hbaseSiteTag = this.get("content.hbaseSiteTag");
    App.ajax.send({
      name: 'admin.high_availability.load_hbase_configs',
      sender: this,
      data: {
        hbaseSiteTag: hbaseSiteTag
      },
      success: 'onLoadHbaseConfigs',
      error: 'onTaskError'
    });
  },
  restoreAccumuloConfigs: function(){
    this.loadConfigTag("accumuloSiteTag");
    var accumuloSiteTag = this.get("content.accumuloSiteTag");
    App.ajax.send({
      name: 'admin.high_availability.load_accumulo_configs',
      sender: this,
      data: {
        accumuloSiteTag: accumuloSiteTag
      },
      success: 'onLoadAccumuloConfigs',
      error: 'onTaskError'
    });
  },
  restoreHawqConfigs: function(){
    var tags = ['hawqSiteTag', 'hdfsClientTag'];
    tags.forEach(function (tagName) {
      var tag = this.get("content." + tagName);
      App.ajax.send({
        name: 'admin.high_availability.load_hawq_configs',
        sender: this,
        data: {
          tagName: tag
        },
        success: 'onLoadHawqConfigs',
        error: 'onTaskError'
      });
    }, this);
  },

  deletePXF: function(){
    var secondNameNodeHost = this.get('content.masterComponentHosts').filterProperty('component', 'NAMENODE').findProperty('isInstalled', false).mapProperty('hostName');
    var pxfComponent = this.getSlaveComponentHosts().findProperty('componentName', 'PXF');
    var dataNodeComponent = this.getSlaveComponentHosts().findProperty('componentName', 'DATANODE');

    var host, i;

    // check if PXF is already installed on the host assigned for additional NameNode
    var pxfComponentInstalled = false;
    for(i = 0; i < pxfComponent.hosts.length; i++) {
      host = pxfComponent.hosts[i];
      if (host.hostName === secondNameNodeHost) {
        pxfComponentInstalled = true;
        break;
      }
    }

    // check if DATANODE is already installed on the host assigned for additional NameNode
    var dataNodeComponentInstalled = false;
    for(i = 0; i < dataNodeComponent.hosts.length; i++) {
      host = dataNodeComponent.hosts[i];
      if (host.hostName === secondNameNodeHost) {
        dataNodeComponentInstalled = true;
        break;
      }
    }

    // if no DATANODE exists on that host, remove PXF
    if (!dataNodeComponentInstalled && pxfComponentInstalled) {
      this.updateComponent('PXF', secondNameNodeHost, "PXF", "Stop");
      this.checkBeforeDelete('PXF', secondNameNodeHost);
    }
  },

  stopFailoverControllers: function(){
    var hostNames = this.get('content.masterComponentHosts').filterProperty('component', 'NAMENODE').mapProperty('hostName');
    this.updateComponent('ZKFC', hostNames, "HDFS", "Stop");
  },
  deleteFailoverControllers: function(){
    var hostNames = this.get('content.masterComponentHosts').filterProperty('component', 'NAMENODE').mapProperty('hostName');
    this.checkBeforeDelete('ZKFC', hostNames);
  },
  stopStandbyNameNode: function(){
    var hostName = this.get('content.masterComponentHosts').filterProperty('component', 'NAMENODE').findProperty('isInstalled', false).hostName;
    this.updateComponent('NAMENODE', hostName, "HDFS", "Stop");
  },
  stopNameNode: function(){
    var hostName = this.get('content.masterComponentHosts').filterProperty('component', 'NAMENODE').findProperty('isInstalled', true).hostName;
    this.updateComponent('NAMENODE', hostName, "HDFS", "Stop");
  },
  restoreHDFSConfigs: function(){
    this.unInstallHDFSClients();
  },
  enableSecondaryNameNode: function(){
    var hostName = this.get('content.masterComponentHosts').findProperty('component', 'SECONDARY_NAMENODE').hostName;
    this.updateComponent('SECONDARY_NAMENODE', hostName, "HDFS", "Install", hostName.length);
  },
  stopJournalNodes: function(){
    var hostNames = this.get('content.masterComponentHosts').filterProperty('component', 'JOURNALNODE').mapProperty('hostName');
    this.updateComponent('JOURNALNODE', hostNames, "HDFS", "Stop");
  },
  deleteJournalNodes: function(){
    var hostNames = this.get('content.masterComponentHosts').filterProperty('component', 'JOURNALNODE').mapProperty('hostName');
    this.unInstallComponent('JOURNALNODE', hostNames);
  },
  deleteAdditionalNameNode: function(){
    var hostNames = this.get('content.masterComponentHosts').filterProperty('component', 'NAMENODE').findProperty('isInstalled', false).mapProperty('hostName');
    this.unInstallComponent('NAMENODE', hostNames);
  },
  startAllServices: function(){
    App.ajax.send({
      name: 'common.services.update',
      data: {
        context: "Start all services",
        "ServiceInfo": {
          "state": "STARTED"
        }
      },
      sender: this,
      success: 'startPolling',
      error: 'onTaskError'
    });
  },

  onLoadHbaseConfigs: function (data) {
    var hbaseSiteProperties = data.items.findProperty('type', 'hbase-site').properties;
    App.ajax.send({
      name: 'admin.save_configs',
      sender: this,
      data: {
        siteName: 'hbase-site',
        properties: hbaseSiteProperties
      },
      success: 'onTaskCompleted',
      error: 'onTaskError'
    });
  },
  onLoadAccumuloConfigs: function (data) {
    var accumuloSiteProperties = data.items.findProperty('type', 'accumulo-site').properties;
    App.ajax.send({
      name: 'admin.save_configs',
      sender: this,
      data: {
        siteName: 'accumulo-site',
        properties: accumuloSiteProperties
      },
      success: 'onTaskCompleted',
      error: 'onTaskError'
    });
  },

  onLoadHawqConfigs: function (data) {
    var hawqSiteProperties = data.items.findProperty('type', 'hawq-site').properties;
    App.ajax.send({
      name: 'admin.save_configs',
      sender: this,
      data: {
        siteName: 'hawq-site',
        properties: hawqSiteProperties
      },
      success: 'onTaskCompleted',
      error: 'onTaskError'
    });
  },
  onDeletedHDFSClient: function () {
    var deletedHdfsClients = this.get('deletedHdfsClients');
    var hostName = this.get("content.hdfsClientHostNames");
    var notDeletedHdfsClients = hostName.length - deletedHdfsClients;
    if (notDeletedHdfsClients > 1 && hostName.length != 1 ) {
      this.set('deletedHdfsClients', deletedHdfsClients+1);
      return;
    }
    this.loadConfigTag("hdfsSiteTag");
    this.loadConfigTag("coreSiteTag");
    var hdfsSiteTag = this.get("content.hdfsSiteTag");
    var coreSiteTag = this.get("content.coreSiteTag");
    App.ajax.send({
      name: 'admin.high_availability.load_configs',
      sender: this,
      data: {
        hdfsSiteTag: hdfsSiteTag,
        coreSiteTag: coreSiteTag
      },
      success: 'onLoadConfigs',
      error: 'onTaskError'
    });
  },

  onLoadConfigs: function (data) {
    this.set('configsSaved', false);
    App.ajax.send({
      name: 'admin.save_configs',
      sender: this,
      data: {
        siteName: 'hdfs-site',
        properties: data.items.findProperty('type', 'hdfs-site').properties
      },
      success: 'onHdfsConfigsSaved',
      error: 'onTaskError'
    });
    App.ajax.send({
      name: 'admin.save_configs',
      sender: this,
      data: {
        siteName: 'core-site',
        properties: data.items.findProperty('type', 'core-site').properties
      },
      success: 'onHdfsConfigsSaved',
      error: 'onTaskError'
    });
  },

  onHdfsConfigsSaved: function () {
    if (!this.get('configsSaved')) {
      this.set('configsSaved', true);
      return;
    }
    this.onTaskCompleted();
  },

  unInstallHDFSClients: function () {
    var hostName = this.get("content.hdfsClientHostNames");
    for (var i = 0; i < hostName.length; i++) {
      App.ajax.send({
        name: 'common.delete.host_component',
        sender: this,
        data: {
          componentName: 'HDFS_CLIENT',
          hostName: hostName[i]
        },
        success: 'onDeletedHDFSClient',
        error: 'onTaskError'
      });
    }
  },

  unInstallComponent: function (componentName, hostName) {
    if (!(hostName instanceof Array)) {
      hostName = [hostName];
    }
    for (var i = 0; i < hostName.length; i++) {
      App.ajax.send({
        name: 'common.host.host_component.passive',
        sender: this,
        data: {
          hostName: hostName[i],
          componentName: componentName,
          passive_state: "ON",
          taskNum: hostName.length,
          callback: 'checkBeforeDelete'
        },
        success: 'checkResult',
        error: 'checkResult'
      });
    }
  },

  checkBeforeDelete: function (componentName, hostName){
    this.set('hostsToPerformDel', []);
    if (!(hostName instanceof Array)) {
      hostName = [hostName];
    }
    for (var i = 0; i < hostName.length; i++) {
      App.ajax.send({
        name: 'admin.high_availability.getHostComponent',
        sender: this,
        data: {
          componentName: componentName,
          hostName: hostName[i],
          taskNum: hostName.length,
          callback: 'deleteComponent'
        },
        success: 'checkResult',
        error: 'checkResult'
      });
    }
  },

  checkResult: function () {
    var callback = arguments[2].callback;
    var hostName = arguments[2].hostName;
    var componentName = arguments[2].componentName;
    var taskNum = arguments[2].taskNum;
    var hostsToPerformDel = this.get('hostsToPerformDel');
    if(arguments[1] != 'error'){
      hostsToPerformDel.push({
        hostName: hostName,
        isOnHost: true
      });
    }else{
      hostsToPerformDel.push({
        hostName: 'error',
        isOnHost: false
      });
    }
    if(hostsToPerformDel.length == taskNum){
      var hostsForDel = hostsToPerformDel.filterProperty('isOnHost', true).mapProperty('hostName');
      this.set('hostsToPerformDel', []);
      if(hostsForDel.length == 0){
        this.onTaskCompleted();
        return;
      }
      this[callback](componentName, hostsForDel);
    }
  },

  deleteComponent: function (componentName, hostName) {
    if (!(hostName instanceof Array)) {
      hostName = [hostName];
    }
    this.set('numOfDelOperations', hostName.length);
    for (var i = 0; i < hostName.length; i++) {
      App.ajax.send({
        name: 'common.delete.host_component',
        sender: this,
        data: {
          componentName: componentName,
          hostName: hostName[i]
        },
        success: 'onDeleteComplete',
        error: 'onTaskError'
      });
    }
  },

  onDeleteComplete: function () {
    var leftOp = this.get('numOfDelOperations');
    if(leftOp > 1){
      this.set('numOfDelOperations', leftOp-1);
      return;
    }
    this.onTaskCompleted();
  }

});
