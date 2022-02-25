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

App.RollbackHighAvailabilityWizardController = App.WizardController.extend({

  name: 'rollbackHighAvailabilityWizardController',

  totalSteps: 3,

  /**
   * Used for hiding back button in wizard
   */
  hideBackButton: true,

  content: Em.Object.create({
    controllerName: 'RollbackHighAvailabilityWizardController',
    cluster: null,
    masterComponentHosts: null,
    serviceName: 'MISC',
    hdfsUser:"hdfs",
    nameServiceId: '',
    selectedAddNNHost : null,
    selectedSNNHost : null,
    activeNNHost: null
  }),

  setCurrentStep: function (currentStep, completed) {
    this._super(currentStep, completed);
    App.clusterStatus.setClusterStatus({
      clusterName: this.get('content.cluster.name'),
      clusterState: 'ROLLBACK_HIGH_AVAILABILITY',
      wizardControllerName: 'rollbackHighAvailabilityWizardController',
      localdb: App.db.data
    });
  },

  /**
   * return new object extended from clusterStatusTemplate
   * @return Object
   */
  getCluster: function(){
    return jQuery.extend({}, this.get('clusterStatusTemplate'), {name: App.router.getClusterName()});
  },

  /**
   * save status of the cluster.
   * @param clusterStatus object with status,requestId fields.
   */
  saveClusterStatus: function (clusterStatus) {
    var oldStatus = this.toObject(this.get('content.cluster'));
    clusterStatus = jQuery.extend(oldStatus, clusterStatus);
    if (clusterStatus.requestId) {
      clusterStatus.requestId.forEach(function (requestId) {
        if (clusterStatus.oldRequestsId.indexOf(requestId) === -1) {
          clusterStatus.oldRequestsId.push(requestId)
        }
      }, this);
    }
    this.set('content.cluster', clusterStatus);
    this.save('cluster');
  },

  saveTasksStatuses: function(statuses){
    App.db.setRollbackHighAvailabilityWizardTasksStatuses(statuses);
    this.set('content.tasksStatuses', statuses);
  },

  saveRequestIds: function(requestIds){
    App.db.setRollbackHighAvailabilityWizardRequestIds(requestIds);
    this.set('content.requestIds', requestIds);
  },

  saveSelectedSNN: function(addNN){
    App.db.setRollBackHighAvailabilityWizardSelectedSNN(addNN);
    this.set('content.selectedAddNN', addNN);
  },

  saveSelectedAddNN: function(sNN){
    App.db.setRollBackHighAvailabilityWizardSelectedAddNN(sNN);
    this.set('content.selectedSNN', sNN);
  },

  loadAddNNHost: function () {
    var addNNHost = App.db.getRollBackHighAvailabilityWizardAddNNHost();
    this.set('content.addNNHost', addNNHost);
  },

  loadSNNHost: function () {
    var sNNHost = App.db.getRollBackHighAvailabilityWizardSNNHost();
    this.set('content.sNNHost', sNNHost);
  },

  loadTasksStatuses: function(){
    var sNNHost = App.db.getRollbackHighAvailabilityWizardTasksStatuses();
    this.set('content.tasksStatuses', sNNHost);
  },

  loadRequestIds: function(){
    var requestIds = App.db.getRollbackHighAvailabilityWizardRequestIds();
    this.set('content.requestIds', requestIds);
  },

  /**
   * Load data for all steps until <code>current step</code>
   */
  loadAllPriorSteps: function () {
    var step = this.get('currentStep');
    switch (step) {
      case '3':
      case '2':
      case '1':
        this.loadSNNHost();
        this.loadAddNNHost();
        this.load('cluster');
    }
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
  },

  /**
   * Clear all temporary data
   */
  finish: function () {
    this.setCurrentStep('1');
    this.clearAllSteps();
    App.router.get('updateController').updateAll();
  }
});
