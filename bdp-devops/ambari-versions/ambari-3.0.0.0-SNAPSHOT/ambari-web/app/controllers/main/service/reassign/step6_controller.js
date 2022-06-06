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

App.ReassignMasterWizardStep6Controller = App.HighAvailabilityProgressPageController.extend(App.WizardEnableDone, {

  name: "reassignMasterWizardStep6Controller",

  commands: [
    'stopMysqlService',
    'putHostComponentsInMaintenanceMode',
    'stopHostComponentsInMaintenanceMode',
    'deleteHostComponents',
    'startDatanodes',
    'startAllServices'
  ],

  clusterDeployState: 'REASSIGN_MASTER_INSTALLING',

  multiTaskCounter: 0,

  hostComponents: [],

  loadStep: function () {
    if (this.get('content.reassign.component_name') === 'NAMENODE' && App.get('isHaEnabled')) {
      this.set('hostComponents', ['NAMENODE', 'ZKFC']);
    } else {
      this.set('hostComponents', [this.get('content.reassign.component_name')]);
    }

    if (App.Service.find().someProperty('serviceName', 'PXF') && this.get('content.reassign.component_name') === 'NAMENODE') {
      var pxfHosts = App.HostComponent.find().filterProperty('componentName', 'PXF').mapProperty('hostName');
      var dataNodeHosts = App.HostComponent.find().filterProperty('componentName', 'DATANODE').mapProperty('hostName');

      // If NAMENODE is being moved and source host does not have DATANODE, PXF should be removed from source host
      if (pxfHosts.contains(this.get('content.reassignHosts.source')) && !dataNodeHosts.contains(this.get('content.reassignHosts.source')))
        this.get('hostComponents').push('PXF');

      // If NAMENODE is being moved and target host does not have PXF, PXF should be added to target host
      if (!pxfHosts.contains(this.get('content.reassignHosts.target')))
        this.get('commands').splice(this.get('commands').indexOf('startAllServices'), 0, 'installPxf');
    }

    this._super();
  },

  initializeTasks: function () {
    var commands = this.get('commands');
    var hostComponentsNames = '';
    this.get('hostComponents').forEach(function (comp, index) {
      hostComponentsNames += index ? '+' : '';
      hostComponentsNames += comp === 'ZKFC' ? comp : App.format.role(comp, false);
    }, this);
    var currentStep = App.router.get('reassignMasterController.currentStep');
    for (var i = 0; i < commands.length; i++) {
      var title = Em.I18n.t('services.reassign.step6.tasks.' + commands[i] + '.title').format(hostComponentsNames);
      this.get('tasks').pushObject(Ember.Object.create({
        title: title,
        status: 'PENDING',
        id: i,
        command: commands[i],
        showRetry: false,
        showRollback: false,
        name: title,
        displayName: title,
        progress: 0,
        isRunning: false,
        hosts: []
      }));
    }

    this.removeUnneededTasks();
    this.set('isLoaded', true);
  },

  removeUnneededTasks: function () {
    if (this.get('content.reassign.component_name') !== 'MYSQL_SERVER') {
      this.removeTasks(['putHostComponentsInMaintenanceMode', 'stopMysqlService']);
      if (!this.get('content.reassignComponentsInMM.length')) {
        this.removeTasks(['stopHostComponentsInMaintenanceMode']);
      }
    } else {
      this.removeTasks(['stopHostComponentsInMaintenanceMode']);
    }
    if (!(this.get('content.reassign.component_name') === 'NAMENODE' && App.get('isHaEnabled'))) {
      this.removeTasks(['startDatanodes']);
    }
  },

  hideRollbackButton: function () {
    var failedTask = this.get('tasks').findProperty('showRollback');
    if (failedTask) {
      failedTask.set('showRollback', false)
    }
  }.observes('tasks.@each.showRollback'),

  onComponentsTasksSuccess: function () {
    this.decrementProperty('multiTaskCounter');
    if (this.get('multiTaskCounter') <= 0) {
      this.onTaskCompleted();
    }
  },

  installPxf: function () {
    this.createInstallComponentTask('PXF', this.get('content.reassignHosts.target'), "PXF");
  },

  startAllServices: function () {
    this.startServices(true);
  },

  deleteHostComponents: function () {
    var hostComponents = this.get('hostComponents');
    var hostName = this.get('content.reassignHosts.source');
    this.set('multiTaskCounter', hostComponents.length);
    for (var i = 0; i < hostComponents.length; i++) {
      App.ajax.send({
        name: 'common.delete.host_component',
        sender: this,
        data: {
          hostName: hostName,
          componentName: hostComponents[i]
        },
        success: 'onComponentsTasksSuccess',
        error: 'onDeleteHostComponentsError'
      });
    }
  },

  onDeleteHostComponentsError: function (error) {
    if (error.responseText.indexOf('org.apache.ambari.server.controller.spi.NoSuchResourceException') !== -1) {
      this.onComponentsTasksSuccess();
    } else {
      this.onTaskError();
    }
  },

  putHostComponentsInMaintenanceMode: function () {
    var hostComponents = this.get('hostComponents');
    var hostName = this.get('content.reassignHosts.source');
    this.set('multiTaskCounter', hostComponents.length);
    for (var i = 0; i < hostComponents.length; i++) {
      App.ajax.send({
        name: 'common.host.host_component.passive',
        sender: this,
        data: {
          hostName: hostName,
          passive_state: "ON",
          componentName: hostComponents[i]
        },
        success: 'onComponentsTasksSuccess',
        error: 'onTaskError'
      });
    }
  },

  stopHostComponentsInMaintenanceMode: function () {
    var hostComponentsInMM = this.get('content.reassignComponentsInMM');
    var hostName = this.get('content.reassignHosts.source');
    var serviceName = this.get('content.reassign.service_id');
    hostComponentsInMM = hostComponentsInMM.map(function(componentName){
      return {
        hostName: hostName,
        serviceName: serviceName,
        componentName: componentName
      };
    });
    this.set('multiTaskCounter', hostComponentsInMM.length);
    this.updateComponentsState(hostComponentsInMM, 'INSTALLED');
  },

  /**
   * make server call to stop services
   */
  stopMysqlService: function () {
    var data = {};

    data.context = "Stop required services";
    data.hostName = this.get('content.reassignHosts.source');
    data.serviceName = 'HIVE';
    data.HostRoles = { "state": "INSTALLED" };
    data.componentName = "MYSQL_SERVER";

    App.ajax.send({
      name: 'common.host.host_component.update',
      sender: this,
      data: data,
      success: 'startPolling',
      error: 'onTaskError'
    });
  },

  startDatanodes: function () {
    this.updateComponent('DATANODE', null, 'HDFS', 'Start');
  }

});
