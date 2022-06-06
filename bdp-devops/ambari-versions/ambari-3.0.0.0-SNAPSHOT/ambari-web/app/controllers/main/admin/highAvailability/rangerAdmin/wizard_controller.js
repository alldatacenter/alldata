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

App.RAHighAvailabilityWizardController = App.WizardController.extend({

  name: 'rAHighAvailabilityWizardController',

  totalSteps: 4,

  /**
   * @type {string}
   */
  displayName: Em.I18n.t('admin.ra_highAvailability.wizard.header'),

  isFinished: false,

  content: Em.Object.create({
    controllerName: 'rAHighAvailabilityWizardController',
    cluster: null,
    loadBalancerURL: null,
    hosts: null,
    services: null,
    masterComponentHosts: null
  }),

  configs: [
    {
      siteName: 'admin-properties',
      propertyName: 'policymgr_external_url'
    },
    {
      siteName: 'ranger-hdfs-security',
      propertyName: 'ranger.plugin.hdfs.policy.rest.url'
    },
    {
      siteName: 'ranger-yarn-security',
      propertyName: 'ranger.plugin.yarn.policy.rest.url'
    },
    {
      siteName: 'ranger-hbase-security',
      propertyName: 'ranger.plugin.hbase.policy.rest.url'
    },
    {
      siteName: 'ranger-hive-security',
      propertyName: 'ranger.plugin.hive.policy.rest.url'
    },
    {
      siteName: 'ranger-knox-security',
      propertyName: 'ranger.plugin.knox.policy.rest.url'
    },
    {
      siteName: 'ranger-kafka-security',
      propertyName: 'ranger.plugin.kafka.policy.rest.url'
    },
    {
      siteName: 'ranger-kms-security',
      propertyName: 'ranger.plugin.kms.policy.rest.url'
    },
    {
      siteName: 'ranger-storm-security',
      propertyName: 'ranger.plugin.storm.policy.rest.url'
    },
    {
      siteName: 'ranger-atlas-security',
      propertyName: 'ranger.plugin.atlas.policy.rest.url'
    }
  ],

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
      wizardControllerName: 'rAHighAvailabilityWizardController',
      localdb: App.db.data
    });
  },

  loadMap: {
    '1': [
      {
        type: 'sync',
        callback: function () {
          this.load('cluster');
          this.load('loadBalancerURL');
        }
      }
    ],
    '2': [
      {
        type: 'async',
        callback: function () {
          var dfd = $.Deferred();
          var self = this;
          this.loadHosts().done(function () {
            self.loadServicesFromServer();
            self.loadMasterComponentHosts().done(function () {
              dfd.resolve();
            });
          });
          return dfd.promise();
        }
      }
    ],
    '3': [
      {
        type: 'sync',
        callback: function () {
          this.load('raHosts');
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
