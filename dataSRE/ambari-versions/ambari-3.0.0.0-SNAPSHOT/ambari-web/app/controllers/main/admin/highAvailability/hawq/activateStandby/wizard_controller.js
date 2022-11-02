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

App.ActivateHawqStandbyWizardController = App.WizardController.extend({

  name: 'activateHawqStandbyWizardController',

  totalSteps: 3,

  /**
   * @type {string}
   */
  displayName: Em.I18n.t('admin.activateHawqStandby.wizard.header'),

  isFinished: false,

  content: Em.Object.create({
    controllerName: 'activateHawqStandbyWizardController'
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
          var dfd = $.Deferred();
          this.loadHawqHosts();
          this.loadServicesFromServer();
          this.loadMasterComponentHosts().done(function () {
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
      wizardControllerName: this.name,
      localdb: App.db.data
    });
  },

  /**
   * Save hosts for Hawq Master and Hawq Standby<code>controller.content</code>
   * @param hawqHosts
   */
  saveHawqHosts: function (hawqHosts) {
    this.set('content.hawqHosts', hawqHosts);
    this.setDBProperty('hawqHosts', hawqHosts);
  },

  /**
   * Load hosts for additional and current ResourceManagers from local db to <code>controller.content</code>
   */
  loadHawqHosts: function() {
    var hawqHosts = this.getDBProperty('hawqHosts');
    this.set('content.hawqHosts', hawqHosts);
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
