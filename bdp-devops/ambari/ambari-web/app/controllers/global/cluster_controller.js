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
var stringUtils = require('utils/string_utils');
var credentialUtils = require('utils/credentials');

App.ClusterController = Em.Controller.extend(App.ReloadPopupMixin, {
  name: 'clusterController',
  isLoaded: false,
  ambariProperties: null,
  clusterEnv: null,
  clusterDataLoadedPercent: 'width:0', // 0 to 1

  isClusterNameLoaded: false,

  isAlertsLoaded: false,

  isComponentsStateLoaded: false,

  isHostsLoaded: false,

  isConfigsPropertiesLoaded: false,

  isComponentsConfigLoaded: false,

  isStackConfigsLoaded: false,

  isServiceMetricsLoaded: false,

  /**
   * @type {boolean}
   */
  isHostComponentMetricsLoaded: false,

  isHDFSNameSpacesLoaded: false,

  /**
   * This counter used as event trigger to notify that quick links should be changed.
   */
  quickLinksUpdateCounter: 0,

  /**
   * Ambari uses custom jdk.
   * @type {Boolean}
   */
  isCustomJDK: false,

  isHostContentLoaded: Em.computed.and('isHostsLoaded', 'isComponentsStateLoaded'),

  isServiceContentFullyLoaded: Em.computed.and('isServiceMetricsLoaded', 'isComponentsStateLoaded', 'isComponentsConfigLoaded'),

  isStackVersionsLoaded: false,

  clusterName: Em.computed.alias('App.clusterName'),

  updateLoadStatus: function (item) {
    var loadList = this.get('dataLoadList');
    var loaded = true;
    var numLoaded = 0;
    var loadListLength = 0;
    loadList.set(item, true);
    for (var i in loadList) {
      if (loadList.hasOwnProperty(i)) {
        loadListLength++;
        if (!loadList[i] && loaded) {
          loaded = false;
        }
      }
      // calculate the number of true
      if (loadList.hasOwnProperty(i) && loadList[i]) {
        numLoaded++;
      }
    }
    this.set('isLoaded', loaded);
    this.set('clusterDataLoadedPercent', 'width:' + (Math.floor(numLoaded / loadListLength * 100)).toString() + '%');
  },

  dataLoadList: Em.Object.create({
    'stackComponents': false,
    'services': false
  }),

  /**
   * load cluster name
   */
  loadClusterName: function (reload, deferred) {
    var dfd = deferred || $.Deferred();

    if (App.get('clusterName') && App.get('clusterId') && !reload) {
      App.set('clusterName', this.get('clusterName'));
      this.set('isClusterNameLoaded', true);
      dfd.resolve();
    } else {
      App.ajax.send({
        name: 'cluster.load_cluster_name',
        sender: this,
        data: {
          reloadPopupText: Em.I18n.t('app.reloadPopup.noClusterName.text'),
          errorLogMessage: 'failed on loading cluster name',
          callback: this.loadClusterName,
          args: [reload, dfd],
          shouldUseDefaultHandler: true
        },
        success: 'reloadSuccessCallback',
        error: 'reloadErrorCallback',
        callback: function () {
          if (!App.get('currentStackVersion')) {
            App.set('currentStackVersion', App.defaultStackVersion);
          }
        }
      }).then(
        function () {
          dfd.resolve();
        },
        null
      );
    }
    return dfd.promise();
  },

  reloadSuccessCallback: function (data) {
    this._super();
    if (data.items && data.items.length > 0) {
      App.setProperties({
        clusterId: data.items[0].Clusters.cluster_id,
        clusterName: data.items[0].Clusters.cluster_name,
        currentStackVersion: data.items[0].Clusters.version,
        isKerberosEnabled: data.items[0].Clusters.security_type === 'KERBEROS'
      });
      this.set('isClusterNameLoaded', true);
    }
  },

  setServerClock: function (data) {
    var clientClock = new Date().getTime();
    var serverClock = (Em.getWithDefault(data, 'RootServiceComponents.server_clock', '')).toString();
    serverClock = serverClock.length < 13 ? serverClock + '000' : serverClock;
    App.set('clockDistance', serverClock - clientClock);
    App.set('currentServerTime', parseInt(serverClock));
  },

  getServerClockErrorCallback: Em.K,

  getUrl: function (testUrl, url) {
    return (App.get('testMode')) ? testUrl : App.get('apiPrefix') + '/clusters/' + App.get('clusterName') + url;
  },

  /**
   *  load all data and update load status
   */
  loadClusterData: function () {
    this.loadAuthorizations();
    this.getAllHostNames();

    if (!App.get('clusterName')) {
      return;
    }

    if (this.get('isLoaded')) { // do not load data repeatedly
      App.router.get('mainController').startPolling();
      return;
    }
    App.router.get('userSettingsController').getAllUserSettings();
    App.router.get('errorsHandlerController').loadErrorLogs();

    this.loadClusterInfo();
    this.restoreUpgradeState();
    App.router.get('wizardWatcherController').getUser();

    this.loadClusterDataToModel();

    //force clear filters  for hosts page to load all data
    App.db.setFilterConditions('mainHostController', null);
  },

  loadClusterInfo: function() {
    var clusterUrl = this.getUrl('/data/clusters/cluster.json', '?fields=Clusters');
    App.HttpClient.get(clusterUrl, App.clusterMapper, {
      complete: function (jqXHR, textStatus) {
        App.set('isCredentialStorePersistent', Em.getWithDefault(App.Cluster.find().findProperty('clusterName', App.get('clusterName')), 'isCredentialStorePersistent', false));
      }
    }, Em.K);
  },

  /**
   * Order of loading:
   * 1. load all created service components
   * 2. request for service components supported by stack
   * 3. load stack components to model
   * 4. request for services
   * 5. put services in cache
   * 6. request for hosts and host-components (single call)
   * 7. request for service metrics
   * 8. load host-components to model
   * 9. load services from cache with metrics to model
   */
  loadClusterDataToModel: function() {
    var self = this;

    this.loadStackServiceComponents(function (data) {
      data.items.forEach(function (service) {
        service.StackServices.is_selected = true;
        service.StackServices.is_installed = false;
      }, self);
      App.stackServiceMapper.mapStackServices(data);
      App.config.setPreDefinedServiceConfigs(true);
      self.updateLoadStatus('stackComponents');
      self.loadServicesAndComponents();
    });
  },

  loadServicesAndComponents: function() {
    var updater = App.router.get('updateController');
    var self = this;

    updater.updateServices(function () {
      self.updateLoadStatus('services');

      //hosts should be loaded after services in order to properly populate host-component relation in App.cache.services
      updater.updateHost(function () {
        self.set('isHostsLoaded', true);
        self.loadAlerts();
      });
      self.loadConfigProperties();
      // components state loading doesn't affect overall progress
      updater.updateComponentsState(function () {
        self.set('isComponentsStateLoaded', true);
        // service metrics should be loaded after components state for mapping service components to service in the DS model
        // service metrics loading doesn't affect overall progress
        updater.updateServiceMetric(function () {
          self.set('isServiceMetricsLoaded', true);
          // make second call, because first is light since it doesn't request host-component metrics
          updater.updateServiceMetric(function() {
            self.set('isHostComponentMetricsLoaded', true);
            updater.updateHDFSNameSpaces();
          });
          // components config loading doesn't affect overall progress
          self.loadComponentWithStaleConfigs(function () {
            self.set('isComponentsConfigLoaded', true);
          });
        });
      });
    });
  },

  loadComponentWithStaleConfigs: function (callback) {
    return App.ajax.send({
      name: 'components.get.staleConfigs',
      sender: this,
      success: 'loadComponentWithStaleConfigsSuccessCallback',
      callback: callback
    });
  },

  loadComponentWithStaleConfigsSuccessCallback: function(json) {
    json.items.forEach((item) => {
      const componentName = item.ServiceComponentInfo.component_name;
      const hosts = item.host_components.mapProperty('HostRoles.host_name') || [];
      App.componentsStateMapper.updateStaleConfigsHosts(componentName, hosts);
    });
  },

  loadConfigProperties: function() {
    var self = this;

    App.config.loadConfigsFromStack(App.Service.find().mapProperty('serviceName')).always(function () {
      App.config.loadClusterConfigsFromStack().always(function () {
        App.router.get('configurationController').updateConfigTags().always(() => {
          App.router.get('updateController').updateClusterEnv().always(() => {
            self.set('isConfigsPropertiesLoaded', true);
          });
        });
      });
    });
  },

  loadAlerts: function() {
    var updater = App.router.get('updateController');
    var self = this;

    console.time('Overall alerts loading time');
    updater.updateAlertGroups(function () {
      updater.updateAlertDefinitions(function () {
        updater.updateAlertDefinitionSummary(function () {
          updater.updateUnhealthyAlertInstances(function () {
            console.timeEnd('Overall alerts loading time');
            self.set('isAlertsLoaded', true);
          });
        });
      });
    });
  },

  /**
   * restore upgrade status from server
   * and make call to get latest status from server
   * Also loading all upgrades to App.StackUpgradeHistory model
   */
  restoreUpgradeState: function () {
    var self = this;
    return this.getAllUpgrades().done(function (data) {
      var upgradeController = App.router.get('mainAdminStackAndUpgradeController');
      var allUpgrades = data.items.sortProperty('Upgrade.request_id');
      var lastUpgradeData = allUpgrades.pop();
      if (lastUpgradeData){
        var status = lastUpgradeData.Upgrade.request_status;
        var lastUpgradeNotFinished = (self.isSuspendedState(status) || self.isRunningState(status));
        if (lastUpgradeNotFinished){
          /**
           * No need to display history if there is only one running or suspended upgrade.
           * Because UI still needs to provide user the option to resume the upgrade via the Upgrade Wizard UI.
           * If there is more than one upgrade. Show/Hive the tab based on the status.
           */
          var hasFinishedUpgrades = allUpgrades.some(function (item) {
            var status = item.Upgrade.request_status;
            if (!self.isRunningState(status)){
              return true;
            }
          }, self);
          App.set('upgradeHistoryAvailable', hasFinishedUpgrades);
        } else {
          //There is at least one finished upgrade. Display it.
          App.set('upgradeHistoryAvailable', true);
        }
      } else {
        //There is no upgrades at all.
        App.set('upgradeHistoryAvailable', false);
      }

      //completed upgrade shouldn't be restored
      if (lastUpgradeData) {
        if (lastUpgradeData.Upgrade.request_status !== "COMPLETED") {
          upgradeController.restoreLastUpgrade(lastUpgradeData);
        }
      } else {
        upgradeController.initDBProperties();
      }

      App.stackUpgradeHistoryMapper.map(data);
      upgradeController.loadStackVersionsToModel(true).done(function () {
        upgradeController.loadCompatibleVersions();
        upgradeController.updateCurrentStackVersion();
        App.set('stackVersionsAvailable', App.StackVersion.find().content.length > 0);
        self.set('isStackVersionsLoaded', true);
      });
    });
  },

  isRunningState: function(status){
    if (status) {
      return "IN_PROGRESS" === status || "PENDING" === status || status.contains("HOLDING");
    } else {
      //init state
      return true;
    }
  },

  /**
   * ABORTED should be handled as SUSPENDED for the lastUpgradeItem
   * */
  isSuspendedState: function(status){
    return "ABORTED" === status;
  },

  requestHosts: function (realUrl, callback) {
    var testHostUrl = '/data/hosts/HDP2/hosts.json';
    var url = this.getUrl(testHostUrl, realUrl);
    App.HttpClient.get(url, App.hostsMapper, {
      complete: callback
    }, callback)
  },

  /**
   *
   * @param callback
   * @returns {?object}
   */
  loadStackServiceComponents: function (callback) {
    var callbackObj = {
      loadStackServiceComponentsSuccess: callback
    };
    return App.ajax.send({
      name: 'wizard.service_components',
      data: {
        stackUrl: App.get('stackVersionURL'),
        stackVersion: App.get('currentStackVersionNumber')
      },
      sender: callbackObj,
      success: 'loadStackServiceComponentsSuccess'
    });
  },

  loadAmbariProperties: function () {
    return App.ajax.send({
      name: 'ambari.service',
      sender: this,
      success: 'loadAmbariPropertiesSuccess',
      error: 'loadAmbariPropertiesError'
    });
  },

  loadAuthorizations: function() {
    return App.ajax.send({
      name: 'router.user.authorizations',
      sender: this,
      data: {userName: App.db.getLoginName()},
      success: 'loadAuthorizationsSuccessCallback'
    });
  },

  loadAuthorizationsSuccessCallback: function(response) {
    if (response && response.items) {
      App.set('auth', response.items.mapProperty('AuthorizationInfo.authorization_id').uniq());
      App.db.setAuth(App.get('auth'));
    }
  },

  loadAmbariPropertiesSuccess: function (data) {
    var mainController = App.router.get('mainController');
    this.set('ambariProperties', data.RootServiceComponents.properties);
    // Absence of 'jdk.name' and 'jce.name' properties says that ambari configured with custom jdk.
    this.set('isCustomJDK', App.isEmptyObject(App.permit(data.RootServiceComponents.properties, ['jdk.name', 'jce.name'])));
    this.setServerClock(data);
    mainController.setAmbariServerVersion.call(mainController, data);
    mainController.monitorInactivity();
  },

  loadAmbariPropertiesError: Em.K,

  updateClusterData: function () {
    var testUrl = '/data/clusters/HDP2/cluster.json';
    var clusterUrl = this.getUrl(testUrl, '?fields=Clusters');
    App.HttpClient.get(clusterUrl, App.clusterMapper, {
      complete: function () {
      }
    });
  },

  /**
   *
   * @returns {*|Transport|$.ajax|boolean|ServerResponse}
   */
  getAllHostNames: function () {
    return App.ajax.send({
      name: 'hosts.all',
      sender: this,
      success: 'getHostNamesSuccess',
      error: 'getHostNamesError'
    });
  },

  getHostNamesSuccess: function (data) {
    App.set("allHostNames", data.items.mapProperty("Hosts.host_name"));
  },

  getHostNamesError: Em.K,


  /**
   * puts kerberos admin credentials in the live cluster session
   * and resend ajax request
   * @param {credentialResourceObject} credentialResource
   * @param {object} ajaxOpt
   * @returns {$.ajax}
   */
  createKerberosAdminSession: function (credentialResource, ajaxOpt) {
    return credentialUtils.createOrUpdateCredentials(App.get('clusterName'), credentialUtils.ALIAS.KDC_CREDENTIALS, credentialResource).then(function() {
      if (ajaxOpt) {
        $.ajax(ajaxOpt);
      }
    });
  },

  //TODO Replace this check with any other which is applicable to non-HDP stack
  /**
   * Check if HDP stack version is more or equal than 2.2.2 to determine if pluggable metrics for Storm are supported
   * @method checkDetailedRepoVersion
   * @returns {promise|*|promise|promise|HTMLElement|promise}
   */
  checkDetailedRepoVersion: function () {
    var dfd;
    var currentStackName = App.get('currentStackName');
    var currentStackVersionNumber = App.get('currentStackVersionNumber');
    if (currentStackName == 'HDP' && currentStackVersionNumber == '2.2') {
      dfd = App.ajax.send({
        name: 'cluster.load_detailed_repo_version',
        sender: this,
        success: 'checkDetailedRepoVersionSuccessCallback',
        error: 'checkDetailedRepoVersionErrorCallback'
      });
    } else {
      dfd = $.Deferred();
      App.set('isStormMetricsSupported', currentStackName != 'HDP' || stringUtils.compareVersions(currentStackVersionNumber, '2.2') == 1);
      dfd.resolve();
    }
    return dfd.promise();
  },

  checkDetailedRepoVersionSuccessCallback: function (data) {
    var rv = (Em.getWithDefault(data, 'items', []) || []).filter(function(i) {
      return Em.getWithDefault(i || {}, 'ClusterStackVersions.stack', null) === App.get('currentStackName') &&
        Em.getWithDefault(i || {}, 'ClusterStackVersions.version', null) === App.get('currentStackVersionNumber');
    })[0];
    var version = Em.getWithDefault(rv || {}, 'repository_versions.0.RepositoryVersions.repository_version', false);
    App.set('isStormMetricsSupported', stringUtils.compareVersions(version, '2.2.2') > -1 || !version);
  },
  checkDetailedRepoVersionErrorCallback: function () {
    App.set('isStormMetricsSupported', true);
  },

  /**
   * Load required data for all upgrades from API
   * @returns {$.ajax}
   */
  getAllUpgrades: function () {
    return App.ajax.send({
      name: 'cluster.load_last_upgrade',
      sender: this
    });
  },

  triggerQuickLinksUpdate: function() {
    this.incrementProperty('quickLinksUpdateCounter');
  }
});
