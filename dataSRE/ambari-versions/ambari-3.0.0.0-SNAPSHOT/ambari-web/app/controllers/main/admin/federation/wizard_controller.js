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

App.NameNodeFederationWizardController = App.WizardController.extend({

  name: 'nameNodeFederationWizardController',

  totalSteps: 4,

  /**
   * @type {string}
   */
  displayName: Em.I18n.t('admin.nameNodeFederation.wizard.header'),

  isFinished: false,

  content: Em.Object.create({
    controllerName: 'nameNodeFederationWizardController'
  }),

  /**
   * Load data for all steps until <code>current step</code>
   */
  loadMap: {
    '1': [
      {
        type: 'sync',
        callback: function () {
          this.load('cluster');
        }
      }
    ],
    '2': [
      {
        type: 'async',
        callback: function () {
          var self = this,
            dfd = $.Deferred();
          this.loadServicesFromServer();
          this.loadMasterComponentHosts().done(function () {
            self.loadConfirmedHosts();
            dfd.resolve();
          });
          return dfd.promise();
        }
      }
    ],
    '3': [
      {
        type: 'sync',
        callback: function () {
          this.loadNameServiceId();
        }
      }
    ],
    '4': [
      {
        type: 'sync',
        callback: function () {
          this.loadServiceConfigProperties();
          this.loadTasksStatuses();
          this.loadTasksRequestIds();
          this.loadRequestIds();
        }
      }
    ]
  },

  init: function () {
    this._super();
    this.clearStep();
  },

  clearStep: function () {
    this.set('isFinished', false);
  },

  setCurrentStep: function (currentStep, completed) {
    this._super(currentStep, completed);
    App.clusterStatus.setClusterStatus({
      clusterName: this.get('content.cluster.name'),
      wizardControllerName: 'nameNodeFederationWizardController',
      localdb: App.db.data
    });
  },

  saveNameServiceId: function (nameServiceId) {
    this.setDBProperty('nameServiceId', nameServiceId);
    this.set('content.nameServiceId', nameServiceId);
  },

  loadNameServiceId: function () {
    var nameServiceId = this.getDBProperty('nameServiceId');
    this.set('content.nameServiceId', nameServiceId);
  },

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

  /**
   * Remove all loaded data.
   * Created as copy for App.router.clearAllSteps
   */
  clearAllSteps: function () {
    this.clearInstallOptions();
    // clear temporary information stored during the install
    this.set('content.cluster', this.getCluster());
  },

  /**
   * Clear all temporary data
   */
  finish: function () {
    this.resetDbNamespace();
    App.router.get('updateController').updateAll();
    this.set('isFinished', true);
  }
});
