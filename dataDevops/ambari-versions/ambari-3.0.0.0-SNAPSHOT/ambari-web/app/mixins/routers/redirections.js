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

/**
 * Mixin for routers which may redirect user to the some cluster installer steps
 * Based on cluster state
 * @type {Em.Mixin}
 */
App.RouterRedirections = Em.Mixin.create({

  /**
   * List of cluster statuses when it's not installed
   * @type {string[]}
   */
  installerStatuses: ['CLUSTER_NOT_CREATED_1', 'CLUSTER_DEPLOY_PREP_2', 'CLUSTER_INSTALLING_3', 'SERVICE_STARTING_3', 'CLUSTER_INSTALLED_4'],

  /**
   * Redirect user to the proper installer step if needed
   * Based on cluster state:
   *  1. CLUSTER_NOT_CREATED_1 - to current step
   *  2. CLUSTER_DEPLOY_PREP_2 - to step 8
   *  3. CLUSTER_INSTALLING_3/SERVICE_STARTING_3 - to step 9
   *  4. CLUSTER_INSTALLED_4 - to step 10
   * @param {Em.Router} router
   * @param {Object} currentClusterStatus
   * @param {Boolean} isOnInstaller true - user is on the cluster installer, false - something else
   * @method redirectToInstaller
   */
  redirectToInstaller: function (router, currentClusterStatus, isOnInstaller) {

    var installerController = router.get('installerController');
    var path = isOnInstaller ? '' : 'installer.';
    switch (currentClusterStatus.clusterState) {
      case 'CLUSTER_NOT_CREATED_1' :
        App.get('router').setAuthenticated(true);
        router.transitionTo(path + 'step' + installerController.get('currentStep'));
        break;
      case 'CLUSTER_DEPLOY_PREP_2' :
        installerController.setCurrentStep('8');
        App.db.data = currentClusterStatus.localdb;
        App.get('router').setAuthenticated(true);
        router.transitionTo(path + 'step' + installerController.get('currentStep'));
        break;
      case 'CLUSTER_INSTALLING_3' :
      case 'SERVICE_STARTING_3' :
        if (!installerController.get('isStep9')) {
          installerController.setCurrentStep('9');
          installerController.setLowerStepsDisable(9);
        }
        router.transitionTo(path + 'step' + installerController.get('currentStep'));
        break;
      case 'CLUSTER_INSTALLED_4' :
        if (!installerController.get('isStep10')) {
          installerController.setCurrentStep('10');
          installerController.setLowerStepsDisable(10);
        }
        App.db.data = currentClusterStatus.localdb;
        App.get('router').setAuthenticated(true);
        router.transitionTo(path + 'step' + installerController.get('currentStep'));
        break;
    }
  }

});