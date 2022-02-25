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
var lazyloading = require('utils/lazy_loading');
var numberUtils = require('utils/number_utils');

App.WizardStep3Controller = Em.Controller.extend(App.ReloadPopupMixin, App.CheckHostMixin, {

  name: 'wizardStep3Controller',

  hosts: [],

  content: [],

  registeredHosts: [],

  jdkRequestIndex: null,

  registrationStartedAt: null,

  /**
   * Timeout for registration
   * Based on <code>installOptions.manualInstall</code>
   * @type {number}
   */
  registrationTimeoutSecs: Em.computed.ifThenElse('content.installOptions.manualInstall', 15, 120),

  /**
   * Bootstrap calls are stopped
   * @type {bool}
   */
  stopBootstrap: false,

  /**
   * is Submit button disabled
   * @type {bool}
   */
  isSubmitDisabled: true,

  /**
   * True if bootstrap POST request failed
   * @type {bool}
   */
  isBootstrapFailed: false,

  /**
   * is Retry button disabled
   * @type {bool}
   */
  isRetryDisabled: function() {
    return this.get('isBackDisabled') ? this.get('isBackDisabled') : !this.get('bootHosts').filterProperty('bootStatus', 'FAILED').length;
  }.property('bootHosts.@each.bootStatus', 'isBackDisabled'),

  /**
   * Is Back button disabled
   * @return {bool}
   */
  isBackDisabled: function () {
    return (this.get('isRegistrationInProgress') || !this.get('isWarningsLoaded')) && !this.get('isBootstrapFailed') || App.get('router.btnClickInProgress');
  }.property('isRegistrationInProgress', 'isWarningsLoaded', 'isBootstrapFailed'),

  /**
   * Controller is using in Add Host Wizard
   * @return {bool}
   */
  isAddHostWizard: Em.computed.equal('content.controllerName', 'addHostController'),

  /**
   * @type {bool}
   */
  isLoaded: false,

  /**
   * Polls count
   * @type {number}
   */
  numPolls: 0,

  /**
   * Is hosts registration in progress
   * @type {bool}
   */
  isRegistrationInProgress: true,

  /**
   * Are some registered hosts which are not added by user
   * @type {bool}
   */
  hasMoreRegisteredHosts: false,

  /**
   * List of installed hostnames
   * @type {string[]}
   */
  hostsInCluster: function () {
    var installedHostsName = [];
    var hosts = this.get('content.hosts');

    for (var hostName in hosts) {
      if (hosts[hostName].isInstalled) {
        installedHostsName.push(hostName);
      }
    }
    return installedHostsName;
  }.property('content.hosts'),

  /**
   * Are hosts warnings loaded
   * @type {bool}
   */
  isWarningsLoaded: Em.computed.and('isJDKWarningsLoaded', 'isHostsWarningsLoaded'),

  /**
   * Check are hosts have any warnings
   * @type {bool}
   */
  isHostHaveWarnings: Em.computed.gt('warnings.length', 0),

  /**
   * Should warnings-box be visible
   * @type {bool}
   */
  isWarningsBoxVisible: function () {
    return (App.get('testMode')) ? true : !this.get('isRegistrationInProgress');
  }.property('isRegistrationInProgress'),

  isNextButtonDisabled: Em.computed.or('App.router.btnClickInProgress', 'isSubmitDisabled'),

  isBackButtonDisabled: Em.computed.or('App.router.btnClickInProgress', 'isBackDisabled'),

  /**
   *
   * @method navigateStep
   */
  navigateStep: function () {
    if (this.get('isLoaded')) {
      if (!this.get('content.installOptions.manualInstall')) {
        if (!this.get('wizardController').getDBProperty('bootStatus')) {
          this.setupBootStrap();
        }
      } else {
        this.set('bootHosts', this.get('hosts'));
        if (App.get('testMode')) {
          this.startHostcheck(this.get('hosts'));
          this.get('bootHosts').setEach('cpu', '2');
          this.get('bootHosts').setEach('memory', '2000000');
          this.set('isSubmitDisabled', false);
        } else {
          this.set('registrationStartedAt', null);
          this.startRegistration();
        }
      }
    }
  }.observes('isLoaded'),

  /**
   * Clear controller data
   * @method clearStep
   */
  clearStep: function () {
    this.set('stopBootstrap', false);
    this.set('hosts', []);
    this.get('bootHosts').clear();
    this.get('wizardController').setDBProperty('bootStatus', false);
    this.set('isHostsWarningsLoaded', false);
    this.set('isJDKWarningsLoaded', false);
    this.set('registrationStartedAt', null);
    this.set('isLoaded', false);
    this.set('isSubmitDisabled', true);
    this.set('stopChecking', false);
  },

  /**
   * setup bootstrap data and completion callback for bootstrap call
   * @method setupBootStrap
   */
  setupBootStrap: function () {
    var self = this;
    var bootStrapData = JSON.stringify({
        'verbose': true,
        'sshKey': this.get('content.installOptions.sshKey'),
        'hosts': this.getBootstrapHosts(),
        'user': this.get('content.installOptions.sshUser'),
        'sshPort': this.get('content.installOptions.sshPort'),
        'userRunAs': App.get('supports.customizeAgentUserAccount') ? this.get('content.installOptions.agentUser') : 'root'
    });
    App.router.get(this.get('content.controllerName')).launchBootstrap(bootStrapData, function (requestId) {
      if (requestId == '0') {
        self.startBootstrap();
      } else if (requestId) {
        self.set('content.installOptions.bootRequestId', requestId);
        App.router.get(self.get('content.controllerName')).save('installOptions');
        self.startBootstrap();
      }
    });
  },

  getBootstrapHosts: function () {
    var hosts = this.get('content.hosts');
    var bootstrapHosts = [];
    for (var host in hosts) {
      if (hosts.hasOwnProperty(host)) {
        if (!hosts[host].isInstalled) {
          bootstrapHosts.push(host);
        }
      }
    }

    return bootstrapHosts;
  },

  /**
   * Make basic init steps
   * @method loadStep
   */
  loadStep: function () {
    var wizardController = this.get('wizardController');
    var previousStep = wizardController && wizardController.get('previousStep');
    var currentStep = wizardController && wizardController.get('currentStep');
    var isHostsLoaded = this.get('hosts').length !== 0;
    var isPrevAndCurrStepsSetted = previousStep && currentStep;
    var isPrevStepSmallerThenCurrent = previousStep < currentStep;
    if (!isHostsLoaded || isPrevStepSmallerThenCurrent ||
        !wizardController || !isPrevAndCurrStepsSetted) {
      this.disablePreviousSteps();
      this.clearStep();
      App.router.get('clusterController').loadAmbariProperties();
      this.loadHosts();
    }
  },

  /**
   * Loads the hostinfo from localStorage on the insertion of view. It's being called from view
   * @method loadHosts
   */
  loadHosts: function () {
    var hostsInfo = this.get('content.hosts');
    var hosts = [];
    var bootStatus = (this.get('content.installOptions.manualInstall')) ? 'DONE' : 'PENDING';
    if (App.get('testMode')) {
      bootStatus = 'REGISTERED';
    }

    for (var index in hostsInfo) {
      if (hostsInfo.hasOwnProperty(index) && !hostsInfo[index].isInstalled) {
        hosts.pushObject(App.HostInfo.create({
          name: hostsInfo[index].name,
          bootStatus: bootStatus,
          isChecked: false
        }));
      }
    }
    this.set('hosts', hosts);
    this.set('isLoaded', true);
  },

  /**
   * Parses and updates the content based on bootstrap API response.
   * @return {bool} true if polling should continue (some hosts are in "RUNNING" state); false otherwise
   * @method parseHostInfo
   */
  parseHostInfo: function (hostsStatusFromServer) {
    hostsStatusFromServer.forEach(function (_hostStatus) {
      var host = this.get('bootHosts').findProperty('name', _hostStatus.hostName);
      // check if hostname extracted from REST API data matches any hostname in content
      // also, make sure that bootStatus modified by isHostsRegistered call does not get overwritten
      // since these calls are being made in parallel
      if (host && !['REGISTERED', 'REGISTERING'].contains(host.get('bootStatus'))) {
        host.set('bootStatus', _hostStatus.status);
        host.set('bootLog', _hostStatus.log);
      }
    }, this);
    // if the data rendered by REST API has hosts in "RUNNING" state, polling will continue
    return this.get('bootHosts').length != 0 && this.get('bootHosts').someProperty('bootStatus', 'RUNNING');
  },

  /**
   * Remove list of hosts
   * @param {Ember.Enumerable} hosts
   * @return {App.ModalPopup}
   * @method removeHosts
   */
  removeHosts: function (hosts) {
    var self = this;
    return App.showConfirmationPopup(function () {
      App.router.send('removeHosts', hosts);
      self.hosts.removeObjects(hosts);
      self.stopRegistration();
      if (!self.hosts.length) {
        self.set('isSubmitDisabled', true);
      }
    }, Em.I18n.t('installer.step3.hosts.remove.popup.body'));
  },

  /**
   * Removes a single element on the trash icon click. Called from View
   * @param {object} hostInfo
   * @method removeHost
   */
  removeHost: function (hostInfo) {
    if (!this.get('isBackDisabled'))
      this.removeHosts([hostInfo]);
  },

  /**
   * Remove selected hosts (click-handler)
   * @return App.ModalPopup
   * @method removeSelectedHosts
   */
  removeSelectedHosts: function () {
    var selectedHosts = this.get('hosts').filterProperty('isChecked', true);
    return this.removeHosts(selectedHosts);
  },

  /**
   * Show popup with the list of hosts which are selected
   * @return App.ModalPopup
   * @method selectedHostsPopup
   */
  selectedHostsPopup: function () {
    var selectedHosts = this.get('hosts').filterProperty('isChecked').mapProperty('name');
    return App.ModalPopup.show({
      header: Em.I18n.t('installer.step3.selectedHosts.popup.header'),
      secondary: null,
      bodyClass: Em.View.extend({
        templateName: require('templates/common/items_list_popup'),
        items: selectedHosts,
        insertedItems: [],
        didInsertElement: function () {
          lazyloading.run({
            destination: this.get('insertedItems'),
            source: this.get('items'),
            context: this,
            initSize: 100,
            chunkSize: 500,
            delay: 100
          });
        }
      })
    });
  },

  /**
   * Retry one host {click-handler}
   * @param {object} hostInfo
   * @method retryHost
   */
  retryHost: function (hostInfo) {
    this.retryHosts([hostInfo]);
  },

  /**
   * Retry list of hosts
   * @param {object[]} hosts
   * @method retryHosts
   */
  retryHosts: function (hosts) {
    var self = this;
    var bootStrapData = JSON.stringify({
        'verbose': true,
        'sshKey': this.get('content.installOptions.sshKey'),
        'hosts': hosts.mapProperty('name'),
        'user': this.get('content.installOptions.sshUser'),
        'sshPort': this.get('content.installOptions.sshPort'),
        'userRunAs': App.get('supports.customizeAgentUserAccount') ? this.get('content.installOptions.agentUser') : 'root'
      });
    this.set('numPolls', 0);
    this.set('registrationStartedAt', null);
    this.set('isHostsWarningsLoaded', false);
    this.set('stopChecking', false);
    this.set('isSubmitDisabled', true);
    if (this.get('content.installOptions.manualInstall')) {
      this.startRegistration();
    } else {
      App.router.get(this.get('content.controllerName')).launchBootstrap(bootStrapData, function (requestId) {
        self.set('content.installOptions.bootRequestId', requestId);
        self.doBootstrap();
      });
    }
  },

  /**
   * Retry selected hosts (click-handler)
   * @method retrySelectedHosts
   */
  retrySelectedHosts: function () {
    if (!this.get('isRetryDisabled')) {
      var selectedHosts = this.get('bootHosts').filterProperty('bootStatus', 'FAILED');
      selectedHosts.forEach(function (_host) {
        _host.set('bootStatus', 'DONE');
        _host.set('bootLog', 'Retrying ...');
      }, this);
      this.retryHosts(selectedHosts);
    }
  },

  /**
   * Init bootstrap settings and start it
   * @method startBootstrap
   */
  startBootstrap: function () {
    //this.set('isSubmitDisabled', true);    //TODO: uncomment after actual hookup
    this.set('numPolls', 0);
    this.set('registrationStartedAt', null);
    this.set('bootHosts', this.get('hosts'));
    this.doBootstrap();
  },

  /**
   * Update <code>isRegistrationInProgress</code> once
   * @method setRegistrationInProgressOnce
   */
  setRegistrationInProgressOnce: function () {
    Em.run.once(this, 'setRegistrationInProgress');
  }.observes('bootHosts.@each.bootStatus'),

  /**
   * Set <code>isRegistrationInProgress</code> value based on each host boot status
   * @method setRegistrationInProgress
   */
  setRegistrationInProgress: function () {
    var bootHosts = this.get('bootHosts');
    //if hosts aren't loaded yet then registration should be in progress
    var result = (bootHosts.length === 0 && !this.get('isLoaded'));
    for (var i = 0, l = bootHosts.length; i < l; i++) {
      if (bootHosts[i].get('bootStatus') !== 'REGISTERED' && bootHosts[i].get('bootStatus') !== 'FAILED') {
        result = true;
        break;
      }
    }
    this.set('isRegistrationInProgress', result);
  },

  /**
   * Disable wizard's previous steps (while registering)
   * @method disablePreviousSteps
   */
  disablePreviousSteps: function () {
    App.router.get('installerController.isStepDisabled').filter(function (step) {
      return step.step >= 0 && step.step <= 2;
    }).setEach('value', this.get('isBackDisabled'));
    App.router.get('addHostController.isStepDisabled').filter(function (step) {
      return step.step >= 0 && step.step <= 1;
    }).setEach('value', this.get('isBackDisabled'));
  }.observes('isBackDisabled'),

  /**
   * Close reload popup on exit from Confirm Hosts step
   * @method closeReloadPopupOnExit
   */
  closeReloadPopupOnExit: function () {
    if (this.get('stopBootstrap')) {
      this.closeReloadPopup();
    }
  }.observes('stopBootstrap'),

  /**
   * Do bootstrap calls
   * @method doBootstrap
   * @return {$.ajax|null}
   */
  doBootstrap: function () {
    if (this.get('stopBootstrap')) {
      return null;
    }
    this.incrementProperty('numPolls');

    return App.ajax.send({
      name: 'wizard.step3.bootstrap',
      sender: this,
      data: {
        bootRequestId: this.get('content.installOptions.bootRequestId'),
        numPolls: this.get('numPolls'),
        callback: this.doBootstrap,
        timeout: 3000,
        shouldUseDefaultHandler: true
      },
      success: 'doBootstrapSuccessCallback',
      error: 'reloadErrorCallback'
    });
  },

  /**
   * Success-callback for each boostrap request
   * @param {object} data
   * @method doBootstrapSuccessCallback
   */
  doBootstrapSuccessCallback: function (data) {
    var self = this;
    var pollingInterval = 3000;
    this.reloadSuccessCallback();
    if (Em.isNone(data.hostsStatus)) {
      window.setTimeout(function () {
        self.doBootstrap()
      }, pollingInterval);
    } else {
      // in case of bootstrapping just one host, the server returns an object rather than an array, so
      // force into an array
      if (!(data.hostsStatus instanceof Array)) {
        data.hostsStatus = [ data.hostsStatus ];
      }
      var keepPolling = this.parseHostInfo(data.hostsStatus);

      // Single host : if the only hostname is invalid (data.status == 'ERROR')
      // Multiple hosts : if one or more hostnames are invalid
      // following check will mark the bootStatus as 'FAILED' for the invalid hostname
      var installedHosts = App.Host.find().mapProperty('hostName');
      var isErrorStatus = data.status == 'ERROR';
      this.set('isBootstrapFailed', isErrorStatus);
      if (isErrorStatus || data.hostsStatus.mapProperty('hostName').removeObjects(installedHosts).length != this.get('bootHosts').length) {

        var hosts = this.get('bootHosts');

        for (var i = 0; i < hosts.length; i++) {

          var isValidHost = data.hostsStatus.someProperty('hostName', hosts[i].get('name'));
          if (hosts[i].get('bootStatus') !== 'REGISTERED') {
            if (!isValidHost) {
              hosts[i].set('bootStatus', 'FAILED');
              hosts[i].set('bootLog', Em.I18n.t('installer.step3.hosts.bootLog.failed'));
            }
          }
        }
      }

      if (isErrorStatus || data.hostsStatus.someProperty('status', 'DONE') || data.hostsStatus.someProperty('status', 'FAILED')) {
        // kicking off registration polls after at least one host has succeeded
        this.startRegistration();
      }
      if (keepPolling) {
        window.setTimeout(function () {
          self.doBootstrap()
        }, pollingInterval);
      }
    }
  },

  /**
   * Start hosts registration
   * @method startRegistration
   */
  startRegistration: function () {
    if (Em.isNone(this.get('registrationStartedAt'))) {
      this.set('registrationStartedAt', App.dateTime());
      this.isHostsRegistered();
    }
  },

  /**
   * Do requests to check if hosts are already registered
   * @return {$.ajax|null}
   * @method isHostsRegistered
   */
  isHostsRegistered: function () {
    if (this.get('stopBootstrap')) {
      return null;
    }
    return App.ajax.send({
      name: 'wizard.step3.is_hosts_registered',
      sender: this,
      success: 'isHostsRegisteredSuccessCallback',
      error: 'reloadErrorCallback',
      data: {
        callback: this.isHostsRegistered,
        timeout: 3000,
        shouldUseDefaultHandler: true
      }
    });
  },

  /**
   * Success-callback for registered hosts request
   * @param {object} data
   * @method isHostsRegisteredSuccessCallback
   */
  isHostsRegisteredSuccessCallback: function (data) {
    var hosts = this.get('bootHosts');
    var jsonData = data;
    this.reloadSuccessCallback();
    if (!jsonData) {
      return;
    }

    // keep polling until all hosts have registered/failed, or registrationTimeout seconds after the last host finished bootstrapping
    var stopPolling = true;
    hosts.forEach(function (_host, index) {
      // Change name of first host for test mode.
      if (App.get('testMode')) {
        if (index == 0) {
          _host.set('name', 'localhost.localdomain');
        }
      }
      // actions to take depending on the host's current bootStatus
      // RUNNING - bootstrap is running; leave it alone
      // DONE - bootstrap is done; transition to REGISTERING
      // REGISTERING - bootstrap is done but has not registered; transition to REGISTERED if host found in polling API result
      // REGISTERED - bootstrap and registration is done; leave it alone
      // FAILED - either bootstrap or registration failed; leave it alone
      switch (_host.get('bootStatus')) {
        case 'DONE':
          _host.set('bootStatus', 'REGISTERING');
          _host.set('bootLog', (_host.get('bootLog') != null ? _host.get('bootLog') : '') + Em.I18n.t('installer.step3.hosts.bootLog.registering'));
          // update registration timestamp so that the timeout is computed from the last host that finished bootstrapping
          this.set('registrationStartedAt', App.dateTime());
          stopPolling = false;
          break;
        case 'REGISTERING':
          if (jsonData.items.someProperty('Hosts.host_name', _host.name) && !jsonData.items.filterProperty('Hosts.host_name', _host.name).someProperty('Hosts.host_status', 'UNKNOWN')) {
            _host.set('bootStatus', 'REGISTERED');
            _host.set('bootLog', (_host.get('bootLog') != null ? _host.get('bootLog') : '') + Em.I18n.t('installer.step3.hosts.bootLog.registering'));
          } else {
            stopPolling = false;
          }
          break;
        case 'RUNNING':
          stopPolling = false;
          break;
        case 'REGISTERED':
        case 'FAILED':
        default:
          break;
      }
    }, this);

    if (stopPolling) {
      this.startHostcheck(hosts);
    }
    else {
      if (hosts.someProperty('bootStatus', 'RUNNING') || App.dateTime() - this.get('registrationStartedAt') < this.get('registrationTimeoutSecs') * 1000) {
        // we want to keep polling for registration status if any of the hosts are still bootstrapping (so we check for RUNNING).
        var self = this;
        window.setTimeout(function () {
          self.isHostsRegistered();
        }, 3000);
      }
      else {
        // registration timed out.  mark all REGISTERING hosts to FAILED
        hosts.filterProperty('bootStatus', 'REGISTERING').forEach(function (_host) {
          _host.set('bootStatus', 'FAILED');
          _host.set('bootLog', (_host.get('bootLog') != null ? _host.get('bootLog') : '') + Em.I18n.t('installer.step3.hosts.bootLog.failed'));
        });
        this.startHostcheck(hosts);
      }
    }
  },

  /**
   * Do request for all registered hosts
   * @return {$.ajax}
   * @method getAllRegisteredHosts
   */
  getAllRegisteredHosts: function () {
    return App.ajax.send({
      name: 'wizard.step3.is_hosts_registered',
      sender: this,
      success: 'getAllRegisteredHostsCallback'
    });
  }.observes('bootHosts'),

  /**
   * Success-callback for all registered hosts request
   * @param {object} hosts
   * @method getAllRegisteredHostsCallback
   */
  getAllRegisteredHostsCallback: function (hosts) {
    var registeredHosts = [];
    var hostsInCluster = this.get('hostsInCluster');
    var addedHosts = this.get('bootHosts').getEach('name');
    hosts.items.forEach(function (host) {
      if (!hostsInCluster.contains(host.Hosts.host_name) && !addedHosts.contains(host.Hosts.host_name)) {
        registeredHosts.push(host.Hosts.host_name);
      }
    });
    if (registeredHosts.length) {
      this.set('hasMoreRegisteredHosts', true);
      this.set('registeredHosts', registeredHosts);
    } else {
      this.set('hasMoreRegisteredHosts', false);
      this.set('registeredHosts', '');
    }
  },

  /**
   * Get JDK name from server to determine if user had setup a customized JDK path when doing 'ambari-server setup'.
   * The Ambari properties are different from default ambari-server setup, property 'jdk.name' will be missing if a customized jdk path is applied.
   * @return {$.ajax}
   * @method getJDKName
   */
  getJDKName: function () {
    return App.ajax.send({
      name: 'ambari.service',
      sender: this,
      data: {
        fields : '?fields=RootServiceComponents/properties/jdk.name,RootServiceComponents/properties/java.home,RootServiceComponents/properties/jdk_location'
      },
      success: 'getJDKNameSuccessCallback'
    });
  },

  /**
    * Success callback for JDK name, property 'jdk.name' will be missing if a customized jdk path is applied
    * @param {object} data
    * @method getJDKNameSuccessCallback
    */
  getJDKNameSuccessCallback: function (data) {
    this.set('needJDKCheckOnHosts', !data.RootServiceComponents.properties["jdk.name"]);
    this.set('jdkLocation', Em.get(data, "RootServiceComponents.properties.jdk_location"));
    this.set('javaHome', data.RootServiceComponents.properties["java.home"]);
  },

  doCheckJDK: function () {
    const hostsNames = this.get('bootHosts').filterProperty('bootStatus', 'REGISTERED').getEach('name').join(','),
      javaHome = this.get('javaHome'),
      jdkLocation = this.get('jdkLocation');
    App.ajax.send({
      name: 'wizard.step3.jdk_check',
      sender: this,
      data: {
        host_names: hostsNames,
        java_home: javaHome,
        jdk_location: jdkLocation
      },
      success: 'doCheckJDKsuccessCallback',
      error: 'doCheckJDKerrorCallback'
    });
  },

  doCheckJDKsuccessCallback: function (data) {
    if(data){
      this.set('jdkRequestIndex', data.href.split('/')[data.href.split('/').length - 1]);
    }
    if (this.get('jdkCategoryWarnings') == null) {
      // get jdk check results for all hosts
      App.ajax.send({
        name: 'wizard.step3.jdk_check.get_results',
        sender: this,
        data: {
          requestIndex: this.get('jdkRequestIndex')
        },
        success: 'parseJDKCheckResults'
      })
    } else {
      this.set('isJDKWarningsLoaded', true);
    }
  },
  doCheckJDKerrorCallback: function () {
    this.set('isJDKWarningsLoaded', true);
  },
  parseJDKCheckResults: function (data) {
    var jdkWarnings = [], hostsJDKContext = [], hostsJDKNames = [];
    // check if the request ended
    if (data.Requests.end_time > 0 && data.tasks) {
      data.tasks.forEach( function(task) {
        // generate warning context
        if (Em.get(task, "Tasks.structured_out.java_home_check.exit_code") == 1){
          var jdkContext = Em.I18n.t('installer.step3.hostWarningsPopup.jdk.context').format(task.Tasks.host_name);
          hostsJDKContext.push(jdkContext);
          hostsJDKNames.push(task.Tasks.host_name);
        }
      });
      if (hostsJDKContext.length > 0) { // java jdk warning exist
        var invalidJavaHome = this.get('javaHome');
        jdkWarnings.push({
          name: Em.I18n.t('installer.step3.hostWarningsPopup.jdk.name').format(invalidJavaHome),
          hosts: hostsJDKContext,
          hostsLong: hostsJDKContext,
          hostsNames: hostsJDKNames,
          category: 'jdk'
        });
      }
      this.set('jdkCategoryWarnings', jdkWarnings);
    } else {
      // still doing JDK check, data not ready to be parsed
      this.set('jdkCategoryWarnings', null);
    }
    this.doCheckJDKsuccessCallback();
  },

  /**
   * Check JDK issues on registered hosts.
   */
  checkHostJDK: function () {
    this.set('isJDKWarningsLoaded', false);
    this.set('jdkCategoryWarnings', null);
    var self = this;
    this.getJDKName().done( function() {
      if (self.get('needJDKCheckOnHosts')) {
        // need to do JDK check on each host
       self.doCheckJDK();
      } else {
        // no customized JDK path, so no need to check jdk
        self.set('jdkCategoryWarnings', []);
        self.set('isJDKWarningsLoaded', true);
      }
    });
  },

  startHostcheck: function(hosts) {
    if (!hosts.everyProperty('bootStatus', 'FAILED')) {
      this.set('isWarningsLoaded', false);
      this.getHostNameResolution();
      this.checkHostJDK();
    } else {
      this.stopHostCheck();
    }
  },

  _submitProceed: function () {
    this.set('confirmedHosts', this.get('bootHosts'));
    App.get('router').send('next');
  },

  /**
   * Submit-click handler
   * Disable 'Next' button while it is already under process. (using Router's property 'nextBtnClickInProgress')
   * @return {App.ModalPopup?}
   * @method submit
   */
  submit: function () {
    var self = this;

    if(App.get('router.nextBtnClickInProgress')) {
      return;
    }
    if (this.get('isHostHaveWarnings')) {
      return App.showConfirmationPopup(
        function () {
          self._submitProceed();
        },
        Em.I18n.t('installer.step3.hostWarningsPopup.hostHasWarnings'), null, Em.I18n.t('installer.step3.hostWarningsPopup.hostHasWarnings.header'), null, 'warning');
    }
    this._submitProceed();
  },

  /**
   * Show popup with host log
   * @param {object} event
   * @return {App.ModalPopup}
   */
  hostLogPopup: function (event) {
    var host = event.context;

    return App.ModalPopup.show({
      header: Em.I18n.t('installer.step3.hostLog.popup.header').format(host.get('name')),
      secondary: null,
      host: host,
      bodyClass: App.WizardStep3HostLogPopupBody
    });
  },


  /**
   * Open popup that contain hosts' warnings
   * @return {App.ModalPopup}
   * @method hostWarningsPopup
   */
  hostWarningsPopup: function () {
    var self = this;
    return App.ModalPopup.show({

      header: Em.I18n.t('installer.step3.warnings.popup.header'),

      secondary: Em.I18n.t('installer.step3.hostWarningsPopup.rerunChecks'),

      primary: Em.I18n.t('common.close'),

      autoHeight: false,

      'data-qa': 'host-checks-modal',

      onPrimary: function () {
        self.set('checksUpdateStatus', null);
        this.hide();
      },

      onClose: function () {
        self.set('checksUpdateStatus', null);
        this.hide();
      },

      onSecondary: function () {
        self.rerunChecks();
      },

      didInsertElement: function () {
        this._super();
        this.fitHeight();
      },

      footerClass: App.WizardStep3HostWarningPopupFooter.reopen({
        checkHostFinished: true
      }),

      bodyClass: App.WizardStep3HostWarningPopupBody.reopen({
        checkHostFinished: true
      })
    });
  },

  /**
   * Show popup with registered hosts
   * @return {App.ModalPopup}
   * @method registeredHostsPopup
   */
  registeredHostsPopup: function () {
    var self = this;
    return App.ModalPopup.show({
      header: Em.I18n.t('installer.step3.warning.registeredHosts').format(this.get('registeredHosts').length),
      secondary: null,
      bodyClass: Em.View.extend({
        templateName: require('templates/wizard/step3/step3_registered_hosts_popup'),
        message: Em.I18n.t('installer.step3.registeredHostsPopup'),
        registeredHosts: self.get('registeredHosts'),
        checkHostsFinished: true
      })
    })
  }

});
