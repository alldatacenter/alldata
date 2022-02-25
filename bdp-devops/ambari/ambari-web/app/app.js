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

if (Ember.$.uuid === undefined) {
  Ember.$.uuid = 0;
}

// Application bootstrapper
require('utils/bootstrap_reopen');
require('utils/ember_reopen');
require('utils/ember_computed');
var stringUtils = require('utils/string_utils');
var stompClientClass = require('utils/stomp_client');

module.exports = Em.Application.create({
  name: 'Ambari Web',
  rootElement: '#wrapper',

  store: DS.Store.create({
    revision: 4,
    adapter: DS.FixtureAdapter.create({
      simulateRemoteResponse: false
    }),
    typeMaps: {},
    recordCache: []
  }),
  StompClient: stompClientClass.create(),
  isAdmin: false,
  isOperator: false,
  isClusterUser: false,
  isPermissionDataLoaded: false,
  auth: undefined,
  isOnlyViewUser: function() {
    return App.auth && (App.auth.length == 0 || (App.isAuthorized('VIEW.USE') && App.auth.length == 1));
  }.property('auth'),

  /**
   * @type {boolean}
   * @default false
   */
  isKerberosEnabled: false,

  /**
   * state of stack upgrade process
   * states:
   *  - NOT_REQUIRED
   *  - PENDING
   *  - IN_PROGRESS
   *  - HOLDING
   *  - COMPLETED
   *  - ABORTED
   *  - HOLDING_FAILED
   *  - HOLDING_TIMEDOUT
   * @type {String}
   */
  upgradeState: 'NOT_REQUIRED',

  /**
   * Check if upgrade is in INIT state
   * 'INIT' is set on upgrade start and when it's finished
   * @type {boolean}
   */
  upgradeInit: Em.computed.equal('upgradeState', 'NOT_REQUIRED'),

  /**
   * flag is true when upgrade process is running
   * @returns {boolean}
   */
  upgradeInProgress: Em.computed.equal('upgradeState', 'IN_PROGRESS'),

  /**
   * Checks if update process is completed
   * @type {boolean}
   */
  upgradeCompleted: Em.computed.equal('upgradeState', 'COMPLETED'),

  /**
   * flag is true when upgrade process is waiting for user action
   * to proceed, retry, perform manual steps etc.
   * @returns {boolean}
   */
  upgradeHolding: function() {
    return this.get('upgradeState').contains("HOLDING") || this.get('upgradeAborted');
  }.property('upgradeState', 'upgradeAborted'),

  /**
   * flag is true when upgrade process is aborted
   * SHOULD behave similar to HOLDING_FAILED state
   * @returns {boolean}
   */
  upgradeAborted: function () {
    return this.get('upgradeState') === "ABORTED" && !App.router.get('mainAdminStackAndUpgradeController.isSuspended');
  }.property('upgradeState', 'router.mainAdminStackAndUpgradeController.isSuspended'),

  /**
   * flag is true when upgrade process is suspended
   * @returns {boolean}
   */
  upgradeSuspended: function () {
    return this.get('upgradeState') === "ABORTED" && App.router.get('mainAdminStackAndUpgradeController.isSuspended');
  }.property('upgradeState', 'router.mainAdminStackAndUpgradeController.isSuspended'),

  /**
   * RU is running
   * @type {boolean}
   */
  upgradeIsRunning: Em.computed.or('upgradeInProgress', 'upgradeHolding'),

  /**
   * flag is true when upgrade process is running or suspended
   * or wizard used by another user
   * @returns {boolean}
   */
  wizardIsNotFinished: function () {
    return this.get('upgradeIsRunning') ||
           this.get('upgradeSuspended') ||
           App.router.get('wizardWatcherController.isNonWizardUser');
  }.property('upgradeIsRunning', 'upgradeAborted', 'router.wizardWatcherController.isNonWizardUser', 'upgradeSuspended'),

  /**
   * @param {string} authRoles
   * @returns {boolean}
   */
  havePermissions: function (authRoles) {
    var result = false;
    authRoles = $.map(authRoles.split(","), $.trim);

    // When Upgrade running(not suspended) only operations related to upgrade should be allowed
    if ((!this.get('upgradeSuspended') &&
      !authRoles.contains('CLUSTER.UPGRADE_DOWNGRADE_STACK') &&
      !authRoles.contains('CLUSTER.MANAGE_USER_PERSISTED_DATA')) &&
      !App.get('supports.opsDuringRollingUpgrade') &&
      !['NOT_REQUIRED', 'COMPLETED'].contains(this.get('upgradeState')) ||
      !App.auth){
      return false;
    }

    authRoles.forEach(function (auth) {
      result = result || App.auth.contains(auth);
    });

    return result;
  },
  /**
   * @param {string} authRoles
   * @returns {boolean}
   */
  isAuthorized: function (authRoles) {
    return this.havePermissions(authRoles) && !App.router.get('wizardWatcherController.isNonWizardUser');
  },

  isStackServicesLoaded: false,
  /**
   * return url prefix with number value of version of HDP stack
   */
  stackVersionURL: function () {
    return '/stacks/{0}/versions/{1}'.format(this.get('currentStackName') || 'HDP', this.get('currentStackVersionNumber'));
  }.property('currentStackName','currentStackVersionNumber'),

  falconServerURL: function () {
    var falconService = this.Service.find().findProperty('serviceName', 'FALCON');
    if (falconService) {
      return falconService.get('hostComponents').findProperty('componentName', 'FALCON_SERVER').get('hostName');
    }
    return '';
  }.property().volatile(),

  /* Determine if Application Timeline Service supports Kerberization.
   * Because this value is retrieved from the cardinality of the component, it is safe to keep in app.js
   * since its value will not change during the lifetime of the application.
   */
  doesATSSupportKerberos: function() {
    var YARNService = App.StackServiceComponent.find().filterProperty('serviceName', 'YARN');
    if (YARNService.length) {
      var ATS = App.StackServiceComponent.find().findProperty('componentName', 'APP_TIMELINE_SERVER');
      return (!!ATS && !!ATS.get('minToInstall'));
    }
    return false;
  }.property('router.clusterController.isLoaded'),

  clusterId: null,
  clusterName: null,
  clockDistance: null, // server clock - client clock
  currentStackVersion: '',
  currentStackName: function() {
    return Em.get((this.get('currentStackVersion') || this.get('defaultStackVersion')).match(/(.+)-\d.+/), '1');
  }.property('currentStackVersion', 'defaultStackVersion'),

  /**
   * true if cluster has only 1 host
   * for now is used to disable move/HA actions
   * @type {boolean}
   */
  isSingleNode: Em.computed.equal('allHostNames.length', 1),

  allHostNames: [],

  /**
   * This object is populated to keep track of uninstalled components to be included in the layout for recommendation/validation call
   * @type {object}
   * keys = componentName, hostName
   */
  componentToBeAdded: {},


  /**
   * This object is populated to keep track of installed components to be excluded in the layout for recommendation/validation call
   * @type {object}
   * keys = componentName, hostName
   */
  componentToBeDeleted: {},

  uiOnlyConfigDerivedFromTheme: [],

  currentStackVersionNumber: function () {
    var regExp = new RegExp(this.get('currentStackName') + '-');
    return (this.get('currentStackVersion') || this.get('defaultStackVersion')).replace(regExp, '');
  }.property('currentStackVersion', 'defaultStackVersion', 'currentStackName'),

  isHadoopWindowsStack: Em.computed.equal('currentStackName', 'HDPWIN'),

  /**
   * If NameNode High Availability is enabled
   * Based on <code>clusterStatus.isInstalled</code>, stack version, <code>SNameNode</code> availability
   *
   * @type {bool}
   */
  isHaEnabled: function () {
    return App.Service.find('HDFS').get('isLoaded')
      && App.MasterComponent.find('SECONDARY_NAMENODE').get('totalCount') === 0
      && App.MasterComponent.find('NAMENODE').get('totalCount') > 1;
  }.property('router.clusterController.dataLoadList.services', 'router.clusterController.isServiceContentFullyLoaded'),

  hasNameNodeFederation: function () {
    return App.HDFSService.find('HDFS').get('masterComponentGroups.length') > 1;
  }.property('router.clusterController.isHostComponentMetricsLoaded', 'router.clusterController.isHDFSNameSpacesLoaded'),

  /**
   * If ResourceManager High Availability is enabled
   * Based on number of ResourceManager host components installed
   *
   * @type {bool}
   */
  isRMHaEnabled: function () {
    var result = false;
    var rmStackComponent = App.StackServiceComponent.find().findProperty('componentName','RESOURCEMANAGER');
    if (rmStackComponent && rmStackComponent.get('isMultipleAllowed')) {
      result = this.HostComponent.find().filterProperty('componentName', 'RESOURCEMANAGER').length > 1;
    }
    return result;
  }.property('router.clusterController.isLoaded', 'isStackServicesLoaded'),

  /**
   * If Ranger Admin High Availability is enabled
   * Based on number of Ranger Admin host components installed
   *
   * @type {bool}
   */
  isRAHaEnabled: function () {
    var result = false;
    var raStackComponent = App.StackServiceComponent.find().findProperty('componentName','RANGER_ADMIN');
    if (raStackComponent && raStackComponent.get('isMultipleAllowed')) {
      result = App.HostComponent.find().filterProperty('componentName', 'RANGER_ADMIN').length > 1;
    }
    return result;
  }.property('router.clusterController.isLoaded', 'isStackServicesLoaded'),

  /**
   * Object with utility functions for list of service names with similar behavior
   */
  services: Em.Object.create({
    all: function () {
      return App.StackService.find().mapProperty('serviceName');
    }.property('App.router.clusterController.isLoaded'),

    clientOnly: function () {
      return App.StackService.find().filterProperty('isClientOnlyService').mapProperty('serviceName');
    }.property('App.router.clusterController.isLoaded'),

    hasClient: function () {
      return App.StackService.find().filterProperty('hasClient').mapProperty('serviceName');
    }.property('App.router.clusterController.isLoaded'),

    hasMaster: function () {
      return App.StackService.find().filterProperty('hasMaster').mapProperty('serviceName');
    }.property('App.router.clusterController.isLoaded'),

    hasSlave: function () {
      return App.StackService.find().filterProperty('hasSlave').mapProperty('serviceName');
    }.property('App.router.clusterController.isLoaded'),

    noConfigTypes: function () {
      return App.StackService.find().filterProperty('isNoConfigTypes').mapProperty('serviceName');
    }.property('App.router.clusterController.isLoaded'),

    servicesWithHeatmapTab: function () {
      return App.StackService.find().filterProperty('hasHeatmapSection').mapProperty('serviceName');
    }.property('App.router.clusterController.isLoaded'),

    monitoring: function () {
      return App.StackService.find().filterProperty('isMonitoringService').mapProperty('serviceName');
    }.property('App.router.clusterController.isLoaded'),

    hostMetrics: function () {
      return App.StackService.find().filterProperty('isHostMetricsService').mapProperty('serviceName');
    }.property('App.router.clusterController.isLoaded'),

    serviceMetrics: function () {
      return App.StackService.find().filterProperty('isServiceMetricsService').mapProperty('serviceName');
    }.property('App.router.clusterController.isLoaded'),

    supportsServiceCheck: function() {
      return App.StackService.find().filterProperty('serviceCheckSupported').mapProperty('serviceName');
    }.property('App.router.clusterController.isLoaded'),

    supportsDeleteViaUI: function() {
      return App.StackService.find().filterProperty('supportDeleteViaUi').mapProperty('serviceName');
    }.property('App.router.clusterController.isLoaded')
  }),

  /**
   * List of components with allowed action for them
   * @type {Em.Object}
   */
  components: Em.Object.create({
    isMasterAddableOnlyOnHA: function () {
      return App.StackServiceComponent.find().filterProperty('isMasterAddableOnlyOnHA').mapProperty('componentName')
    }.property('App.router.clusterController.isLoaded'),

    allComponents: function () {
      return App.StackServiceComponent.find().mapProperty('componentName')
    }.property('App.router.clusterController.isLoaded'),

    reassignable: function () {
      return App.StackServiceComponent.find().filterProperty('isReassignable').mapProperty('componentName')
    }.property('App.router.clusterController.isLoaded'),

    restartable: function () {
      return App.StackServiceComponent.find().filterProperty('isRestartable').mapProperty('componentName')
    }.property('App.router.clusterController.isLoaded'),

    deletable: function () {
      return App.StackServiceComponent.find().filterProperty('isDeletable').mapProperty('componentName')
    }.property('App.router.clusterController.isLoaded'),

    rollinRestartAllowed: function () {
      return App.StackServiceComponent.find().filterProperty('isRollinRestartAllowed').mapProperty('componentName')
    }.property('App.router.clusterController.isLoaded'),

    decommissionAllowed: function () {
      return App.StackServiceComponent.find().filterProperty('isDecommissionAllowed').mapProperty('componentName')
    }.property('App.router.clusterController.isLoaded'),

    refreshConfigsAllowed: function () {
      return App.StackServiceComponent.find().filterProperty('isRefreshConfigsAllowed').mapProperty('componentName')
    }.property('App.router.clusterController.isLoaded'),

    addableToHost: function () {
      return App.StackServiceComponent.find().filterProperty('isAddableToHost').mapProperty('componentName')
    }.property('App.router.clusterController.isLoaded'),

    addableMasterInstallerWizard: function () {
      return App.StackServiceComponent.find().filterProperty('isMasterAddableInstallerWizard').mapProperty('componentName')
    }.property('App.router.clusterController.isLoaded'),

    multipleMasters: function () {
      return App.StackServiceComponent.find().filterProperty('isMasterWithMultipleInstances').mapProperty('componentName')
    }.property('App.router.clusterController.isLoaded'),

    slaves: function () {
      return App.StackServiceComponent.find().filterProperty('isSlave').mapProperty('componentName')
    }.property('App.router.clusterController.isLoaded'),

    masters: function () {
      return App.StackServiceComponent.find().filterProperty('isMaster').mapProperty('componentName')
    }.property('App.router.clusterController.isLoaded'),

    clients: function () {
      return App.StackServiceComponent.find().filterProperty('isClient').mapProperty('componentName')
    }.property('App.router.clusterController.isLoaded'),

    nonHDP: function () {
      return App.StackServiceComponent.find().filterProperty('isNonHDPComponent').mapProperty('componentName')
    }.property('App.router.clusterController.isLoaded')
  })
});
