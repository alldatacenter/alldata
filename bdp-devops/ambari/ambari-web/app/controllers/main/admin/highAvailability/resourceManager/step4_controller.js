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

require('controllers/main/admin/serviceAccounts_controller');

App.RMHighAvailabilityWizardStep4Controller = App.HighAvailabilityProgressPageController.extend(App.WizardEnableDone, {

  name: "rMHighAvailabilityWizardStep4Controller",

  clusterDeployState: 'RM_HIGH_AVAILABILITY_DEPLOY',

  commands: ['stopRequiredServices', 'installResourceManager', 'reconfigureYARN', 'reconfigureHAWQ', 'reconfigureHDFS', 'startAllServices'],

  tasksMessagesPrefix: 'admin.rm_highAvailability.wizard.step',

  initializeTasks: function () {
    this._super();
    var tasksToRemove = [];
    if (!App.Service.find().someProperty('serviceName', 'HAWQ')) {
      tasksToRemove.push('reconfigureHAWQ');
    }
    this.removeTasks(tasksToRemove);
  },

  stopRequiredServices: function () {
    this.stopServices(['HDFS']);
  },

  installResourceManager: function () {
    var hostName = this.get('content.rmHosts.additionalRM');
    this.createInstallComponentTask('RESOURCEMANAGER', hostName, "YARN");
  },

  reconfigureYARN: function () {
    this.loadConfigsTags("Yarn");
  },

  reconfigureHAWQ: function () {
    this.loadConfigsTags("Hawq");
  },

  reconfigureHDFS: function () {
    this.loadConfigsTags("Hdfs");
  },

  loadConfigsTags: function (service) {
    var onLoadServiceConfigsTags = 'onLoad' + service + "ConfigsTags";
    App.ajax.send({
      name: 'config.tags',
      sender: this,
      success: onLoadServiceConfigsTags,
      error: 'onTaskError'
    });
  },

  onLoadYarnConfigsTags: function (data) {
    App.ajax.send({
      name: 'reassign.load_configs',
      sender: this,
      data: {
        urlParams: '(type=yarn-site&tag=' + data.Clusters.desired_configs['yarn-site'].tag + ')',
        type: 'yarn-site'
      },
      success: 'onLoadConfigs',
      error: 'onTaskError'
    });
  },

  onLoadHawqConfigsTags: function (data) {
    App.ajax.send({
      name: 'reassign.load_configs',
      sender: this,
      data: {
        urlParams: '(type=yarn-client&tag=' + data.Clusters.desired_configs['yarn-client'].tag + ')',
        type: 'yarn-client'
      },
      success: 'onLoadConfigs',
      error: 'onTaskError'
    });
  },

  onLoadHdfsConfigsTags: function (data) {
    App.ajax.send({
      name: 'reassign.load_configs',
      sender: this,
      data: {
        urlParams: '(type=core-site&tag=' + data.Clusters.desired_configs['core-site'].tag + ')',
        type: 'core-site'
      },
      success: 'onLoadConfigs',
      error: 'onTaskError'
    });
  },

  onLoadConfigs: function (data, opt, params) {
    var propertiesToAdd = this.get('content.configs').filterProperty('filename', params.type);
    propertiesToAdd.forEach(function (property) {
      data.items[0].properties[property.name] = property.value;
    });

    var configData = this.reconfigureSites([params.type], data, Em.I18n.t('admin.highAvailability.step4.save.configuration.note').format(App.format.role('RESOURCEMANAGER', false)));

    App.ajax.send({
      name: 'common.service.configurations',
      sender: this,
      data: {
        desired_config: configData
      },
      success: 'onSaveConfigs',
      error: 'onTaskError'
    });
  },
  onSaveConfigs: function () {
    this.onTaskCompleted();
  },

  startAllServices: function () {
    this.startServices(true);
  }
});
