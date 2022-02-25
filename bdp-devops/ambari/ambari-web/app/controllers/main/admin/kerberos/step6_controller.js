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

App.KerberosWizardStep6Controller = App.KerberosProgressPageController.extend({
  name: 'kerberosWizardStep6Controller',
  clusterDeployState: 'KERBEROS_DEPLOY',
  commands: ['stopServices'],

  stopServices: function () {
    App.ajax.send({
      name: 'common.services.update',
      data: {
        context: "Stop services",
        "ServiceInfo": {
          "state": "INSTALLED"
        }
      },
      sender: this,
      success: 'startPolling',
      error: 'onTaskError'
    });
  },

  loadStep: function() {
    this.checkComponentsRemoval();
    this._super();
  },

  /**
   * remove Application Timeline Server component if needed.
   */
  checkComponentsRemoval: function() {
    if (App.Service.find().someProperty('serviceName', 'YARN') && !App.get('doesATSSupportKerberos')
      && !this.get('commands').contains('deleteATS') && App.HostComponent.find().findProperty('componentName', 'APP_TIMELINE_SERVER')) {
      this.get('commands').pushObject('deleteATS');
    }
  },

  /**
   * Remove Application Timeline Server from the host.
   * @returns {$.Deferred}
   */
  deleteATS: function() {
    return App.ajax.send({
      name: 'common.delete.host_component',
      sender: this,
      data: {
        componentName: 'APP_TIMELINE_SERVER',
        hostName: App.HostComponent.find().findProperty('componentName', 'APP_TIMELINE_SERVER').get('hostName')
      },
      success: 'onDeleteATSSuccess',
      error: 'onDeleteATSError'
    });
  },

  onDeleteATSSuccess: function() {
    this.onTaskCompleted();
  },

  onDeleteATSError: function(error) {
    if (error.responseText.indexOf('org.apache.ambari.server.controller.spi.NoSuchResourceException') !== -1) {
      this.onDeleteATSSuccess();
    }
  }
});
