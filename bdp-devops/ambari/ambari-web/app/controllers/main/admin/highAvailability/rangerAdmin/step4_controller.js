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

App.RAHighAvailabilityWizardStep4Controller = App.HighAvailabilityProgressPageController.extend({

  name: "rAHighAvailabilityWizardStep4Controller",

  clusterDeployState: 'RA_HIGH_AVAILABILITY_DEPLOY',

  commands: ['stopAllServices', 'installRangerAdmin', 'reconfigureRanger', 'startAllServices'],

  tasksMessagesPrefix: 'admin.ra_highAvailability.wizard.step',

  stopAllServices: function () {
    this.stopServices([], true, true);
  },

  installRangerAdmin: function () {
    var hostNames = this.get('content.raHosts.additionalRA');
    this.createInstallComponentTask('RANGER_ADMIN', hostNames, "RANGER");
  },

  reconfigureRanger: function () {
    this.loadConfigsTags();
  },

  loadConfigsTags: function () {
    App.ajax.send({
      name: 'config.tags',
      sender: this,
      success: 'onLoadConfigsTags',
      error: 'onTaskError'
    });
  },

  onLoadConfigsTags: function (data) {
    var urlParams = [];
    var siteNamesToFetch = this.get('wizardController.configs').mapProperty('siteName');
    siteNamesToFetch.map(function(siteName) {
      if(siteName in data.Clusters.desired_configs) {
        urlParams.push('(type=' + siteName + '&tag=' + data.Clusters.desired_configs[siteName].tag + ')');
      }
    });
    App.ajax.send({
      name: 'reassign.load_configs',
      sender: this,
      data: {
        urlParams: urlParams.join('|')
      },
      success: 'onLoadConfigs',
      error: 'onTaskError'
    });
  },

  onLoadConfigs: function (data) {
    var configs = [];

    this.get('wizardController.configs').map(function(item) {
      var config = data.items.findProperty('type', item.siteName);
      if (config) {
        config.properties[item.propertyName] = this.get('content.loadBalancerURL');
        configs.push({
          Clusters: {
            desired_config: this.reconfigureSites([item.siteName], data, Em.I18n.t('admin.highAvailability.step4.save.configuration.note').format(App.format.role('RANGER_ADMIN', false)))
          }
        });
      }
    }, this);

    App.ajax.send({
      name: 'common.service.multiConfigurations',
      sender: this,
      data: {
        configs: configs
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
