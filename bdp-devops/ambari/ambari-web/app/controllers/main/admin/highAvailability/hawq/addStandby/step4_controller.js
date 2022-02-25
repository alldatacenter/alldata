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

App.AddHawqStandbyWizardStep4Controller = App.HighAvailabilityProgressPageController.extend(App.WizardEnableDone, {

  name: "addHawqStandbyWizardStep4Controller",

  clusterDeployState: 'ADD_HAWQ_STANDBY',

  commands: ['stopRequiredServices', 'installHawqStandbyMaster', 'reconfigureHAWQ', 'startRequiredServices'],

  tasksMessagesPrefix: 'admin.addHawqStandby.wizard.step',

  stopRequiredServices: function () {
    this.stopServices(['HAWQ'], true);
  },

  installHawqStandbyMaster: function () {
    var hostName = this.get('content.hawqHosts.newHawqStandby');
    this.createInstallComponentTask('HAWQSTANDBY', hostName, "HAWQ");
  },

  reconfigureHAWQ: function () {
    App.ajax.send({
      name: 'config.tags',
      sender: this,
      success: 'onLoadHawqConfigsTags',
      error: 'onTaskError'
    });
  },

  onLoadHawqConfigsTags: function (data) {
    App.ajax.send({
      name: 'reassign.load_configs',
      sender: this,
      data: {
        urlParams: '(type=hawq-site&tag=' + data.Clusters.desired_configs['hawq-site'].tag + ')',
        type: 'hawq-site'
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

    var configData = this.reconfigureSites([params.type], data, Em.I18n.t('admin.addHawqStandby.step4.save.configuration.note').format(App.format.role('HAWQSTANDBY', false)));

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

  startRequiredServices: function () {
    this.startServices(false, ["HAWQ"], true);
  }
});

