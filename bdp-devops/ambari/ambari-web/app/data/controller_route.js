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
 * This is required to reroute directly to the wizard controller. This is desired in following scenarios
 * 1. A wizard is being navigated and another browser window is simultaneously opened by the same or another user
 * 2. browser window crashed while navigating through any wizard and a new browser window is opened to complete the wizard
 * @type {Array} Array of Objects
 */
module.exports = [
  {
    wizardControllerName: App.router.get('addHostController.name'),
    route: 'main.hostAdd'
  },
  {
    wizardControllerName: App.router.get('kerberosWizardController.name'),
    route: 'main.admin.adminKerberos.adminAddKerberos'
  },
  {
    wizardControllerName: App.router.get('addServiceController.name'),
    route: 'main.serviceAdd'
  },
  {
    wizardControllerName: App.router.get('reassignMasterController.name'),
    route: 'main.reassign'
  },
  {
    wizardControllerName: App.router.get('highAvailabilityWizardController.name'),
    route: 'main.services.enableHighAvailability'
  },
  {
    wizardControllerName: App.router.get('rMHighAvailabilityWizardController.name'),
    route: 'main.services.enableRMHighAvailability'
  },
  {
    wizardControllerName: App.router.get('addHawqStandbyWizardController.name'),
    route: 'main.services.addHawqStandby'
  },
  {
    wizardControllerName: App.router.get('removeHawqStandbyWizardController.name'),
    route: 'main.services.removeHawqStandby'
  },
  {
    wizardControllerName: App.router.get('activateHawqStandbyWizardController.name'),
    route: 'main.services.activateHawqStandby'
  },
  {
    wizardControllerName: App.router.get('rAHighAvailabilityWizardController.name'),
    route: 'main.services.enableRAHighAvailability'
  },
  {
    wizardControllerName: App.router.get('rollbackHighAvailabilityWizardController.name'),
    route: 'main.services.rollbackHighAvailability'
  },
  {
    wizardControllerName: App.router.get('mainAdminStackAndUpgradeController.name'),
    route: App.db.get('MainAdminStackAndUpgrade', 'upgradeState') === 'NOT_REQUIRED' ? 'main.admin.stackAndUpgrade.index' : 'main.admin.stackAndUpgrade.versions'
  },
  {
    wizardControllerName: App.router.get('widgetWizardController.name'),
    route: 'main.createWidget'
  },
  {
    wizardControllerName: App.router.get('widgetEditController.name'),
    route: 'main.editWidget'
  },
  {
    wizardControllerName: App.router.get('nameNodeFederationWizardController.name'),
    route: 'main.services.enableNameNodeFederation'
  },
  {
    wizardControllerName: App.router.get('manageJournalNodeWizardController.name'),
    route: 'main.services.manageJournalNode'
  }
];
