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

App.ManageJournalNodeWizardController = App.WizardController.extend({

  name: 'manageJournalNodeWizardController',

  totalSteps: 7,

  displayName: Em.I18n.t('admin.manageJournalNode.wizard.header'),

  /**
   * Used for hiding back button in wizard
   */
  hideBackButton: true,

  content: Em.Object.create({
    controllerName: 'manageJournalNodeWizardController',
    cluster: null,
    hosts: null,
    services: null,
    slaveComponentHosts: null,
    masterComponentHosts: null,
    serviceConfigProperties: [],
    serviceName: 'MISC',
    hdfsUser: "hdfs",
    nameServiceId: '',
    failedTask: null,
    requestIds: null
  }),

  setCurrentStep: function (currentStep, completed) {
    this._super(currentStep, completed);
    App.clusterStatus.setClusterStatus({
      clusterName: this.get('content.cluster.name'),
      clusterState: 'JOURNALNODE_MANAGEMENT',
      wizardControllerName: 'manageJournalNodeWizardController',
      localdb: App.db.data
    });
  },

  /**
   * return new object extended from clusterStatusTemplate
   * @return Object
   */
  getCluster: function () {
    return jQuery.extend({}, this.get('clusterStatusTemplate'), {name: App.router.getClusterName()});
  },

  loadMap: {
    '1': [
      {
        type: 'async',
        callback: function () {
          var dfd = $.Deferred(),
            self = this,
            usersLoadingCallback = function () {
              self.save('hdfsUser');
              self.load('cluster');
              self.loadHosts().done(function () {
                self.loadServicesFromServer();
                self.loadMasterComponentHosts().done(function () {
                  self.load('hdfsUser');
                  if (!self.getDBProperty('activeNN')) {
                    self.saveNNs();
                  } else {
                    self.loadNNs();
                  }
                  dfd.resolve();
                });
              });
            };
          if (self.getDBProperty('hdfsUser')) {
            usersLoadingCallback();
          } else {
            this.loadHdfsUserFromServer().done(function (data) {
              self.set('content.hdfsUser', Em.get(data, '0.properties.hdfs_user'));
              usersLoadingCallback();
            });
          }
          return dfd.promise();
        }
      }
    ],
    2: [
      {
        type: 'sync',
        callback: function () {
          this.loadNameServiceId();
          this.loadServiceConfigProperties();
        }
      }
    ],
    '4': [
      {
        type: 'sync',
        callback: function () {
          this.loadTasksStatuses();
          this.loadTasksRequestIds();
          this.loadRequestIds();
        }
      }
    ]
  },

  getJournalNodesToAdd: function () {
    var result = [];
    var masterComponentHosts = this.get('content.masterComponentHosts');
    if (masterComponentHosts) {
      result = masterComponentHosts
        .filterProperty('component', 'JOURNALNODE')
        .filterProperty('isInstalled', false)
        .mapProperty('hostName');
    }
    return result;
  },

  getJournalNodesToDelete: function () {
    var result = [];
    var masterComponentHosts = this.get('content.masterComponentHosts');
    if (masterComponentHosts) {
      var currentJNs = masterComponentHosts.filterProperty('component', 'JOURNALNODE');
      var existingHosts = App.HostComponent.find().filterProperty('componentName', 'JOURNALNODE').mapProperty('hostName');
      result = existingHosts.filter(function (host) {
        return currentJNs.filterProperty('hostName', host).length === 0;
      });
    }
    return result;
  },

  isDeleteOnly: function () {
    return this.get('currentStep') > 1 && this.getJournalNodesToAdd().length === 0 && this.getJournalNodesToDelete().length > 0;
  }.property('content.masterComponentHosts', 'App.router.clusterController.isHostsLoaded', 'currentStep'),

  /**
   * Save config properties
   * @param stepController ManageJournalNodeWizardStep3Controller
   */
  saveServiceConfigProperties: function (stepController) {
    var serviceConfigProperties = [];
    var data = stepController.get('serverConfigData');

    var _content = stepController.get('stepConfigs')[0];
    _content.get('configs').forEach(function (_configProperties) {
      var siteObj = data.items.findProperty('type', _configProperties.get('filename'));
      if (siteObj) {
        siteObj.properties[_configProperties.get('name')] = _configProperties.get('value');
      }
    }, this);
    this.setDBProperty('serviceConfigProperties', data);
    this.set('content.serviceConfigProperties', data);
  },

  /**
   * Load serviceConfigProperties to model
   */
  loadServiceConfigProperties: function () {
    this.set('content.serviceConfigProperties', this.getDBProperty('serviceConfigProperties'));
  },


  saveNNs: function () {
    var activeNN = App.HostComponent.find().findProperty('displayNameAdvanced', 'Active NameNode');
    var standByNN = App.HostComponent.find().findProperty('displayNameAdvanced', 'Standby NameNode');
    this.set('content.activeNN', activeNN);
    this.set('content.standByNN', standByNN);
    this.setDBProperty('activeNN', activeNN);
    this.setDBProperty('standByNN', standByNN);
  },

  loadNNs: function () {
    var activeNN = this.getDBProperty('activeNN');
    var standByNN = this.getDBProperty('standByNN');
    this.set('content.activeNN', activeNN);
    this.set('content.standByNN', standByNN);
  },


  saveConfigTag: function (tag) {
    App.db.setManageJournalNodeWizardConfigTag(tag);
    this.set('content.' + tag.name, tag.value);
  },


  loadConfigTag: function (tag) {
    var tagVal = App.db.getManageJournalNodeWizardConfigTag(tag);
    this.set('content.' + tag, tagVal);
  },

  saveNameServiceId: function (nameServiceId) {
    this.setDBProperty('nameServiceId', nameServiceId);
    this.set('content.nameServiceId', nameServiceId);
  },

  loadNameServiceId: function () {
    var nameServiceId = this.getDBProperty('nameServiceId');
    this.set('content.nameServiceId', nameServiceId);
  },

  /**
   * Remove all loaded data.
   * Created as copy for App.router.clearAllSteps
   */
  clearAllSteps: function () {
    this.clearInstallOptions();
    // clear temporary information stored during the install
    this.set('content.cluster', this.getCluster());
  },

  clearTasksData: function () {
    this.saveTasksStatuses(undefined);
    this.saveRequestIds(undefined);
    this.saveTasksRequestIds(undefined);
  },

  /**
   * Clear all temporary data
   */
  finish: function () {
    App.db.data.Installer = {};
    this.resetDbNamespace();
    App.router.get('updateController').updateAll();
  }
});
