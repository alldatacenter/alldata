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

App.RMHighAvailabilityWizardController = App.WizardController.extend({

  name: 'rMHighAvailabilityWizardController',

  totalSteps: 4,

  /**
   * @type {string}
   */
  displayName: Em.I18n.t('admin.rm_highAvailability.wizard.header'),

  isFinished: false,

  content: Em.Object.create({
    controllerName: 'rMHighAvailabilityWizardController'
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
          this.loadRmHosts();
          this.loadServicesFromServer();
          this.loadMasterComponentHosts().done(function () {
            self.loadConfirmedHosts();
            dfd.resolve();
          });
          return dfd.promise();
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
          this.loadConfigs();
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
      wizardControllerName: 'rMHighAvailabilityWizardController',
      localdb: App.db.data
    });
  },

  /**
   * Save hosts for additional and current ResourceManagers to local db and <code>controller.content</code>
   * @param rmHosts
   */
  saveRmHosts: function (rmHosts) {
    this.set('content.rmHosts', rmHosts);
    this.setDBProperty('rmHosts', rmHosts);
  },

  /**
   * Load hosts for additional and current ResourceManagers from local db to <code>controller.content</code>
   */
  loadRmHosts: function() {
    var rmHosts = this.getDBProperty('rmHosts');
    this.set('content.rmHosts', rmHosts);
  },

  /**
   * Save configs to load and apply them on Configure Components step
   * @param configs
   */
  saveConfigs: function (configs) {
    this.set('content.configs', configs);
    this.setDBProperty('configs', configs);
  },

  /**
   * Load configs to apply them on Configure Components step
   */
  loadConfigs: function() {
    var configs = this.getDBProperty('configs');
    this.set('content.configs', configs);
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
