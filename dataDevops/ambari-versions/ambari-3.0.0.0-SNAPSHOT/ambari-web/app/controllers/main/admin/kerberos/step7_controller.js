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

App.KerberosWizardStep7Controller = App.KerberosProgressPageController.extend(App.AddSecurityConfigs, {
  name: 'kerberosWizardStep7Controller',
  clusterDeployState: 'KERBEROS_DEPLOY',
  isSingleRequestPage: true,
  request: {},
  commands: [],
  contextForPollingRequest: Em.I18n.t('requestInfo.kerberizeCluster'),

  /**
   * Define whether show Back button
   * @type {Boolean}
   */
  isBackButtonDisabled: true,

  /**
   * Start cluster kerberization. On retry just unkerberize and kerbrize cluster.
   * @param {bool} isRetry
   */
  setRequest: function (isRetry) {
    var kerberizeRequest = {
      name: 'KERBERIZE_CLUSTER',
      ajaxName: 'admin.kerberize.cluster',
      ajaxData: {
        data: {
          Clusters: {
            security_type: "KERBEROS"
          }
        }
      }
    };
    if (isRetry) {
      // on retry send force update
      this.set('request', {
        name: 'KERBERIZE_CLUSTER',
        ajaxName: 'admin.kerberize.cluster.force'
      });
      this.clearStage();
      this.loadStep();
    } else {
      this.set('request', kerberizeRequest);
    }
  },

  /**
   * Send request to unkerberisze cluster
   * @returns {$.ajax}
   */
  unkerberizeCluster: function () {
    return App.ajax.send({
      name: 'admin.unkerberize.cluster',
      sender: this,
      success: 'goToNextStep',
      error: 'goToNextStep'
    });
  },

  goToNextStep: function() {
    this.clearStage();
    App.router.transitionTo('step7');
  },

  retry: function () {
    this.set('showRetry', false);
    this.removeObserver('tasks.@each.status', this, 'onTaskStatusChange');
    this.set('status', 'IN_PROGRESS');
    this.get('tasks').setEach('status', 'PENDING');
    this.setRequest(true);
  },

  /**
   * Enable or disable previous steps according to tasks statuses
   */
  enableDisablePreviousSteps: function () {
    var wizardController = App.router.get(this.get('content.controllerName'));
    if (this.get('tasks').someProperty('status', 'FAILED')) {
      wizardController.enableStep(4);
      this.set('isBackButtonDisabled', false);
    } else {
      wizardController.setLowerStepsDisable(6);
      this.set('isBackButtonDisabled', true);
    }
  }.observes('tasks.@each.status')
});
