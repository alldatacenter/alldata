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

App.ActivateHawqStandbyWizardStep3Controller = App.HighAvailabilityProgressPageController.extend(App.WizardEnableDone, {

  name: "activateHawqStandbyWizardStep3Controller",

  clusterDeployState: 'ACTIVATE_HAWQ_STANDBY',

  hawqActivateStandbyCustomCommand: "ACTIVATE_HAWQ_STANDBY",

  hawqServiceName: "HAWQ",

  hawqStandbyComponentName: "HAWQSTANDBY",

  hawqMasterComponentName: "HAWQMASTER",

  commands: ['activateStandby', 'stopRequiredServices', 'reconfigureHAWQ', 'installHawqMaster', 'deleteOldHawqMaster',
              'deleteHawqStandby', 'startRequiredServices'],

  tasksMessagesPrefix: 'admin.activateHawqStandby.wizard.step',

  activateStandby: function () {
    App.ajax.send({
      name : 'service.item.executeCustomCommand',
      sender: this,
      data : {
        command : this.hawqActivateStandbyCustomCommand,
        context : Em.I18n.t('admin.activateHawqStandby.wizard.step3.activateHawqStandbyCommand.context'),
        hosts : this.get('content.hawqHosts.hawqStandby'),
        serviceName : this.hawqServiceName,
        componentName : this.hawqStandbyComponentName
      },
      success: 'startPolling',
      error: 'onTaskError'
    });
  },

  stopRequiredServices: function () {
    this.stopServices([this.hawqServiceName], true);
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
    delete data.items[0].properties['hawq_standby_address_host'];

    var propertiesToAdd = this.get('content.configs').filterProperty('filename', params.type);
    propertiesToAdd.forEach(function (property) {
      data.items[0].properties[property.name] = property.value;
    });

    var configData = this.reconfigureSites([params.type], data, Em.I18n.t('admin.activateHawqStandby.step4.save.configuration.note').format(App.format.role('HAWQSTANDBY', false)));

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

  installHawqMaster: function () {
    var hostName = this.get('content.hawqHosts.hawqStandby');
    this.createInstallComponentTask(this.hawqMasterComponentName, hostName, this.hawqServiceName);
  },

  deleteOldHawqMaster: function () {
    var hostName = this.get('content.hawqHosts.hawqMaster');
    this.deleteComponent(this.hawqMasterComponentName, hostName);
  },

  deleteHawqStandby: function () {
    var hostName = this.get('content.hawqHosts.hawqStandby');
    this.deleteComponent(this.hawqStandbyComponentName, hostName);
  },

  startRequiredServices: function () {
    this.startServices(false, [this.hawqServiceName], true);
  }

});
