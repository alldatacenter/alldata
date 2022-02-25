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

App.ApplicationController = Em.Controller.extend(App.Persist, {

  name: 'applicationController',

  isPollerRunning: false,

  clusterName: Em.computed.alias('App.router.clusterController.clusterName'),

  /**
   * set ambari server version from installerController or mainController, making sure version shown up all the time
   */
  ambariVersion: function () {
    return App.router.get('installerController.ambariServerVersion') || App.router.get('mainController.ambariServerVersion') || Em.I18n.t('common.notAvailable');
  }.property('App.router.installerController.ambariServerVersion', 'App.router.mainController.ambariServerVersion'),

  clusterDisplayName: Em.computed.truncate('clusterName', 13, 10),

  isClusterDataLoaded: Em.computed.and('App.router.clusterController.isLoaded','App.router.loggedIn'),

  isExistingClusterDataLoaded: Em.computed.and('App.router.clusterInstallCompleted', 'isClusterDataLoaded'),

  enableLinks: Em.computed.and('isExistingClusterDataLoaded', '!App.isOnlyViewUser'),

  /**
   * Determines if "Exit" menu-item should be shown
   * It should if cluster isn't installed
   * If cluster is installer, <code>isClusterDataLoaded</code> is checked
   * @type {boolean}
   */
  showExitLink: function () {
    if (App.router.get('clusterInstallCompleted')) {
      return this.get('isClusterDataLoaded');
    }
    return true;
  }.property('App.router.clusterInstallCompleted', 'isClusterDataLoaded'),

  /**
   * Determines if "Manage Ambari" menu-item should be shown
   *
   * @type {boolean}
   */
  showManageAmbari: function () {
    if (App.router.get('clusterInstallCompleted')) {
      return this.get('isClusterDataLoaded');
    }
    return App.get('isPermissionDataLoaded');
  }.property('App.router.clusterInstallCompleted', 'isClusterDataLoaded', 'App.isPermissionDataLoaded'),

  /**
   * Determines if upgrade label should be shown
   *
   * @type {boolean}
   */
  showUpgradeLabel: Em.computed.or('App.upgradeInProgress', 'App.upgradeHolding', 'App.upgradeSuspended'),

  /**
   * @return {{msg: string, cls: string, icon: string}}
   */
  upgradeMap: function () {
    var upgradeInProgress = App.get('upgradeInProgress');
    var upgradeHolding = App.get('upgradeHolding');
    var upgradeSuspended = App.get('upgradeSuspended');
    var isDowngrade = App.router.get('mainAdminStackAndUpgradeController.isDowngrade');
    var typeSuffix = isDowngrade ? 'downgrade' : 'upgrade';
    var hasUpgradePrivilege = App.isAuthorized('CLUSTER.UPGRADE_DOWNGRADE_STACK');
    var wizardWatcherController = App.router.get('wizardWatcherController');
    var isNotWizardUser;
    var wizardUserName;
    if (upgradeInProgress) {
      isNotWizardUser =wizardWatcherController.get('isNonWizardUser');
      wizardUserName = wizardWatcherController.get('wizardUser');
      return {
        cls: hasUpgradePrivilege? 'upgrade-in-progress' : 'upgrade-in-progress not-allowed-cursor',
        icon: 'glyphicon-cog',
        msg: isNotWizardUser ?  Em.I18n.t('admin.stackVersions.version.' + typeSuffix + '.running.nonWizard').format(wizardUserName) : Em.I18n.t('admin.stackVersions.version.' + typeSuffix + '.running')
      }
    }
    if (upgradeHolding) {
      return {
        cls: hasUpgradePrivilege? 'upgrade-holding' : 'upgrade-holding not-allowed-cursor',
        icon: 'glyphicon-pause',
        msg: Em.I18n.t('admin.stackVersions.version.' + typeSuffix + '.pause')
      }
    }
    if (upgradeSuspended) {
      return {
        cls: hasUpgradePrivilege? 'upgrade-aborted' : 'upgrade-aborted not-allowed-cursor',
        icon: 'glyphicon-pause',
        msg: Em.I18n.t('admin.stackVersions.version.' + typeSuffix + '.suspended')
      }
    }
    return {};
  }.property('App.upgradeInProgress', 'App.upgradeHolding', 'App.upgradeSuspended', 'App.router.mainAdminStackAndUpgradeController.isDowngrade'),

  startKeepAlivePoller: function() {
    if (!this.get('isPollerRunning')) {
     this.set('isPollerRunning',true);
      App.updater.run(this, 'getStack', 'isPollerRunning', App.sessionKeepAliveInterval);
    }
  },

  getStack: function(callback) {
    App.ajax.send({
      name: 'router.login.clusters',
      sender: this,
      callback: callback
    });
  },

  goToAdminView: function () {
    App.router.route("adminView");
  },

  goToDashboard: function () {
    if (this.get('enableLinks')) {
      App.router.route("main/dashboard");
    }
  },

  showAboutPopup: function() {
    App.ModalPopup.show({
      header: Em.I18n.t('common.aboutAmbari'),
      secondary: false,
      bodyClass: Em.View.extend({
        templateName: require('templates/common/about'),
        ambariVersion: this.get('ambariVersion')
      })
    });    
  }

});