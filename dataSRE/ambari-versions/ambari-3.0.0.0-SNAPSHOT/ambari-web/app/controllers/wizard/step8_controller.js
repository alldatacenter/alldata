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
var fileUtils = require('utils/file_utils');

App.WizardStep8Controller = Em.Controller.extend(App.AddSecurityConfigs, App.wizardDeployProgressControllerMixin, App.ConfigOverridable, App.ConfigsSaverMixin, {

  name: 'wizardStep8Controller',

  /**
   * @type {boolean}
   */
  isAddService: Em.computed.equal('content.controllerName', 'addServiceController'),

  /**
   * @type {boolean}
   */
  isAddHost: Em.computed.equal('content.controllerName', 'addHostController'),

  /**
   * @type {boolean}
   */
  isInstaller: Em.computed.equal('content.controllerName', 'installerController'),

  /**
   * List of raw data about cluster that should be displayed
   * @type {Array}
   */
  rawContent: [
    {
      config_name: 'Admin',
      display_name: 'Admin Name',
      config_value: ''
    },
    {
      config_name: 'cluster',
      display_name: 'Cluster Name',
      config_value: ''
    },
    {
      config_name: 'hosts',
      display_name: 'Total Hosts',
      config_value: ''
    },
    {
      config_name: 'Repo',
      display_name: 'Local Repository',
      config_value: ''
    }
  ],

  /**
   * List of data about cluster (based on formatted <code>rawContent</code>)
   * @type {Object[]}
   */
  clusterInfo: [],

  /**
   * List of services with components assigned to hosts
   * @type {Object[]}
   */
  services: [],

  /**
   * @type {Object[]}
   */
  configs: [],

  /**
   * True if Kerberos is installed on the cluster and the kdc_type on the server is set to "none"
   * @type {Boolean}
   */
  isManualKerberos: Em.computed.equal('App.router.mainAdminKerberosController.kdc_type', 'none'),

  showDownloadCsv: function () {
    return !!App.get('router.mainAdminKerberosController.kdc_type')
  }.property('App.router.mainAdminKerberosController.kdc_type'),


  /**
   * Should Submit button be disabled
   * @type {bool}
   */
  isSubmitDisabled: false,

  /**
   * Should Back button be disabled
   * @type {bool}
   */
  isBackBtnDisabled: false,

  /**
   * Is error appears while <code>ajaxQueue</code> executes
   * @type {bool}
   */
  hasErrorOccurred: false,

  /**
   * Are services installed
   * Used to hide Deploy Progress Bar
   * @type {bool}
   */
  servicesInstalled: false,

  /**
   * List of service config tags
   * @type {Object[]}
   */
  serviceConfigTags: [],

  /**
   * Selected config group
   * @type {Object}
   */
  selectedConfigGroup: null,

  /**
   * List of config groups
   * @type {Object[]}
   */
  configGroups: [],

  /**
   * List of selected but not installed services
   * @type {Object[]}
   */
  selectedServices: function () {
    return this.get('content.services').filterProperty('isSelected', true).filterProperty('isInstalled', false);
  }.property('content.services.@each.isSelected','content.services.@each.isInstalled').cacheable(),

  /**
   * List of installed services
   * @type {Object[]}
   */
  installedServices: Em.computed.filterBy('content.services', 'isInstalled', true),

  /**
   * Current cluster name
   * @type {string}
   */
  clusterName: Em.computed.alias('content.cluster.name'),

  /**
   * List of existing cluster names
   * @type {string[]}
   */
  clusterNames: [],

  /**
   * Number of completed cluster delete requests
   * @type {number}
   */
  clusterDeleteRequestsCompleted: 0,

  /**
   * Number of existing repo_versions
   * @type {number}
   */
  existingRepositoryVersions: 0,

  /**
   * Indicates if all cluster delete requests are completed
   * @type {boolean}
   */
  isAllClusterDeleteRequestsCompleted: Em.computed.equalProperties('clusterDeleteRequestsCompleted', 'clusterNames.length'),

  /**
   * Error popup body views for clusters that couldn't be deleted
   * @type {App.AjaxDefaultErrorPopupBodyView[]}
   */
  clusterDeleteErrorViews: [],

  /**
   * Clear current step data
   * @method clearStep
   */
  clearStep: function () {
    this.get('services').clear();
    this.get('configs').clear();
    this.get('clusterInfo').clear();
    this.get('serviceConfigTags').clear();
    this.set('servicesInstalled', false);
    this.set('ajaxQueueLength', 0);
    this.set('ajaxRequestsQueue', App.ajaxQueue.create());
    this.set('ajaxRequestsQueue.finishedCallback', this.ajaxQueueFinished);
    this.get('clusterDeleteErrorViews').clear();
    this.set('clusterDeleteRequestsCompleted', 0);
  },

  /**
   * Load current step data
   * @method loadStep
   */
  loadStep: function () {
    this.clearStep();
    if (this.get('content.serviceConfigProperties')) {
      this.formatProperties();
      this.loadConfigs();
    }
    this.loadClusterInfo();
    this.loadServices();
    this.set('isSubmitDisabled', false);
    this.set('isBackBtnDisabled', false);
  },

  /**
   * replace whitespace character with coma between directories
   * @method formatProperties
   */
  formatProperties: function () {
    this.get('content.serviceConfigProperties').forEach(function (_configProperty) {
      _configProperty.value = typeof _configProperty.value === "boolean"
        ? _configProperty.value.toString() : App.config.trimProperty(_configProperty, false);
    });
  },

  /**
   * Load all site properties
   * @method loadConfigs
   */
  loadConfigs: function () {
    this.set('configs', this.get('content.serviceConfigProperties').filter(function (config) {
      return !config.group;
    }));
  },

  /**
   * Format <code>content.hosts</code> from Object to Array
   * @returns {Array}
   * @method getRegisteredHosts
   */
  getRegisteredHosts: function () {
    var allHosts = this.get('content.hosts');
    var hosts = [];
    for (var hostName in allHosts) {
      if (allHosts.hasOwnProperty(hostName)) {
        if (allHosts[hostName].bootStatus === 'REGISTERED') {
          allHosts[hostName].hostName = allHosts[hostName].name;
          hosts.pushObject(allHosts[hostName]);
        }
      }
    }
    return hosts;
  },

  /**
   * Load all info about cluster to <code>clusterInfo</code> variable
   * @method loadClusterInfo
   */
  loadClusterInfo: function () {

    //Admin name
    var admin = this.rawContent.findProperty('config_name', 'Admin');
    admin.config_value = App.db.getLoginName();
    if (admin.config_value) {
      this.get('clusterInfo').pushObject(Ember.Object.create(admin));
    }

    // cluster name
    var cluster = this.rawContent.findProperty('config_name', 'cluster');
    cluster.config_value = this.get('content.cluster.name');
    this.get('clusterInfo').pushObject(Ember.Object.create(cluster));

    //hosts
    var newHostsCount = 0;
    var totalHostsCount = 0;
    var hosts = this.get('content.hosts');
    for (var hostName in hosts) {
      newHostsCount += ~~!hosts[hostName].isInstalled;
      totalHostsCount++;
    }

    var totalHostsObj = this.rawContent.findProperty('config_name', 'hosts');
    totalHostsObj.config_value = totalHostsCount + ' (' + newHostsCount + ' new)';
    this.get('clusterInfo').pushObject(Em.Object.create(totalHostsObj));

    //repo
    if (this.get('isAddService') || this.get('isAddHost')) {
      // For some stacks there is no info regarding stack versions to upgrade, e.g. HDP-2.1
      if (App.StackVersion.find().get('content.length')) {
        this.loadRepoInfo();
      } else {
        this.loadDefaultRepoInfo();
      }
    } else {
      // from install wizard
      var selectedStack = App.Stack.find().findProperty('isSelected', true);
      var allRepos = [];
      if (selectedStack && selectedStack.get('operatingSystems')) {
        selectedStack.get('operatingSystems').forEach(function (os) {
          if (os.get('isSelected')) {
            os.get('repositories').forEach(function(repo) {
              if (repo.get('showRepo')) {
                allRepos.push(Em.Object.create({
                  base_url: repo.get('baseUrl'),
                  os_type: repo.get('osType'),
                  repo_id: repo.get('repoId')
                }));
              }
            }, this);
          }
        }, this);
      }
      allRepos.set('display_name', Em.I18n.t("installer.step8.repoInfo.displayName"));
      this.get('clusterInfo').set('useRedhatSatellite', selectedStack.get('useRedhatSatellite'));
      this.get('clusterInfo').set('repoInfo', allRepos);
    }
  },

  /**
   * Load repo info for add Service/Host wizard review page
   * @return {$.ajax|null}
   * @method loadRepoInfo
   */
  loadRepoInfo: function () {
    var stackName = App.get('currentStackName');

    var currentRepoVersion;
    App.RepositoryVersion.find().forEach(function (repoVersion) {
      if (repoVersion.get('stackVersionType') === stackName && repoVersion.get('isCurrent') && repoVersion.get('isStandard')) {
        currentRepoVersion = repoVersion.get('repositoryVersion');
      }
    });

    if (!currentRepoVersion) {
      console.error('Error while getting current stack repository version');
    }

    return App.ajax.send({
      name: 'cluster.load_repo_version',
      sender: this,
      data: {
        stackName: stackName,
        repositoryVersion: currentRepoVersion
      },
      success: 'loadRepoInfoSuccessCallback',
      error: 'loadRepoInfoErrorCallback'
    });
  },

  /**
   * Save all repo base URL of all OS type to <code>repoInfo<code>
   * @param {object} data
   * @method loadRepoInfoSuccessCallback
   */
  loadRepoInfoSuccessCallback: function (data) {
    Em.assert('Current repo-version may be only one', data.items.length === 1);
    if (data.items.length) {
      var allRepos = this.generateRepoInfo(Em.getWithDefault(data, 'items.0.repository_versions.0.operating_systems', []));
      allRepos.set('display_name', Em.I18n.t("installer.step8.repoInfo.displayName"));
      this.get('clusterInfo').set('repoInfo', allRepos);
      //if the property is missing, set as false
      this.get('clusterInfo').set('useRedhatSatellite', data.items[0].repository_versions[0].operating_systems[0].OperatingSystems.ambari_managed_repositories === false);
    } else {
      this.loadDefaultRepoInfo();
    }
  },

  /**
   * Generate list regarding info about OS versions and repositories.
   *
   * @param {Object{}} oses - OS array
   * @returns {Em.Object[]}
   */
  generateRepoInfo: function(oses) {
    return oses.map(function(os) {
      return os.repositories.map(function (repository) {
        return Em.Object.create({
          base_url: repository.Repositories.base_url,
          os_type: repository.Repositories.os_type,
          repo_id: repository.Repositories.repo_id
        });
      });
    }).reduce(function(p, c) { return p.concat(c); });
  },

  /**
   * Load repo info from stack. Used if installed stack doesn't have upgrade info.
   *
   * @returns {$.Deferred}
   * @method loadDefaultRepoInfo
   */
  loadDefaultRepoInfo: function() {
    var nameVersionCombo = App.get('currentStackVersion').split('-');

    return App.ajax.send({
      name: 'cluster.load_repositories',
      sender: this,
      data: {
        stackName: nameVersionCombo[0],
        stackVersion: nameVersionCombo[1]
      },
      success: 'loadDefaultRepoInfoSuccessCallback',
      error: 'loadRepoInfoErrorCallback'
    });
  },

  /**
   * @param {Object} data - JSON data from server
   * @method loadDefaultRepoInfoSuccessCallback
   */
  loadDefaultRepoInfoSuccessCallback: function (data) {
    var allRepos = this.generateRepoInfo(Em.getWithDefault(data, 'items', []));
    allRepos.set('display_name', Em.I18n.t("installer.step8.repoInfo.displayName"));
    this.get('clusterInfo').set('repoInfo', allRepos);
    //if the property is missing, set as false
    this.get('clusterInfo').set('useRedhatSatellite', data.items[0].OperatingSystems.ambari_managed_repositories === false);
  },

  /**
   * @method loadRepoInfoErrorCallback
   */
  loadRepoInfoErrorCallback: function () {
    var allRepos = [];
    allRepos.set('display_name', Em.I18n.t("installer.step8.repoInfo.displayName"));
    this.get('clusterInfo').set('repoInfo', allRepos);
  },

  /**
   * Load all info about services to <code>services</code> variable
   * @method loadServices
   */
  loadServices: function () {
    this.get('selectedServices').filterProperty('isHiddenOnSelectServicePage', false).forEach(function (service) {
      var serviceObj = Em.Object.create({
        service_name: service.get('serviceName'),
        display_name: service.get('displayNameOnSelectServicePage'),
        service_components: Em.A([])
      });
      service.get('serviceComponents').forEach(function (component) {
        // show clients for services that have only clients components
        if ((component.get('isClient') || component.get('isRequiredOnAllHosts')) && !service.get('isClientOnlyService')) return;
        // no HA component
        if (component.get('isHAComponentOnly')) return;
        // skip if component is not allowed on single node cluster
        if (Object.keys(this.get('content.hosts')).length === 1 && component.get('isNotAllowedOnSingleNodeCluster')) return;
        var displayName;
        if (component.get('isClient')) {
          displayName = Em.I18n.t('common.clients')
        } else {
          // remove service name from component display name
          displayName = App.format.role(component.get('componentName'), false).replace(new RegExp('^' + service.get('serviceName') + '\\s', 'i'), '');
        }

        var componentName = component.get('componentName');
        var masterComponents = this.get('content.masterComponentHosts');
        var isMasterComponentSelected = masterComponents.someProperty('component', componentName);
        var isMaster = component.get('isMaster');

        if (!isMaster || isMasterComponentSelected) {
          serviceObj.get('service_components').pushObject(Em.Object.create({
            component_name: component.get('isClient') ? Em.I18n.t('common.client').toUpperCase() : component.get('componentName'),
            display_name: displayName,
            component_value: this.assignComponentHosts(component)
          }));
        }
      }, this);
      if (service.get('customReviewHandler')) {
        for (var displayName in service.get('customReviewHandler')) {
          serviceObj.get('service_components').pushObject(Em.Object.create({
            display_name: displayName,
            component_value: this.assignComponentHosts(Em.Object.create({
              customHandler: service.get('customReviewHandler.' + displayName)
            }))
          }));
        }
      }
      this.get('services').pushObject(serviceObj);
    }, this);
  },

  /**
   * Set <code>component_value</code> property to <code>component</code>
   * @param {Em.Object} component
   * @return {String}
   * @method assignComponentHosts
   */
  assignComponentHosts: function (component) {
    var componentValue;
    if (component.get('customHandler')) {
      componentValue = this[component.get('customHandler')].call(this, component);
    }
    else {
      if (component.get('isMaster')) {
        componentValue = this.getMasterComponentValue(component.get('componentName'));
      }
      else {
        var componentName = component.get('isClient') ? Em.I18n.t('common.client').toUpperCase() : component.get('componentName');
        var hostsLength = this.get('content.slaveComponentHosts')
          .findProperty('componentName', componentName).hosts.length;
        componentValue = hostsLength + Em.I18n.t('installer.step8.host' + (hostsLength > 1 ? 's' : ''));
      }
    }
    return componentValue;
  },

  getMasterComponentValue: function (componentName) {
    var masterComponents = this.get('content.masterComponentHosts');
    var hostsCount = masterComponents.filterProperty('component', componentName).length;
    return stringUtils.pluralize(hostsCount,
      masterComponents.findProperty('component', componentName).hostName,
        hostsCount + ' ' + Em.I18n.t('installer.step8.hosts'));
  },

  loadHiveDbValue: function() {
    return this.loadDbValue('HIVE');
  },

  loadOozieDbValue: function() {
    return this.loadDbValue('OOZIE');
  },

  /**
   * Set displayed Hive DB value based on DB type
   * @method loadHiveDbValue
   */
  loadDbValue: function (serviceName) {
    var serviceConfigProperties = this.get('content.serviceConfigProperties');
    var dbFull = serviceConfigProperties.findProperty('name', serviceName.toLowerCase() + '_database');
      //db = serviceConfigProperties.findProperty('name', serviceName.toLowerCase() + '_ambari_database');
    //since db.value contains the intial default value of <service>_admin_database (MySQL) and not the actual db type selected,
    //ignore the value when displaying the database name on the summary page
    return dbFull ? dbFull.value : '';
  },

  /**
   * Set displayed HBase master value
   * @param {Object} hbaseMaster
   * @method loadHbaseMasterValue
   */
  loadHbaseMasterValue: function (hbaseMaster) {
    var hbaseHostName = this.get('content.masterComponentHosts').filterProperty('component', hbaseMaster.component_name);
    if (hbaseHostName.length === 1) {
      hbaseMaster.set('component_value', hbaseHostName[0].hostName);
    } else {
      hbaseMaster.set('component_value', hbaseHostName[0].hostName + " " + Em.I18n.t('installer.step8.other').format(hbaseHostName.length - 1));
    }
  },

  /**
   * Set displayed ZooKeeper Server value
   * @param {Object} serverComponent
   * @method loadZkServerValue
   */
  loadZkServerValue: function (serverComponent) {
    var zkHostNames = this.get('content.masterComponentHosts').filterProperty('component', serverComponent.component_name).length;
    var hostSuffix;
    if (zkHostNames === 1) {
      hostSuffix = Em.I18n.t('installer.step8.host');
    } else {
      hostSuffix = Em.I18n.t('installer.step8.hosts');
    }
    serverComponent.set('component_value', zkHostNames + hostSuffix);
  },

  /**
   * Onclick handler for <code>next</code> button
   * @method submit
   * @return {void}
   */
  submit: function () {
    var wizardController;
    if (!this.get('isSubmitDisabled')) {
      wizardController = App.router.get(this.get('content.controllerName'));
      wizardController.setLowerStepsDisable(wizardController.get('currentStep'));
      this.set('isSubmitDisabled', true);
      this.set('isBackBtnDisabled', true);
      this.showRestartWarnings()
        .then(this.checkKDCSession.bind(this));
    }
  },

  /**
   * Warn user about services that will be restarted during installation.
   *
   * @returns {$.Deferred}
   */
  showRestartWarnings: function() {
    var self = this;
    var dfd = $.Deferred();
    var wizardController = App.router.get(this.get('content.controllerName'));
    var selectedServiceNames = this.get('selectedServices').mapProperty('serviceName');
    var installedServiceNames = this.get('installedServices').mapProperty('serviceName');

    if (this.get('isAddService') && selectedServiceNames.contains('OOZIE')) {
      var affectedServices = ['HDFS', 'YARN'].filter(function(serviceName) {
        return installedServiceNames.contains(serviceName);
      });
      if (affectedServices.length) {
        var serviceNames = affectedServices.length > 1 ?
            '<b>{0}</b> {1} <b>{2}</b>'.format(affectedServices[0], Em.I18n.t('and'), affectedServices[1]) : '<b>' + affectedServices[0] + '</b> ';
        App.ModalPopup.show({
          encodeBody: false,
          header: Em.I18n.t('common.warning'),
          body: Em.I18n.t('installer.step8.services.restart.required').format(serviceNames, stringUtils.pluralize(affectedServices.length, Em.I18n.t('common.service').toLowerCase())),
          secondary: Em.I18n.t('common.cancel'),
          primary: Em.I18n.t('common.proceedAnyway'),
          onPrimary: function() {
            this.hide();
            dfd.resolve();
          },
          onClose: function() {
            this.hide();
            self.set('isSubmitDisabled', false);
            self.set('isBackBtnDisabled', false);
            wizardController.setStepsEnable();
            dfd.reject();
          },
          onSecondary: function() {
            this.onClose();
          }
        });
      } else {
        dfd.resolve();
      }
    } else {
      dfd.resolve();
    }
    return dfd.promise();
  },

  checkKDCSession: function() {
    var self = this;
    var wizardController = App.router.get(this.get('content.controllerName'));
    if (!this.get('isInstaller')) {
      App.get('router.mainAdminKerberosController').getKDCSessionState(this.submitProceed.bind(this), function () {
        self.set('isSubmitDisabled', false);
        self.set('isBackBtnDisabled', false);
        wizardController.setStepsEnable();
        if (self.get('isAddService')) {
          wizardController.setSkipSlavesStep(wizardController.getDBProperty('selectedServiceNames'), 3);
        }
      });
    } else {
      this.submitProceed();
    }
  },

  /**
   * Prepare <code>ajaxQueue</code> and start to execute it
   * @method submitProceed
   */
  submitProceed: function () {
    var self = this;
    this.set('clusterDeleteRequestsCompleted', 0);
    this.get('clusterDeleteErrorViews').clear();
    if (this.get('isAddHost')) {
      App.router.get('addHostController').setLowerStepsDisable(4);
    }

    // checkpoint the cluster status on the server so that the user can resume from where they left off
    switch (this.get('content.controllerName')) {
      case 'installerController':
        App.clusterStatus.setClusterStatus({
          clusterName: this.get('clusterName'),
          clusterState: 'CLUSTER_DEPLOY_PREP_2',
          wizardControllerName: this.get('content.controllerName'),
          localdb: App.db.data
        });
        break;
      case 'addHostController':
        App.clusterStatus.setClusterStatus({
          clusterName: this.get('clusterName'),
          clusterState: 'ADD_HOSTS_DEPLOY_PREP_2',
          wizardControllerName: this.get('content.controllerName'),
          localdb: App.db.data
        });
        break;
      case 'addServiceController':
        App.clusterStatus.setClusterStatus({
          clusterName: this.get('clusterName'),
          clusterState: 'ADD_SERVICES_DEPLOY_PREP_2',
          wizardControllerName: this.get('content.controllerName'),
          localdb: App.db.data
        });
        break;
      default:
        break;
    }
    // delete any existing clusters to start from a clean slate
    // before creating a new cluster in install wizard
    // TODO: modify for multi-cluster support
    this.getExistingClusterNames().complete(function () {
      var clusterNames = self.get('clusterNames');
      if (self.get('isInstaller') && !App.get('testMode') && clusterNames.length) {
        self.deleteClusters(clusterNames);
      } else {
        self.getExistingVersions();
      }
    });
  },

  /**
   * Get list of existing cluster names
   * @returns {object|null}
   * returns an array of existing cluster names.
   * returns an empty array if there are no existing clusters.
   * @method getExistingClusterNames
   */
  getExistingClusterNames: function () {
    return App.ajax.send({
      name: 'wizard.step8.existing_cluster_names',
      sender: this,
      success: 'getExistingClusterNamesSuccessCallBack',
      error: 'getExistingClusterNamesErrorCallback'
    });
  },

  /**
   * Save received list to <code>clusterNames</code>
   * @param {Object} data
   * @method getExistingClusterNamesSuccessCallBack
   */
  getExistingClusterNamesSuccessCallBack: function (data) {
    var clusterNames = data.items.mapProperty('Clusters.cluster_name');
    this.set('clusterNames', clusterNames);
  },

  /**
   * If error appears, set <code>clusterNames</code> to <code>[]</code>
   * @method getExistingClusterNamesErrorCallback
   */
  getExistingClusterNamesErrorCallback: function () {
    this.set('clusterNames', []);
  },

  /**
   * Delete cluster by name
   * One request for one cluster!
   * @param {string[]} clusterNames
   * @method deleteClusters
   */
  deleteClusters: function (clusterNames) {
    this.get('clusterDeleteErrorViews').clear();
    clusterNames.forEach(function (clusterName, index) {
      App.ajax.send({
        name: 'common.delete.cluster',
        sender: this,
        data: {
          name: clusterName,
          isLast: index === clusterNames.length - 1
        },
        success: 'deleteClusterSuccessCallback',
        error: 'deleteClusterErrorCallback'
      });
    }, this);

  },

  /**
   * Method to execute after successful cluster deletion
   * @method deleteClusterSuccessCallback
   */
  deleteClusterSuccessCallback: function () {
    this.incrementProperty('clusterDeleteRequestsCompleted');
    if (this.get('isAllClusterDeleteRequestsCompleted')) {
      if (this.get('clusterDeleteErrorViews.length')) {
        this.showDeleteClustersErrorPopup();
      } else {
        this.getExistingVersions();
      }
    }
  },

  /**
   * Method to execute after failed cluster deletion
   * @param {object} request
   * @param {string} ajaxOptions
   * @param {string} error
   * @param {object} opt
   * @method deleteClusterErrorCallback
   */
  deleteClusterErrorCallback: function (request, ajaxOptions, error, opt) {
    this.incrementProperty('clusterDeleteRequestsCompleted');
    try {
      var json = $.parseJSON(request.responseText);
      var message = json.message;
    } catch (err) {
    }
    this.get('clusterDeleteErrorViews').pushObject(App.AjaxDefaultErrorPopupBodyView.create({
      url: opt.url,
      type: opt.type,
      status: request.status,
      message: message
    }));
    if (this.get('isAllClusterDeleteRequestsCompleted')) {
      this.showDeleteClustersErrorPopup();
    }
  },

  /**
   * Show error popup if cluster deletion failed
   * @method showDeleteClustersErrorPopup
   */
  showDeleteClustersErrorPopup: function () {
    var self = this;
    this.setProperties({
      isSubmitDisabled: false,
      isBackBtnDisabled: false
    });
    App.ModalPopup.show({
      header: Em.I18n.t('common.error'),
      secondary: false,
      onPrimary: function () {
        this.hide();
      },
      bodyClass: Em.ContainerView.extend({
        childViews: self.get('clusterDeleteErrorViews')
      })
    });
  },

  /**
   * Get existing repo_versions
   * @method getExistingVersions
   */
  getExistingVersions: function () {
    return App.ajax.send({
      name: 'wizard.get_version_definitions',
      sender: this,
      success: 'getExistingVersionsSuccessCallback'
    });
  },

  /**
   * @param {Object} data
   * @method getExistingVersionsSuccessCallback
   */
  getExistingVersionsSuccessCallback: function (data) {
    if (this.get('isInstaller') && !App.get('testMode') && data.items.length) {
      this.set('existingRepositoryVersions', data.items.length);
      this.deleteExistingVersions(data.items);
    } else {
      this.startDeploy();
    }
  },

  /**
   * Delete existing repo_versions
   * @param {Array} versions
   * @method deleteExistingVersions
   */
  deleteExistingVersions: function (versions) {
    versions.forEach(function (version) {
      App.ajax.send({
        name: 'wizard.delete_repository_versions',
        sender: this,
        data: {
          id: version.VersionDefinition.id,
          stackName: version.VersionDefinition.stack_name,
          stackVersion: version.VersionDefinition.stack_version
        },
        success: 'deleteExistingVersionsSuccessCallback'
      });
    }, this);
  },

  /**
   * Method to execute after successful version deletion
   * @method deleteExistingVersionsSuccessCallback
   */
  deleteExistingVersionsSuccessCallback: function () {
    this.decrementProperty('existingRepositoryVersions');
    if (this.get('existingRepositoryVersions') === 0) {
      this.startDeploy();
    }
  },

  /**
   * updates kerberosDescriptorConfigs
   * @method updateKerberosDescriptor
   */
  updateKerberosDescriptor: function(instant) {
    var kerberosDescriptor = this.get('wizardController').getDBProperty('kerberosDescriptorConfigs');
    var descriptorExists = this.get('wizardController').getDBProperty('isClusterDescriptorExists') === true;

    var ajaxOpts = {
      name: descriptorExists ? 'admin.kerberos.cluster.artifact.update' : 'admin.kerberos.cluster.artifact.create',
      data: {
        artifactName: 'kerberos_descriptor',
        data: {
          artifact_data: this.removeIdentityReferences(kerberosDescriptor)
        }
      }
    };
    if (instant) {
      ajaxOpts.sender = this;
      App.ajax.send(ajaxOpts);
    } else {
      this.addRequestToAjaxQueue(ajaxOpts);
    }
  },

  /**
   * To Start deploy process
   * @method startDeploy
   */
  startDeploy: function () {
    if (!this.get('isInstaller')) {
      this._startDeploy();
    } else {
      var installerController = App.router.get('installerController');
      var versionData = installerController.getSelectedRepoVersionData();
      if (versionData) {
        var self = this;
        installerController.postVersionDefinitionFileStep8(versionData.isXMLdata, versionData.data).done(function (versionInfo) {
          if (versionInfo.id && versionInfo.stackName && versionInfo.stackVersion) {
            var selectedStack = App.Stack.find().findProperty('isSelected', true);
            if (selectedStack) {
              selectedStack.set('versionInfoId', versionInfo.id);
            }
            installerController.updateRepoOSInfo(versionInfo, selectedStack).done(function() {
              self._startDeploy();
            });
          }
        });
      } else {
        this._startDeploy();
      }
    }
  },

  /**
   * Start deploy process
   * @method startDeploy
   */
  _startDeploy: function () {
    this.createCluster();
    this.createSelectedServices();
    if (!this.get('isAddHost')) {
      if (this.get('isAddService')) {
        // for manually enabled Kerberos descriptor was updated on transition to this step
        if (App.get('isKerberosEnabled') && !this.get('isManualKerberos')) {
          this.updateKerberosDescriptor();
        }
        var fileNamesToUpdate = this.get('wizardController').getDBProperty('fileNamesToUpdate').uniq();
        if (fileNamesToUpdate && fileNamesToUpdate.length) {
          this.applyConfigurationsToCluster(this.generateDesiredConfigsJSON(this.get('configs'), fileNamesToUpdate));
        }
      }
      this.createConfigurations();
      this.applyConfigurationsToCluster(this.get('serviceConfigTags'));
    }
    this.createComponents();
    this.registerHostsToCluster();
    this.createConfigurationGroups();
    this.createMasterHostComponents();
    this.createSlaveAndClientsHostComponents();
    if (this.get('isAddService')) {
      this.createAdditionalClientComponents();
    }
    this.createAdditionalHostComponents();

    this.set('ajaxQueueLength', this.get('ajaxRequestsQueue.queue.length'));
    this.get('ajaxRequestsQueue').start();
    this.showLoadingIndicator();
  },

  /**
   * *******************************************************************
   * The following create* functions are called upon submitting Step 8.
   * *******************************************************************
   */

  /**
   * Create cluster using selected stack version
   * Queued request
   * @method createCluster
   */
  createCluster: function () {
    if (!this.get('isInstaller')) return;
    var stackVersion = this.get('content.installOptions.localRepo') ? App.currentStackVersion.replace(/(-\d+(\.\d)*)/ig, "Local$&") : App.currentStackVersion;
    this.addRequestToAjaxQueue({
      name: 'wizard.step8.create_cluster',
      data: {
        data: JSON.stringify({ "Clusters": {"version": stackVersion}})
      },
      success: 'createClusterSuccess'
    });
  },

  createClusterSuccess: function (data, xhr, params) {
    App.set('clusterName', params.cluster);
  },

  /**
   * Create selected to install services
   * Queued request
   * Skipped if no services where selected!
   * @method createSelectedServices
   */
  createSelectedServices: function () {
    var data = this.createSelectedServicesData();
    if (!data.length) return;
    this.addRequestToAjaxQueue({
      name: 'wizard.step8.create_selected_services',
      data: {
        data: JSON.stringify(data)
      }
    });
  },

  /**
   * Format data for <code>createSelectedServices</code> request
   * @returns {Object[]}
   * @method createSelectedServicesData
   */
  createSelectedServicesData: function () {
    var selectedStack;
    if (this.get('isInstaller')) {
      selectedStack = App.Stack.find().findProperty('isSelected', true);
    }
    return this.get('selectedServices').map(service => selectedStack ?
      {"ServiceInfo": { "service_name": service.get('serviceName'), "desired_repository_version_id": selectedStack.get('versionInfoId') }} :
      {"ServiceInfo": { "service_name": service.get('serviceName') }});
  },

  /**
   * Create components for selected services
   * Queued requests
   * One request for each service!
   * @method createComponents
   */
  createComponents: function () {
    var serviceComponents = App.StackServiceComponent.find().filterProperty('isInstallable');
    this.get('selectedServices').forEach(function (_service) {
      var serviceName = _service.get('serviceName');
      var componentsData = serviceComponents.filterProperty('serviceName', serviceName).map(function (_component) {
        return { "ServiceComponentInfo": { "component_name": _component.get('componentName') } };
      });

      // Service must be specified in terms of a query for creating multiple components at the same time.
      // See AMBARI-1018.
      this.addRequestToCreateComponent(componentsData, serviceName);
    }, this);

    if (this.get('isAddHost')) {
      var allServiceComponents = [];
      var services = App.Service.find().mapProperty('serviceName');
      services.forEach(function(_service){
        var _serviceComponents = App.Service.find(_service).get('serviceComponents');
        allServiceComponents = allServiceComponents.concat(_serviceComponents);
      }, this);
      this.get('content.slaveComponentHosts').forEach(function (component) {
        if (component.componentName !== 'CLIENT' && !allServiceComponents.contains(component.componentName)) {
          this.addRequestToCreateComponent(
              [{"ServiceComponentInfo": {"component_name": component.componentName}}],
              App.StackServiceComponent.find().findProperty('componentName', component.componentName).get('serviceName')
          );
        }
      }, this);
      this.get('content.clients').forEach(function (component) {
        if (!allServiceComponents.contains(component.component_name)) {
          this.addRequestToCreateComponent(
              [{"ServiceComponentInfo": {"component_name": component.component_name}}],
              App.StackServiceComponent.find().findProperty('componentName', component.component_name).get('serviceName')
          );
        }
      }, this);
    }
  },

  /**
   * Add request to ajax queue to create service component
   * @param componentsData
   * @param serviceName
   */
  addRequestToCreateComponent: function (componentsData, serviceName) {
    this.addRequestToAjaxQueue({
      name: 'wizard.step8.create_components',
      data: {
        data: JSON.stringify({"components": componentsData}),
        serviceName: serviceName
      }
    });
  },

  /**
   * Error callback for new service component request
   * So, if component doesn't exist we should create it
   * @param {object} request
   * @param {object} ajaxOptions
   * @param {string} error
   * @param {object} opt
   * @param {object} params
   * @method newServiceComponentErrorCallback
   */
  newServiceComponentErrorCallback: function (request, ajaxOptions, error, opt, params) {
    this.addRequestToAjaxQueue({
      name: 'wizard.step8.create_components',
      data: {
        serviceName: params.serviceName,
        data: JSON.stringify({
          "components": [
            {
              "ServiceComponentInfo": {
                "component_name": params.componentName
              }
            }
          ]
        })
      }
    });
  },

  /**
   * Register hosts
   * Queued request
   * @method registerHostsToCluster
   */
  registerHostsToCluster: function () {
    var data = this.createRegisterHostData();
    if (!data.length) return;
    this.addRequestToAjaxQueue({
      name: 'wizard.step8.register_host_to_cluster',
      data: {
        data: JSON.stringify(data)
      }
    });
  },

  /**
   * Format request-data for <code>registerHostsToCluster</code>
   * @returns {Object}
   * @method createRegisterHostData
   */
  createRegisterHostData: function () {
    return this.getRegisteredHosts().filterProperty('isInstalled', false).map(function (host) {
      return {"Hosts": { "host_name": host.hostName}};
    });
  },

  /**
   * Register new master components
   * @uses registerHostsToComponent
   * @method createMasterHostComponents
   */
  createMasterHostComponents: function () {
    var masterOnAllHosts = [];

    this.get('content.services').filterProperty('isSelected').forEach(function (service) {
      service.get('serviceComponents').filterProperty('isRequiredOnAllHosts').forEach(function (component) {
        if (component.get('isMaster')) {
          masterOnAllHosts.push(component.get('componentName'));
        }
      }, this);
    }, this);

    // create master components for only selected services.
    var selectedMasterComponents = this.get('content.masterComponentHosts').filter(function (_component) {
      return this.get('selectedServices').mapProperty('serviceName').contains(_component.serviceId)
    }, this);
    selectedMasterComponents.mapProperty('component').uniq().forEach(function (component) {
      var hostNames = [];
      if (masterOnAllHosts.length > 0) {
        var compOnAllHosts = false;
        for (var i=0; i < masterOnAllHosts.length; i++) {
          if (component === masterOnAllHosts[i]) {
            compOnAllHosts = true;
            break;
          }
        }
        if (!compOnAllHosts) {
          hostNames = selectedMasterComponents.filterProperty('component', component).filterProperty('isInstalled', false).mapProperty('hostName');
          this.registerHostsToComponent(hostNames, component);
        }
      } else {
        hostNames = selectedMasterComponents.filterProperty('component', component).filterProperty('isInstalled', false).mapProperty('hostName');
        this.registerHostsToComponent(hostNames, component);
      }
    }, this);
  },

  getClientsMap: function (flag) {
    var clients = App.StackServiceComponent.find().filterProperty('isClient'),
      clientsMap = {},
      dependedComponents = flag ? App.StackServiceComponent.find().filterProperty(flag) : App.StackServiceComponent.find();
    clients.forEach(function (client) {
      var clientName = client.get('componentName');
      clientsMap[clientName] = Em.A([]);
      dependedComponents.forEach(function (component) {
        if (component.dependsOn(client))
          clientsMap[clientName].push(component.get('componentName'));
      });
      if (!clientsMap[clientName].length) delete clientsMap[clientName];
    });
    return clientsMap;
  },

  /**
   * Register slave components and clients
   * @uses registerHostsToComponent
   * @method createSlaveAndClientsHostComponents
   */
  createSlaveAndClientsHostComponents: function () {
    var masterHosts = this.get('content.masterComponentHosts');
    var slaveHosts = this.get('content.slaveComponentHosts');
    var clients = this.get('content.clients').filterProperty('isInstalled', false);
    var slaveOnAllHosts = [];
    var clientOnAllHosts = [];

    this.get('content.services').filterProperty('isSelected').forEach(function (service) {
      service.get('serviceComponents').filterProperty('isRequiredOnAllHosts').forEach(function (component) {
        if (component.get('isClient')) {
          clientOnAllHosts.push(component.get('componentName'));
        } else if (component.get('isSlave')) {
          slaveOnAllHosts.push(component.get('componentName'));
        }
      }, this);
    }, this);

    /**
     * Determines on which hosts client should be installed (based on availability of master components on hosts)
     * @type {Object}
     * Format:
     * <code>
     *  {
     *    CLIENT1: Em.A([MASTER1, MASTER2, ...]),
     *    CLIENT2: Em.A([MASTER3, MASTER1, ...])
     *    ...
     *  }
     * </code>
     */
    var clientsToMasterMap = this.getClientsMap('isMaster'),
        clientsToSlaveMap = this.getClientsMap('isSlave');

    slaveHosts.forEach(function (_slave) {
      var hostNames = [];
      var compOnAllHosts;
      if (_slave.componentName !== 'CLIENT') {
        if (slaveOnAllHosts.length > 0) {
          compOnAllHosts = false;
          for (var i=0; i < slaveOnAllHosts.length; i++) {
            if (_slave.componentName === slaveOnAllHosts[i]) {
              // component with ALL cardinality should not
              // registerHostsToComponent in createSlaveAndClientsHostComponents
              compOnAllHosts = true;
              break;
            }
          }
          if (!compOnAllHosts) {
            hostNames = _slave.hosts.filterProperty('isInstalled', false).mapProperty('hostName');
            this.registerHostsToComponent(hostNames, _slave.componentName);
          }
        } else {
          hostNames = _slave.hosts.filterProperty('isInstalled', false).mapProperty('hostName');
          this.registerHostsToComponent(hostNames, _slave.componentName);
        }
      }
      else {
        clients.forEach(function (_client) {
          hostNames = _slave.hosts.mapProperty('hostName');
          // The below logic to install clients to existing/New master hosts should not be applied to Add Host wizard.
          // This is with the presumption that Add Host controller does not add any new Master component to the cluster
          if (!this.get('isAddHost')) {
            if (clientsToMasterMap[_client.component_name]) {
              clientsToMasterMap[_client.component_name].forEach(function (componentName) {
                masterHosts.filterProperty('component', componentName).forEach(function (_masterHost) {
                  hostNames.pushObject(_masterHost.hostName);
                });
              });
            }
          }
          if (clientsToSlaveMap[_client.component_name]) {
            clientsToSlaveMap[_client.component_name].forEach(function (componentName) {
              slaveHosts.filterProperty('componentName', componentName).forEach(function (slaveHost) {
                hostNames = hostNames.concat(slaveHost.hosts.mapProperty('hostName')).uniq();
              });
            });
          }
          if (clientOnAllHosts.length > 0) {
            compOnAllHosts = false;
            for (var i=0; i < clientOnAllHosts.length; i++) {
              if (_client.component_name === clientOnAllHosts[i]) {
                // component with ALL cardinality should not
                // registerHostsToComponent in createSlaveAndClientsHostComponents
                compOnAllHosts = true;
                break;
              }
            }
            if (!compOnAllHosts) {
              hostNames = hostNames.uniq();
              this.registerHostsToComponent(hostNames, _client.component_name);
            }
          } else {
            hostNames = hostNames.uniq();
            this.registerHostsToComponent(hostNames, _client.component_name);
          }
        }, this);
      }
    }, this);
  },

  /**
   * This function is specific to addServiceController
   * Newly introduced master components requires some existing client components to be hosted along with them
   */
  createAdditionalClientComponents: function () {
    var masterHosts = this.get('content.masterComponentHosts');
    var clientHosts = [];
    if (this.get('content.slaveComponentHosts').someProperty('componentName', 'CLIENT')) {
      clientHosts = this.get('content.slaveComponentHosts').findProperty('componentName', 'CLIENT').hosts;
    }
    var clients = this.get('content.clients').filterProperty('isInstalled', false);
    var clientsToMasterMap = this.getClientsMap('isMaster');
    var clientsToClientMap = this.getClientsMap('isClient');
    var installedClients = [];

    // Get all the installed Client components
    this.get('content.services').filterProperty('isInstalled').forEach(function (_service) {
      var serviceClients = App.StackServiceComponent.find().filterProperty('serviceName', _service.get('serviceName')).filterProperty('isClient');
      serviceClients.forEach(function (client) {
        installedClients.push(client.get('componentName'));
      }, this);
    }, this);

    // Check if there is a dependency for being co-hosted between existing client and selected new master
    installedClients.forEach(function (_clientName) {
      if (clientsToMasterMap[_clientName] || clientsToClientMap[_clientName]) {
        var hostNames = [];
        if (clientsToMasterMap[_clientName]) {
          clientsToMasterMap[_clientName].forEach(function (componentName) {
            masterHosts.filterProperty('component', componentName).filterProperty('isInstalled', false).forEach(function (_masterHost) {
              hostNames.pushObject(_masterHost.hostName);
            }, this);
          }, this);
        }
        if (clientsToClientMap[_clientName]) {
          clientsToClientMap[_clientName].forEach(function (componentName) {
            clientHosts.forEach(function (_clientHost) {
              var host = this.get('content.hosts')[_clientHost.hostName];
              var isClientSelected = clients.someProperty('component_name', componentName);
              if (host.isInstalled && isClientSelected && !host.hostComponents.someProperty('HostRoles.component_name', componentName)) {
                hostNames.pushObject(_clientHost.hostName);
              }
            }, this);
          }, this);
        }
        hostNames = hostNames.uniq();
        if (hostNames.length > 0) {
          // If a dependency for being co-hosted is derived between existing client and selected new master but that
          // dependency is already satisfied in the cluster then disregard the derived dependency
          this.removeClientsFromList(_clientName, hostNames);
          this.registerHostsToComponent(hostNames, _clientName);
          if(hostNames.length > 0) {
            this.get('content.additionalClients').pushObject({hostNames: hostNames, componentName: _clientName});
          }
        }
      }
    }, this);
  },

  /**
   *
   * @param clientName
   * @param hostList
   */
  removeClientsFromList: function (clientName, hostList) {
    var clientHosts = [];
    var installedHosts = this.get('content.hosts');
    for (var hostName in installedHosts) {
      if (installedHosts[hostName].isInstalled) {
        if (installedHosts[hostName].hostComponents.mapProperty('HostRoles.component_name').contains(clientName)) {
          clientHosts.push(hostName);
        }
      }
    }

    if (clientHosts.length > 0) {
      clientHosts.forEach(function (hostName) {
        if (hostList.contains(hostName)) {
          hostList.splice(hostList.indexOf(hostName), 1);
        }
      }, this);
    }
  },

  /**
   * Register additional components
   * Based on availability of some services
   * @uses registerHostsToComponent
   * @method createAdditionalHostComponents
   */
  createAdditionalHostComponents: function () {
    var masterHosts = this.get('content.masterComponentHosts');

    // add all components with cardinality == ALL of selected services
    var registeredHosts = this.getRegisteredHosts();
    var notInstalledHosts = registeredHosts.filterProperty('isInstalled', false);
    this.get('content.services').filterProperty('isSelected').forEach(function (service) {
      service.get('serviceComponents').filterProperty('isRequiredOnAllHosts').forEach(function (component) {
        if (service.get('isInstalled') && notInstalledHosts.length) {
          this.registerHostsToComponent(notInstalledHosts.mapProperty('hostName'), component.get('componentName'));
        } else if (!service.get('isInstalled') && registeredHosts.length) {
          this.registerHostsToComponent(registeredHosts.mapProperty('hostName'), component.get('componentName'));
        }
      }, this);
    }, this);

    // add MySQL Server if Hive is selected
    var hiveService = this.get('content.services').filterProperty('isSelected', true).filterProperty('isInstalled', false).findProperty('serviceName', 'HIVE');
    if (hiveService) {
      var hiveDb = this.get('content.serviceConfigProperties').findProperty('name', 'hive_database');
      if (hiveDb.value === "New MySQL Database") {
        this.registerHostsToComponent(masterHosts.filterProperty('component', 'HIVE_SERVER').mapProperty('hostName'), 'MYSQL_SERVER');
      } else if (hiveDb.value === "New PostgreSQL Database") {
        this.registerHostsToComponent(masterHosts.filterProperty('component', 'HIVE_SERVER').mapProperty('hostName'), 'POSTGRESQL_SERVER');
      }
    }
  },

  /**
   * Register component to hosts
   * Queued request
   * @param {String[]} hostNames
   * @param {String} componentName
   * @method registerHostsToComponent
   */
  registerHostsToComponent: function (hostNames, componentName) {
    if (!hostNames.length) return;

    var queryStr = '';
    hostNames.forEach(function (hostName) {
      queryStr += 'Hosts/host_name=' + hostName + '|';
    });
    //slice off last symbol '|'
    queryStr = queryStr.slice(0, -1);

    var data = {
      "RequestInfo": {
        "query": queryStr
      },
      "Body": {
        "host_components": [
          {
            "HostRoles": {
              "component_name": componentName
            }
          }
        ]
      }
    };

    this.addRequestToAjaxQueue({
      name: 'wizard.step8.register_host_to_component',
      data: {
        data: JSON.stringify(data)
      }
    });
  },

  /**
   * Create config objects for cluster and services
   * @method createConfigurations
   */
  createConfigurations: function () {
    if (this.get('isInstaller')) {
      /** add cluster-env **/
      this.get('serviceConfigTags').pushObject(this.createDesiredConfig('cluster-env', this.get('configs').filterProperty('filename', 'cluster-env.xml')));
    }

    this.get('selectedServices').forEach(function (service) {
      Object.keys(service.get('configTypes')).forEach(function (type) {
        if (!this.get('serviceConfigTags').someProperty('type', type)) {
          var configs = this.get('configs').filterProperty('filename', App.config.getOriginalFileName(type));
          var serviceConfigNote = this.getServiceConfigNote(type, service.get('displayName'));
          this.get('serviceConfigTags').pushObject(this.createDesiredConfig(type, configs, serviceConfigNote));
        }
      }, this);
    }, this);
    this.createNotification();
  },

  /**
   * Get config version message
   *
   * @param type
   * @param serviceDisplayName
   * @returns {*}
   */
  getServiceConfigNote: function(type, serviceDisplayName) {
    return this.get('isAddService') && type === 'core-site' ?
      Em.I18n.t('dashboard.configHistory.table.notes.addService') : Em.I18n.t('dashboard.configHistory.table.notes.default').format(serviceDisplayName);
  },

  /**
   * Send <code>serviceConfigTags</code> to server
   * Queued request
   * One request for each service config tag
   * @param serviceConfigTags
   * @method applyConfigurationsToCluster
   */
  applyConfigurationsToCluster: function (serviceConfigTags) {
    var allServices = this.get('installedServices').concat(this.get('selectedServices'));
    var allConfigData = [];
    allServices.forEach(function (service) {
      var serviceConfigData = [];
      Object.keys(service.get('configTypesRendered')).forEach(function (type) {
        var serviceConfigTag = serviceConfigTags.findProperty('type', type);
        if (serviceConfigTag) {
          serviceConfigData.pushObject(serviceConfigTag);
        }
      }, this);
      if (serviceConfigData.length) {
        allConfigData.pushObject(JSON.stringify({
          Clusters: {
            desired_config: serviceConfigData.map(function(item) {
              var props = {};
              Em.keys(item.properties).forEach(function(propName) {
                if (item.properties[propName] !== null) {
                  props[propName] = item.properties[propName];
                }
              });
              item.properties = props;
              return item;
            })
          }
        }));
      }
    }, this);
    var clusterConfig = serviceConfigTags.findProperty('type', 'cluster-env');
    if (clusterConfig) {
      allConfigData.pushObject(JSON.stringify({
        Clusters: {
          desired_config: [clusterConfig]
        }
      }));
    }

    this.addRequestToAjaxQueue({
      name: 'common.across.services.configurations',
      data: {
        data: '[' + allConfigData.toString() + ']'
      }
    });
  },

  /**
   * Create and update config groups
   * @method createConfigurationGroups
   */
  createConfigurationGroups: function () {
    var configGroups = this.get('content.configGroups').filterProperty('is_default', false);
    var groupsToDelete = App.router.get(this.get('content.controllerName')).getDBProperty('groupsToDelete');
    if (groupsToDelete && groupsToDelete.length > 0) {
      this.removeInstalledServicesConfigurationGroups(groupsToDelete);
    }
    configGroups.forEach(function (configGroup) {
      if (configGroup.is_for_update || configGroup.is_temporary) {
        this.saveGroup(configGroup.properties, configGroup, this.getServiceConfigNote('', configGroup.service_id));
      }
    }, this);
    App.ServiceConfigGroup.deleteTemporaryRecords();
  },

  /**
   * add request to create config group to queue
   *
   * @param data
   * @method createConfigGroup
   */
  createConfigGroup: function(data) {
    this.addRequestToAjaxQueue({
      name: 'wizard.step8.apply_configuration_groups',
      sender: this,
      data: {
        data: JSON.stringify(data)
      }
    });
  },

  /**
   * add request to update config group to queue
   *
   * @param data {Object}
   * @method updateConfigGroup
   */
  updateConfigGroup: function (data) {
    this.addRequestToAjaxQueue({
      name: 'config_groups.update_config_group',
      sender: this,
      data: {
        id: data.ConfigGroup.id,
        configGroup: data
      }
    });
  },

  /**
   * Delete selected config groups
   * @param {Object[]} groupsToDelete
   * @method removeInstalledServicesConfigurationGroups
   */
  removeInstalledServicesConfigurationGroups: function (groupsToDelete) {
    var self = this;
    groupsToDelete.forEach(function (item) {
      self.deleteConfigurationGroup(Em.Object.create(item));
    });
  },

  /**
   * Selected and installed services
   * @override
   */
  currentServices: function() {
    return this.get('installedServices').concat(this.get('selectedServices'));
  }.property('installedServices.length', 'selectedServices.length'),

  /**
   * Add handling GLUSTREFS properties
   * @param property
   * @returns {*}
   * @override
   */
  formatValueBeforeSave: function(property) {
    if (this.formatGLUSTERFSProperties(Em.get(property, 'filename'))) {
      switch (property.name) {
        case "fs.default.name":
          return this.get('configs').someProperty('name', 'fs_glusterfs_default_name') ?
            this.get('configs').findProperty('name', 'fs_glusterfs_default_name').value : null;
        case "fs.defaultFS":
          return this.get('configs').someProperty('name', 'glusterfs_defaultFS_name') ?
            this.get('configs').findProperty('name', 'glusterfs_defaultFS_name').value : null;
      }
    }
    return this._super(property);
  },

  /**
   * Defines if some GLUSTERFS properties should be changed
   *
   * @param {String} type
   * @returns {boolean}
   */
  formatGLUSTERFSProperties: function(type) {
    return App.config.getConfigTagFromFileName(type) === 'core-site'
      && this.get('installedServices').concat(this.get('selectedServices')).someProperty('serviceName', 'GLUSTERFS');
  },


  /**
   * Create one Alert Notification (if user select this on step7)
   * Only for Install Wizard and stack
   * @method createNotification
   */
  createNotification: function () {
    if (!this.get('isInstaller')) return;
    var miscConfigs = this.get('configs').filterProperty('serviceName', 'MISC'),
      createNotification = miscConfigs.findProperty('name', 'create_notification').value;
    if (createNotification !== 'yes') return;
      var predefinedNotificationConfigNames = require('data/configs/alert_notification').mapProperty('name'),
      configsForNotification = this.get('configs').filterProperty('filename', 'alert_notification');
    var properties = {},
      names = [
        'ambari.dispatch.recipients',
        'mail.smtp.host',
        'mail.smtp.port',
        'mail.smtp.from',
        'mail.smtp.starttls.enable',
        'mail.smtp.startssl.enable'
      ];
    if (miscConfigs.findProperty('name', 'smtp_use_auth').value == 'true') { // yes, it's not converted to boolean
      names.pushObjects(['ambari.dispatch.credential.username', 'ambari.dispatch.credential.password']);
    }

    names.forEach(function (name) {
      properties[name] = miscConfigs.findProperty('name', name).value;
    });

    properties['ambari.dispatch.recipients'] = properties['ambari.dispatch.recipients'].replace(/\s/g, '').split(',');

    configsForNotification.forEach(function (config) {
      if (predefinedNotificationConfigNames.contains(config.name)) return;
      properties[config.name] = config.value;
    });

    var apiObject = {
      AlertTarget: {
        name: 'Initial Notification',
        description: 'Notification created during cluster installing',
        global: true,
        notification_type: 'EMAIL',
        alert_states: ['OK', 'WARNING', 'CRITICAL', 'UNKNOWN'],
        properties: properties
      }
    };
    this.addRequestToAjaxQueue({
      name: 'alerts.create_alert_notification',
      data: {
        urlParams: 'overwrite_existing=true',
        data: apiObject
      }
    });
  },

  /**
   * Should ajax-queue progress bar be displayed
   * @method showLoadingIndicator
   */
  showLoadingIndicator: function () {
    return App.ModalPopup.show({

      header: Em.I18n.t('installer.step8.deployPopup.header'),

      showFooter: false,

      showCloseButton: false,

      bodyClass: Em.View.extend({

        templateName: require('templates/wizard/step8/step8_log_popup'),

        controllerBinding: 'App.router.wizardStep8Controller',

        /**
         * Css-property for progress-bar
         * @type {string}
         */
        barWidth: '',
        progressBarClass: 'progress log_popup',

        /**
         * Popup-message
         * @type {string}
         */
        message: '',

        /**
         * Set progress bar width and popup message when ajax-queue requests are processed
         * @method ajaxQueueChangeObs
         */
        ajaxQueueChangeObs: function () {
          var length = this.get('controller.ajaxQueueLength');
          var left = this.get('controller.ajaxRequestsQueue.queue.length');
          this.set('barWidth', 'width: ' + (length - left) / length * 100 + '%;');
          this.set('message', Em.I18n.t('installer.step8.deployPopup.message').format(length - left, length));
        }.observes('controller.ajaxQueueLength', 'controller.ajaxRequestsQueue.queue.length'),

        /**
         * Hide popup when ajax-queue is finished
         * @method autoHide
         */
        autoHide: function () {
          if (this.get('controller.servicesInstalled')) {
            this.get('parentView').hide();
          }
        }.observes('controller.servicesInstalled'),

        ajaxQueueErrorAppears: function () {
          if (this.get('controller.hasErrorOccurred')) {
            this.get('parentView').onClose();
          }
        }.observes('controller.hasErrorOccurred')

      })

    });
  },

  getComponentsForHost: function(host) {
    if(!host.hostComponents) {
      App.router.get('installerController').get('allHosts');
    }
    var componentNameDetail = [];
    host.hostComponents.mapProperty('componentName').forEach(function (componentName) {
      if(componentName === 'CLIENT') {
        this.get('content.clients').mapProperty('component_name').forEach(function (clientComponent) {
          componentNameDetail.push({ name : clientComponent });
        }, this);
        return;
      }
      componentNameDetail.push({ name : componentName });
    }, this);
    return componentNameDetail;
  },

  getPropertyAttributesForConfigType : function(configs) {
    //Currently only looks for final properties, if any
    var finalProperties = configs.filterProperty('isFinal', 'true');
    var propertyAttributes = {};
    finalProperties.forEach(function (finalProperty) {
      propertyAttributes[finalProperty['name']] = "true";
    });
    var finalPropertyMap = {};
    if (!App.isEmptyObject(finalProperties)) {
      finalPropertyMap = {
        "isFinal": propertyAttributes
      };
    }
    return finalPropertyMap;
  },

  getConfigurationDetailsForConfigType: function(configs) {
    var configDetails = {};
    var self = this;
    configs.forEach(function (propertyObj) {
      configDetails[propertyObj['name']] = propertyObj['value'];
    }, this);
    var configurationsDetails = {
      "properties_attributes": self.getPropertyAttributesForConfigType(configs),
      "properties": configDetails
    };
    return configurationsDetails;
  },

  hostInExistingHostGroup: function (newHost, host_groups) {
    var hostGroupMatched = false;
      host_groups.some(function (existingHostGroup) {
        if(!hostGroupMatched) {
        var fqdnInHostGroup =  existingHostGroup.hosts[0].fqdn;
        var componentsInExistingHostGroup = this.getRegisteredHosts().filterProperty('hostName', fqdnInHostGroup)[0].hostComponents;
        if(componentsInExistingHostGroup.length !== newHost.hostComponents.length) {
          return;
        } else {
          var componentMismatch = false;
          componentsInExistingHostGroup.forEach(function (componentInExistingHostGroup, index) {
          if(!componentMismatch) {
            if(!newHost.hostComponents.mapProperty('componentName').includes(componentInExistingHostGroup.componentName)) {
              componentMismatch = true;
            }
          }
          });
          if(!componentMismatch) {
            hostGroupMatched = true;
            existingHostGroup["cardinality"] = parseInt(existingHostGroup["cardinality"]) + 1;
            existingHostGroup.hosts.push({"fqdn" : newHost.hostName});
            return true;
          }
        }
        }
      }, this);
    return hostGroupMatched;
  },

   hostInChildHostGroup: function (presentHostGroup, configGroupName, hostInConfigGroup) {
    return  presentHostGroup['childHostGroups'].some(function (childHostGroup) {
      //Check if childHostGroup name is same as this configgroupname, if yes, update childHostGroup else, compare with other childhostgroups
      if(childHostGroup.configGroupName === configGroupName) {
        childHostGroup.hosts.push( { "fqdn" : hostInConfigGroup } );
        childHostGroup['cardinality'] = childHostGroup['cardinality'] + 1;
        return true;
      }
      });
  },

  /**
   * Confirmation popup before generate blueprint
   */
  generateBlueprintConfirmation: function () {
    var self = this;
    return App.showConfirmationPopup(function () {
      self.generateBlueprint();
      }, Em.I18n.t('installer.step8.generateBlueprint.popup.msg').format(App.clusterStatus.clusterName)
    );
  },

  generateBlueprint: function () {
    console.log("Prepare blueprint for download...");
    var self = this;
    //service configurations
    var totalConf = [];
    //Add cluster-env
    var clusterEnv = this.get('configs').filterProperty('filename', 'cluster-env.xml');
    var configurations = {};
    configurations["cluster-env"] = self.getConfigurationDetailsForConfigType(clusterEnv);
    totalConf.push(configurations);
    //Add configurations for selected services
    this.get('selectedServices').forEach(function (service) {
      Object.keys(service.get('configTypes')).forEach(function (type) {
        if (!this.get('serviceConfigTags').someProperty('type', type)) {
          var configs = this.get('configs').filterProperty('filename', App.config.getOriginalFileName(type));
          var configurations = {};
          configurations[type] = self.getConfigurationDetailsForConfigType(configs);
          totalConf.push(configurations);
        }
      }, this);
    }, this);

    var host_groups = [];
    var cluster_template_host_groups = [];
    var counter = 0;

    this.getRegisteredHosts().filterProperty('isInstalled', false).map(function (host) {
      if(self.hostInExistingHostGroup(host, host_groups)) {
        return;
      }
      //Create new host_group if host is not mapped to existing host_groups
      var hostGroupId = "host_group_" + counter;
      var hostListForGroup = [];
      hostListForGroup.push({ "fqdn": host.hostName });
      var hostGroupDetail = {
        "name": hostGroupId,
        "components": self.getComponentsForHost(host),
        "hosts": hostListForGroup,
        "cardinality" : 1
      };
      hostGroupDetail.toJSON = function () {
        var hostGroupDetailResult = _.omit(this, [ "hosts", "childHostGroups" ]);
        hostGroupDetailResult["cardinality"] = this["cardinality"].toString();
        return hostGroupDetailResult;
      };
      host_groups.push(hostGroupDetail);

      var clusterTemplateHostGroupDetail = {
        "name": hostGroupId,
        "hosts": hostListForGroup
      };

      cluster_template_host_groups.push(clusterTemplateHostGroupDetail);
      counter++;
    }, this);

    this.get('content.configGroups').filterProperty("is_default", false).forEach(function (configGroup) {
      if (configGroup.properties.length == 0) {
        return;
      }
      configGroup.hosts.forEach(function (hostInConfigGroup) {
        return host_groups.some(function (presentHostGroup) {
          return presentHostGroup.hosts.some(function (hostInPresentHostGroup, index) {
            if (hostInConfigGroup !== hostInPresentHostGroup.fqdn) {
              return;
            }
            //Check if childHostGroup already created
            if (presentHostGroup['childHostGroups']) {
             if (self.hostInChildHostGroup(presentHostGroup, configGroup.name, hostInConfigGroup)) {
               // Update to remove parentHostGroup as all the hosts are added to childHostGroup/s
               presentHostGroup.hosts.splice(index, 1);
               presentHostGroup["cardinality"] = presentHostGroup['cardinality'] - 1;
               return true;
             }
            }
            //create configuration object
            var hgConfigurations;
            if(presentHostGroup.hosts.length === 1 && presentHostGroup["configurations"]) {
              hgConfigurations = presentHostGroup["configurations"][0];
            } else if (presentHostGroup["configurations"]) { //Deep copy
                hgConfigurations = jQuery.extend(true, {}, presentHostGroup["configurations"][0]);
            } else {
                hgConfigurations = {};
            }
            configGroup.properties.forEach(function (hgProperties) {
              var type = App.config.getConfigTagFromFileName(hgProperties.filename);
              if (!hgConfigurations[type]) {
                hgConfigurations[type] = { properties: {} };
              }
              hgConfigurations[type]['properties'][hgProperties.name] = hgProperties.value;
            });
            var totalHgConf = [];
            totalHgConf.push(hgConfigurations);
            //If only host in presentHostGroup then merge configuration and return
            if (presentHostGroup.hosts.length === 1) {
              //If host_group already has configuration, assigned from previously processed config_group
              if(!presentHostGroup["configurations"]) {
                presentHostGroup["configurations"] = totalHgConf;
              }
              return true;
            }

            //Create new host_group from this host
            var hostGroupId = "host_group_" + counter;
            counter++;
            var hostListForGroup = [];
            hostListForGroup.push({ "fqdn": hostInPresentHostGroup.fqdn });
            var hostGroupDetail = {
              "name": hostGroupId,
              "components": presentHostGroup.components,
              "cardinality": 1,
              "hosts": hostListForGroup,
              "configurations": totalHgConf,
              "configGroupName": configGroup.name
            };
            hostGroupDetail.toJSON = function () {
              var hostGroupDetailResult = _.omit(this, [ "hosts", "configGroupName", "childHostGroups" ]);
              hostGroupDetailResult["cardinality"] = this["cardinality"].toString();
              return hostGroupDetailResult;
            };
            host_groups.push(hostGroupDetail);
            //Update for clustertemplate file
            var clusterTemplateHostGroupDetail = {
              "name": hostGroupId,
              "hosts": hostListForGroup
            };
            cluster_template_host_groups.push(clusterTemplateHostGroupDetail);
            //Add newly created host_group as child to existing host_group
            if (!presentHostGroup['childHostGroups']) {
              presentHostGroup['childHostGroups'] = [];
            }
            presentHostGroup['childHostGroups'].push(hostGroupDetail);
            presentHostGroup.hosts.splice(index, 1);
            presentHostGroup["cardinality"] = presentHostGroup['cardinality'] - 1;
            //return true to indicate that host has been matched
            return true;
          }, this);
        }, this);
      }, this);
    }, this);

    const selectedStack = App.Stack.find().findProperty('isSelected', true);
    const blueprint = {
        'configurations': totalConf,
        'host_groups': host_groups.filter(function (item) { return item.cardinality > 0; }),
        'Blueprints': {'blueprint_name' : App.clusterStatus.clusterName, 'stack_name':selectedStack.get('stackName'), 'stack_version':selectedStack.get('stackVersion')}
    };
    const cluster_template = {
      "blueprint": App.clusterStatus.clusterName,
      "config_recommendation_strategy" : "NEVER_APPLY",
      "provision_action" : "INSTALL_AND_START",
      "configurations": [],
      "host_groups": cluster_template_host_groups.filter(function (item) { return item.hosts.length > 0; }),
      "Clusters": {'cluster_name': App.clusterStatus.clusterName}
    };
    fileUtils.downloadFilesInZip([
      {
        data: JSON.stringify(blueprint),
        type: 'json',
        name: 'blueprint.json'
      },
      {
        data: JSON.stringify(cluster_template),
        type: 'json',
        name: 'clustertemplate.json'
      }
    ]);
  },

  downloadCSV: function() {
    App.router.get('kerberosWizardStep5Controller').getCSVData(false);
  }
});
