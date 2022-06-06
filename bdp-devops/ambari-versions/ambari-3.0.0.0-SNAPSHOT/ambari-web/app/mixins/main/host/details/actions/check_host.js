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

App.CheckHostMixin = Em.Mixin.create({

  bootHosts: [],

  hostCheckResult: null,

  requestId: 0,

  stopChecking: false,

  /**
   * Progress value for "update hosts status" process
   * @type {number}
   */
  checksUpdateProgress: 0,

  /**
   *
   * @type {object}
   */
  checksUpdateStatus: null,


  /**
   * Contain data about installed packages on hosts
   * @type {Array}
   */
  hostsPackagesData: [],

  /**
   * @typedef {{
   *  name: string,
   *  hosts: string[],
   *  hostsLong: string[],
   *  hostsNames: string[],
   * }} checkWarning
   */

  /**
   * @type {checkWarning[]}
   */
  jdkCategoryWarnings: null,

  /**
   * @type {checkWarning[]}
   */
  hostCheckWarnings: [],

  /**
   * @type {checkWarning[]}
   */
  repoCategoryWarnings: [],

  /**
   * @type {checkWarning[]}
   */
  diskCategoryWarnings: [],

  /**
   * @type {checkWarning[]}
   */
  thpCategoryWarnings: [],

  /**
   * All hosts warnings
   * @type {object[]}
   */
  warnings: [],

  /**
   * Warnings grouped by host
   * @type {Ember.Enumerable}
   */
  warningsByHost: [],

  /**
   * This property should be overridden if being used for Add Host Wizard
   * @return {bool}
   */
  isAddHostWizard: false,

  /**
   * True if user triggered host checks rerun
   * @type {boolean}
   */
  isRerun: false,

  /**
   * Timeout for "warning"-requests
   * @type {number}
   */
  warningsTimeInterval: 60000,

  finishStates: ["FAILED", "COMPLETED", "TIMEDOUT", "ABORTED"],

  /**
   * disables host check on Add host wizard as per the experimental flag
   */
  disableHostCheck: function () {
    return this.get('content.installOptions.skipHostChecks') && this.get('isAddHostWizard');
  }.property('content.installOptions.skipHostChecks', 'isAddHostWizard'),

  /**
   * send request to create tasks for performing hosts checks
   * @params {object} data
   *    {
   *       RequestInfo: {
   *           "action": {string},
   *           "context": {string},
   *           "parameters": {
   *             "check_execute_list": {string},
   *             "jdk_location" : {string},
   *             "threshold": {string}
   *             "hosts": {string|undefined}
   *       },
   *       resource_filters: {
   *         "hosts": {string}
   *       }
   *    }
   * @returns {$.ajax}
   * @method requestToPerformHostCheck
   */
  requestToPerformHostCheck: function(data) {
    return App.ajax.send({
      name: 'preinstalled.checks',
      sender: this,
      data: {
        RequestInfo: data.RequestInfo,
        resource_filters: data.resource_filters
      },
      success: "getHostCheckSuccess",
      error: "getHostCheckError"
    })
  },

  /**
   * set all fields from which depends running host check to true value
   * which force finish checking;
   */
  stopHostCheck: function() {
    this.set('stopChecking', true);
    this.set('isJDKWarningsLoaded', true);
    this.set('isHostsWarningsLoaded', true);
  },

  getHostCheckSuccess: function(response) {
    if (!App.get('testMode')) {
      this.set("requestId", response.Requests.id);
    }
    this.getHostCheckTasks();
  },

  getHostCheckError: function() {
    this.getHostInfo();
  },

  /**
   * send ajax request to get all tasks
   * @method getHostCheckTasks
   */
  getHostCheckTasks: function () {
    var self = this;
    var requestId = this.get("requestId");
    var checker = setTimeout(function () {
      if (self.get('stopChecking') == true) {
        clearTimeout(checker);
      } else {
        App.ajax.send({
          name: 'preinstalled.checks.tasks',
          sender: self,
          data: {
            requestId: requestId
          },
          success: 'getHostCheckTasksSuccess',
          error: 'getHostCheckTasksError'
        });
      }
    }, 1000);
  },

  /**
   * add warnings to host warning popup if needed
   * @param data {Object} - json
   * @method getHostCheckTasksSuccess
   */
  getHostCheckTasksSuccess: function (data) {
    if (!data) {
      //if resolution host check has corrupted data then skip it
      return this.getGeneralHostCheck();
    }
    if (this.get('finishStates').contains(data.Requests.request_status)) {
      if (data.Requests.inputs.indexOf("last_agent_env_check") != -1) {
        this.set('stopChecking', true);
        this.set('hostsPackagesData', data.tasks.map(function (task) {
          var installed_packages = Em.get(task, 'Tasks.structured_out.installed_packages');
          return {
            hostName: Em.get(task, 'Tasks.host_name'),
            transparentHugePage: Em.get(task, 'Tasks.structured_out.transparentHugePage.message'),
            installedPackages: installed_packages && Array.isArray(installed_packages) ? installed_packages : []
          };
        }));

        this.set("hostCheckResult", data); //store the data so that it can be used later on in the getHostInfo handling logic.
        /**
         * Still need to get host info for checks that the host check does not perform currently
         * Such as the OS type check and the disk space check
         * */
        this.getHostInfo();
      } else if (data.Requests.inputs.indexOf("host_resolution_check") != -1) {
        this.parseHostNameResolution(data);
        this.getGeneralHostCheck();
      }
    } else {
      this.getHostCheckTasks();
    }
  },

  /**
   * @method getHostCheckTasksError
   */
  getHostCheckTasksError: function() {
    this.set('stopChecking', true);
  },

  /**
   * Get disk info and cpu count of booted hosts from server
   * @return {$.ajax}
   * @method getHostInfo
   */
  getHostInfo: function () {
    this.set('isHostsWarningsLoaded', false);
    // begin JDK check for each host
    return App.ajax.send({
      name: 'wizard.step3.host_info',
      sender: this,
      success: 'getHostInfoSuccessCallback',
      error: 'getHostInfoErrorCallback'
    });
  },

  /**
   * Success-callback for hosts info request
   * @param {object} jsonData
   * @method getHostInfoSuccessCallback
   */
  getHostInfoSuccessCallback: function (jsonData) {
    var hosts = this.get('bootHosts'),
      self = this,
      repoWarnings = [], hostsRepoNames = [], hostsContext = [],
      diskWarnings = [], hostsDiskContext = [], hostsDiskNames = [],
      thpWarnings = [], thpContext = [], thpHostsNames = [];

    // parse host checks warning
    var hostCheckResult = this.get("hostCheckResult");
    if(hostCheckResult){
      this.parseHostCheckWarnings(hostCheckResult);
      this.set("hostCheckResult", null);
    } else {
      this.parseWarnings(jsonData);
    }
    this.set('isHostsWarningsLoaded', true);
    hosts.forEach(function (_host) {

      var host = (App.get('testMode')) ? jsonData.items[0] : jsonData.items.findProperty('Hosts.host_name', _host.name);
      if (App.get('skipBootstrap')) {
        self._setHostDataWithSkipBootstrap(_host);
      }
      else {
        if (host) {
          self._setHostDataFromLoadedHostInfo(_host, host);
          var host_name = Em.get(host, 'Hosts.host_name');

          var context = self.checkHostOSType(host.Hosts.os_family, host_name);
          if (context) {
            hostsContext.push(context);
            hostsRepoNames.push(host_name);
          }
          var diskContext = self.checkHostDiskSpace(host_name, host.Hosts.disk_info);
          if (diskContext) {
            hostsDiskContext.push(diskContext);
            hostsDiskNames.push(host_name);
          }
          // "Transparent Huge Pages" check
          var _hostPackagesData = self.get('hostsPackagesData').findProperty('hostName', host.Hosts.host_name);
          if (_hostPackagesData) {
            var transparentHugePage = _hostPackagesData.transparentHugePage;
            context = self.checkTHP(host_name, transparentHugePage);
          } else {
            context = self.checkTHP(host_name, Em.get(host, 'Hosts.last_agent_env.transparentHugePage'));
          }
          if (context) {
            thpContext.push(context);
            thpHostsNames.push(host_name);
          }
        }
      }
    });
    if (hostsContext.length > 0) { // repository warning exist
      repoWarnings.push({
        name: Em.I18n.t('installer.step3.hostWarningsPopup.repositories.name'),
        hosts: hostsContext,
        hostsLong: hostsContext,
        hostsNames: hostsRepoNames,
        category: 'repositories'
      });
    }
    if (hostsDiskContext.length > 0) { // disk space warning exist
      diskWarnings.push({
        name: Em.I18n.t('installer.step3.hostWarningsPopup.disk.name'),
        hosts: hostsDiskContext,
        hostsLong: hostsDiskContext,
        hostsNames: hostsDiskNames,
        category: 'disk'
      });
    }
    if (thpContext.length > 0) { // THP warning existed
      thpWarnings.push({
        name: Em.I18n.t('installer.step3.hostWarningsPopup.thp.name'),
        hosts: thpContext,
        hostsLong: thpContext,
        hostsNames: thpHostsNames,
        category: 'thp'
      });
    }

    this.set('repoCategoryWarnings', repoWarnings);
    this.set('diskCategoryWarnings', diskWarnings);
    this.set('thpCategoryWarnings', thpWarnings);
    this.stopRegistration();
    this.get('name') === 'mainHostDetailsController' && !this.get('isRerun') && this.set('checkHostFinished', true);
  },

  /**
   * Error-callback for hosts info request
   * @method getHostInfoErrorCallback
   */
  getHostInfoErrorCallback: function () {
    this.set('isHostsWarningsLoaded', true);
    this.registerErrPopup(Em.I18n.t('installer.step3.hostInformation.popup.header'), Em.I18n.t('installer.step3.hostInformation.popup.body'));
  },

  /**
   * parse warnings for host names resolution only
   * @param {object} data
   * @method parseHostNameResolution
   */
  parseHostNameResolution: function (data) {
    if (!data) {
      return;
    }
    data.tasks.forEach(function (task) {
      var name = Em.I18n.t('installer.step3.hostWarningsPopup.resolution.validation.error');
      var hostInfo = this.get("hostCheckWarnings").findProperty('name', name);
      if (this.get('finishStates').contains(task.Tasks.status)) {
        if (task.Tasks.status === "COMPLETED" && !!Em.get(task, "Tasks.structured_out.host_resolution_check.failed_count")) {
          var targetHostName = Em.get(task, "Tasks.host_name");
          var relatedHostNames = Em.get(task, "Tasks.structured_out.host_resolution_check.hosts_with_failures") || [];
          var contextMessage = Em.I18n.t('installer.step3.hostWarningsPopup.resolution.validation.context').format(targetHostName, relatedHostNames.length + ' ' + Em.I18n.t('installer.step3.hostWarningsPopup.host' + (relatedHostNames.length == 1 ? '' : 's')));
          var contextMessageLong = Em.I18n.t('installer.step3.hostWarningsPopup.resolution.validation.context').format(targetHostName, relatedHostNames.join(', '));
          if (!hostInfo) {
            hostInfo = {
              name: name,
              hosts: [contextMessage],
              hostsLong: [contextMessageLong],
              hostsNames: [targetHostName]
            };
            this.get("hostCheckWarnings").push(hostInfo);
          } else {
            if (!hostInfo.hostsNames.contains(targetHostName)) {
              hostInfo.hosts.push(contextMessage);
              hostInfo.hostsLong.push(contextMessageLong);
              hostInfo.hostsNames.push(targetHostName);
            }
          }
        }
      }
    }, this);
  },

  getGeneralHostCheck: function (dataForHostCheck) {
    if (App.get('testMode')) {
      this.getHostInfo();
    } else {
      var data = dataForHostCheck || this.getDataForCheckRequest("last_agent_env_check,installed_packages,existing_repos,transparentHugePage", false);
      data ? this.requestToPerformHostCheck(data) : this.stopHostCheck();
    }
  },

  getHostNameResolution: function (dataForHostCheck) {
    if (App.get('testMode')) {
      this.getHostCheckSuccess();
    } else {
      var data = dataForHostCheck || this.getDataForCheckRequest("host_resolution_check", true);
      if (data && !this.get('disableHostCheck')) {
        this.requestToPerformHostCheck(data);
      } else {
        this.stopHostCheck();
        this.stopRegistration();
      }
    }
  },

  /**
   * Check warnings from server and put it in parsing
   * @method rerunChecks
   */
  rerunChecks: function () {
    var self = this;
    var currentProgress = 0;
    this.get('name') === 'wizardStep3Controller' ? this.getHostNameResolution() : this.getHostNameResolution(this.getDataForHostCheck());
    this.setProperties({
      stopChecking: false,
      isRerun: true
    });
    this.get('name') === 'wizardStep3Controller' ? this.getGeneralHostCheck() : this.getGeneralHostCheck(this.getDataForHostCheck());
    this.get('name') === 'wizardStep3Controller' && this.checkHostJDK();
    var interval = setInterval(function () {
      currentProgress += 100000 / self.get('warningsTimeInterval');
      if (currentProgress < 100) {
        self.set('checksUpdateProgress', currentProgress);
      } else {
        clearInterval(interval);
        App.ajax.send({
          name: 'wizard.step3.rerun_checks',
          sender: self,
          success: 'rerunChecksSuccessCallback',
          error: 'rerunChecksErrorCallback',
          callback: function () {
            self.set('isRerun', false);
            self.get('name') === 'mainHostDetailsController' && self.set('checkHostFinished', true);
          }
        });
      }
    }, 1000);
  },

  /**
   * Success-callback for rerun request
   * @param {object} data
   * @method rerunChecksSuccessCallback
   */
  rerunChecksSuccessCallback: function (data) {
    this.set('checksUpdateProgress', 100);
    this.set('checksUpdateStatus', 'SUCCESS');
    this.parseWarnings(data);
  },

  /**
   * Error-callback for rerun request
   * @method rerunChecksErrorCallback
   */
  rerunChecksErrorCallback: function () {
    this.set('checksUpdateProgress', 100);
    this.set('checksUpdateStatus', 'FAILED');
  },

  /**
   * generates data for reuest to perform check
   * @param {string} checkExecuteList - for now supported:
   *  <code>"last_agent_env_check"<code>
   *  <code>"host_resolution_check"<code>
   * @param {boolean} addHostsParameter - define whether add hosts parameter to RequestInfo
   * @return {object|null}
   * @method getDataForCheckRequest
   */
  getDataForCheckRequest: function (checkExecuteList, addHostsParameter) {
    var newHosts = this.get('bootHosts').filterProperty('bootStatus', 'REGISTERED').getEach('name');
    var hosts = this.get('isAddHostWizard') ? [].concat.apply([], App.MasterComponent.find().mapProperty('hostNames')).concat(newHosts).uniq() : newHosts;
    hosts = hosts.join(',');
    if (hosts.length == 0) return null;
    var jdk_location = App.router.get('clusterController.ambariProperties.jdk_location');
    var RequestInfo = {
      "action": "check_host",
      "context": "Check host",
      "parameters": {
        "check_execute_list": checkExecuteList,
        "jdk_location" : jdk_location,
        "threshold": "20"
      }
    };
    if (addHostsParameter) {
      RequestInfo.parameters.hosts = hosts;
    }
    var resource_filters = {
      "hosts": hosts
    };
    return {
      RequestInfo: RequestInfo,
      resource_filters: resource_filters
    }
  },

  /**
   * Show popup with regitration error-message
   * @param {string} header
   * @param {string} message
   * @return {App.ModalPopup}
   * @method registerErrPopup
   */
  registerErrPopup: function (header, message) {
    return App.ModalPopup.show({
      header: header,
      secondary: false,
      bodyClass: Em.View.extend({
        template: Em.Handlebars.compile('<p>{{view.message}}</p>'),
        message: message
      })
    });
  },

  parseHostCheckWarnings: function (data) {
    data = App.get('testMode') ? data : this.filterHostsData(data);
    var warnings = [];
    var warning;
    var hosts = [];
    var warningCategories = {
      fileFoldersWarnings: {},
      packagesWarnings: {},
      processesWarnings: {},
      servicesWarnings: {},
      usersWarnings: {},
      alternativeWarnings: {}
    };

    var hostsPackagesData = this.get('hostsPackagesData');
    data.tasks.sortPropertyLight('Tasks.host_name').forEach(function (_task) {
      var hostName = _task.Tasks.host_name;
      var host = {
        name: hostName,
        warnings: []
      };

      if (!_task.Tasks.structured_out || !_task.Tasks.structured_out.last_agent_env_check) {
        return;
      }

      var lastAgentEnvCheck = _task.Tasks.structured_out.last_agent_env_check;

      //parse all directories and files warnings for host
      var stackFoldersAndFiles = lastAgentEnvCheck.stackFoldersAndFiles || [];
      stackFoldersAndFiles.forEach(function (path) {
        warning = warningCategories.fileFoldersWarnings[path.name];
        if (warning) {
          warning.hosts.push(hostName);
          warning.hostsLong.push(hostName);
        } else {
          warningCategories.fileFoldersWarnings[path.name] = warning = {
            name: path.name,
            hosts: [hostName],
            hostsLong: [hostName],
            category: 'fileFolders'
          };
        }
        host.warnings.push(warning);
      }, this);

      //parse all package warnings for host
      var _hostPackagesData = hostsPackagesData.findProperty('hostName', hostName);

      if (_hostPackagesData) {
        _hostPackagesData.installedPackages.forEach(function (_package) {
          warning = warningCategories.packagesWarnings[_package.name];
          if (warning) {
            warning.hosts.push(hostName);
            warning.hostsLong.push(hostName);
            warning.version = _package.version;
          } else {
            warningCategories.packagesWarnings[_package.name] = warning = {
              name: _package.name,
              version: _package.version,
              hosts: [hostName],
              hostsLong: [hostName],
              category: 'packages'
            };
          }
          host.warnings.push(warning);
        }, this);
      }

      //parse all process warnings for host
      var hostHealth = lastAgentEnvCheck.hostHealth;

      var liveServices = null;
      var javaProcs = null;

      if(hostHealth) {
        if(hostHealth.activeJavaProcs)
          javaProcs = hostHealth.activeJavaProcs;
        if(hostHealth.liveServices)
          liveServices = hostHealth.liveServices;
      }

      if (javaProcs) {
        javaProcs.forEach(function (process) {
          warning = warningCategories.processesWarnings[process.pid];
          if (warning) {
            warning.hosts.push(hostName);
            warning.hostsLong.push(hostName);
          } else {
            warningCategories.processesWarnings[process.pid] = warning = {
              name: (process.command.substr(0, 35) + '...'),
              hosts: [hostName],
              hostsLong: [hostName],
              category: 'processes',
              user: process.user,
              pid: process.pid,
              command: '<table><tr><td style="word-break: break-all;">' +
              ((process.command.length < 500) ? process.command : process.command.substr(0, 230) + '...' +
              '<p style="text-align: center">................</p>' +
              '...' + process.command.substr(-230)) + '</td></tr></table>'
            };
          }
          host.warnings.push(warning);
        }, this);
      }

      //parse all service warnings for host
      if (liveServices) {
        liveServices.forEach(function (service) {
          if (service.status === 'Unhealthy') {
            warning = warningCategories.servicesWarnings[service.name];
            if (warning) {
              warning.hosts.push(hostName);
              warning.hostsLong.push(hostName);
            } else {
              warningCategories.servicesWarnings[service.name] = warning = {
                name: service.name,
                hosts: [hostName],
                hostsLong: [hostName],
                category: 'services'
              };
            }
            host.warnings.push(warning);
          }
        }, this);
      }
      //parse all user warnings for host
      var existingUsers = lastAgentEnvCheck.existingUsers;
      if (existingUsers) {
        existingUsers.forEach(function (user) {
          warning = warningCategories.usersWarnings[user.name];
          if (warning) {
            warning.hosts.push(hostName);
            warning.hostsLong.push(hostName);
          } else {
            warningCategories.usersWarnings[user.name] = warning = {
              name: user.name,
              hosts: [hostName],
              hostsLong: [hostName],
              category: 'users'
            };
          }
          host.warnings.push(warning);
        }, this);
      }

      //parse misc warnings for host
      var umask = lastAgentEnvCheck.umask;
      if (umask && umask > 23) {
        warning = warnings.filterProperty('category', 'misc').findProperty('name', umask);
        if (warning) {
          warning.hosts.push(hostName);
          warning.hostsLong.push(hostName);
        } else {
          warning = {
            name: umask,
            hosts: [hostName],
            hostsLong: [hostName],
            category: 'misc'
          };
          warnings.push(warning);
        }
        host.warnings.push(warning);
      }

      var firewallRunning = lastAgentEnvCheck.firewallRunning;
      if (firewallRunning !== null && firewallRunning) {
        var name = lastAgentEnvCheck.firewallName + " Running";
        warning = warnings.filterProperty('category', 'firewall').findProperty('name', name);
        if (warning) {
          warning.hosts.push(hostName);
          warning.hostsLong.push(hostName);
        } else {
          warning = {
            name: name,
            hosts: [hostName],
            hostsLong: [hostName],
            category: 'firewall'
          };
          warnings.push(warning);
        }
        host.warnings.push(warning);
      }

      if (lastAgentEnvCheck.alternatives) {
        lastAgentEnvCheck.alternatives.forEach(function (alternative) {
          warning = warningCategories.alternativeWarnings[alternative.name];
          if (warning) {
            warning.hosts.push(hostName);
            warning.hostsLong.push(hostName);
          } else {
            warningCategories.alternativeWarnings[alternative.name] = warning = {
              name: alternative.name,
              target: alternative.target,
              hosts: [hostName],
              hostsLong: [hostName],
              category: 'alternatives'
            };
          }
          host.warnings.push(warning);
        }, this);
      }

      if (lastAgentEnvCheck.reverseLookup === false) {
        var name = Em.I18n.t('installer.step3.hostWarningsPopup.reverseLookup.name');
        warning = warnings.filterProperty('category', 'reverseLookup').findProperty('name', name);
        if (warning) {
          warning.hosts.push(hostName);
          warning.hostsLong.push(hostName);
        } else {
          warning = {
            name: name,
            hosts: [hostName],
            hostsLong: [hostName],
            category: 'reverseLookup'
          };
          warnings.push(warning);
        }
        host.warnings.push(warning);
      }
      hosts.push(host);
    }, this);

    for (var categoryId in warningCategories) {
      var category = warningCategories[categoryId];
      for (var warningId in category) {
        warnings.push(category[warningId]);
      }
    }

    hosts.unshift({
      name: 'All Hosts',
      warnings: warnings
    });
    this.set('warnings', warnings);
    this.set('warningsByHost', hosts);
  },

  /**
   * Parse warnings data for each host and total
   * @param {object} data
   * @method parseWarnings
   */
  parseWarnings: function (data) {
    data = App.get('testMode') ? data : this.filterBootHosts(data);
    var warnings = [];
    var warning;
    var hosts = [];
    var warningCategories = {
      fileFoldersWarnings: {},
      packagesWarnings: {},
      processesWarnings: {},
      servicesWarnings: {},
      usersWarnings: {},
      alternativeWarnings: {}
    };
    var hostsPackagesData = this.get('hostsPackagesData');

    data.items.sortPropertyLight('Hosts.host_name').forEach(function (_host) {
      var host = {
        name: _host.Hosts.host_name,
        warnings: []
      };
      if (!_host.Hosts.last_agent_env) {
        // in some unusual circumstances when last_agent_env is not available from the _host,
        // skip the _host and proceed to process the rest of the hosts.
        return;
      }

      //parse all directories and files warnings for host

      //todo: to be removed after check in new API
      var stackFoldersAndFiles = _host.Hosts.last_agent_env.stackFoldersAndFiles || [];
      stackFoldersAndFiles.forEach(function (path) {
        warning = warningCategories.fileFoldersWarnings[path.name];
        if (warning) {
          warning.hosts.push(_host.Hosts.host_name);
          warning.hostsLong.push(_host.Hosts.host_name);
        } else {
          warningCategories.fileFoldersWarnings[path.name] = warning = {
            name: path.name,
            hosts: [_host.Hosts.host_name],
            hostsLong: [_host.Hosts.host_name],
            category: 'fileFolders'
          };
        }
        host.warnings.push(warning);
      }, this);

      //parse all package warnings for host
      var _hostPackagesData = hostsPackagesData.findProperty('hostName', _host.Hosts.host_name);

      if (_hostPackagesData) {
        _hostPackagesData.installedPackages.forEach(function (_package) {
          warning = warningCategories.packagesWarnings[_package.name];
          if (warning) {
            warning.hosts.push(_host.Hosts.host_name);
            warning.hostsLong.push(_host.Hosts.host_name);
            warning.version = _package.version;
          } else {
            warningCategories.packagesWarnings[_package.name] = warning = {
              name: _package.name,
              version: _package.version,
              hosts: [_host.Hosts.host_name],
              hostsLong: [_host.Hosts.host_name],
              category: 'packages'
            };
          }
          host.warnings.push(warning);
        }, this);
      }

      //parse all process warnings for host

      //todo: to be removed after check in new API
      var javaProcs = _host.Hosts.last_agent_env.hostHealth ? _host.Hosts.last_agent_env.hostHealth.activeJavaProcs : _host.Hosts.last_agent_env.javaProcs;
      if (javaProcs) {
        javaProcs.forEach(function (process) {
          warning = warningCategories.processesWarnings[process.pid];
          if (warning) {
            warning.hosts.push(_host.Hosts.host_name);
            warning.hostsLong.push(_host.Hosts.host_name);
          } else {
            warningCategories.processesWarnings[process.pid] = warning = {
              name: (process.command.substr(0, 35) + '...'),
              hosts: [_host.Hosts.host_name],
              hostsLong: [_host.Hosts.host_name],
              category: 'processes',
              user: process.user,
              pid: process.pid,
              command: '<table><tr><td style="word-break: break-all;">' +
              ((process.command.length < 500) ? process.command : process.command.substr(0, 230) + '...' +
              '<p style="text-align: center">................</p>' +
              '...' + process.command.substr(-230)) + '</td></tr></table>'
            };
          }
          host.warnings.push(warning);
        }, this);
      }

      //parse all service warnings for host

      //todo: to be removed after check in new API
      if (_host.Hosts.last_agent_env.hostHealth && _host.Hosts.last_agent_env.hostHealth.liveServices) {
        _host.Hosts.last_agent_env.hostHealth.liveServices.forEach(function (service) {
          if (service.status === 'Unhealthy') {
            warning = warningCategories.servicesWarnings[service.name];
            if (warning) {
              warning.hosts.push(_host.Hosts.host_name);
              warning.hostsLong.push(_host.Hosts.host_name);
            } else {
              warningCategories.servicesWarnings[service.name] = warning = {
                name: service.name,
                hosts: [_host.Hosts.host_name],
                hostsLong: [_host.Hosts.host_name],
                category: 'services'
              };
            }
            host.warnings.push(warning);
          }
        }, this);
      }
      //parse all user warnings for host

      //todo: to be removed after check in new API
      if (_host.Hosts.last_agent_env.existingUsers) {
        _host.Hosts.last_agent_env.existingUsers.forEach(function (user) {
          warning = warningCategories.usersWarnings[user.name];
          if (warning) {
            warning.hosts.push(_host.Hosts.host_name);
            warning.hostsLong.push(_host.Hosts.host_name);
          } else {
            warningCategories.usersWarnings[user.name] = warning = {
              name: user.name,
              hosts: [_host.Hosts.host_name],
              hostsLong: [_host.Hosts.host_name],
              category: 'users'
            };
          }
          host.warnings.push(warning);
        }, this);
      }

      //parse misc warnings for host
      var umask = _host.Hosts.last_agent_env.umask;
      if (umask && umask > 23) {
        warning = warnings.filterProperty('category', 'misc').findProperty('name', umask);
        if (warning) {
          warning.hosts.push(_host.Hosts.host_name);
          warning.hostsLong.push(_host.Hosts.host_name);
        } else {
          warning = {
            name: umask,
            hosts: [_host.Hosts.host_name],
            hostsLong: [_host.Hosts.host_name],
            category: 'misc'
          };
          warnings.push(warning);
        }
        host.warnings.push(warning);
      }

      var firewallRunning = _host.Hosts.last_agent_env.firewallRunning;
      if (firewallRunning !== null && firewallRunning) {
        var name = _host.Hosts.last_agent_env.firewallName + " Running";
        warning = warnings.filterProperty('category', 'firewall').findProperty('name', name);
        if (warning) {
          warning.hosts.push(_host.Hosts.host_name);
          warning.hostsLong.push(_host.Hosts.host_name);
        } else {
          warning = {
            name: name,
            hosts: [_host.Hosts.host_name],
            hostsLong: [_host.Hosts.host_name],
            category: 'firewall'
          };
          warnings.push(warning);
        }
        host.warnings.push(warning);
      }

      if (_host.Hosts.last_agent_env.alternatives) {
        _host.Hosts.last_agent_env.alternatives.forEach(function (alternative) {
          warning = warningCategories.alternativeWarnings[alternative.name];
          if (warning) {
            warning.hosts.push(_host.Hosts.host_name);
            warning.hostsLong.push(_host.Hosts.host_name);
          } else {
            warningCategories.alternativeWarnings[alternative.name] = warning = {
              name: alternative.name,
              target: alternative.target,
              hosts: [_host.Hosts.host_name],
              hostsLong: [_host.Hosts.host_name],
              category: 'alternatives'
            };
          }
          host.warnings.push(warning);
        }, this);
      }

      if (_host.Hosts.last_agent_env.reverseLookup === false) {
        var name = Em.I18n.t('installer.step3.hostWarningsPopup.reverseLookup.name');
        warning = warnings.filterProperty('category', 'reverseLookup').findProperty('name', name);
        if (warning) {
          warning.hosts.push(_host.Hosts.host_name);
          warning.hostsLong.push(_host.Hosts.host_name);
        } else {
          warning = {
            name: name,
            hosts: [_host.Hosts.host_name],
            hostsLong: [_host.Hosts.host_name],
            category: 'reverseLookup'
          };
          warnings.push(warning);
        }
        host.warnings.push(warning);
      }
      hosts.push(host);
    }, this);

    for (var categoryId in warningCategories) {
      var category = warningCategories[categoryId];
      for (var warningId in category) {
        warnings.push(category[warningId]);
      }
    }

    hosts.unshift({
      name: 'All Hosts',
      warnings: warnings
    });
    this.set('warnings', warnings);
    this.set('warningsByHost', hosts);
  },


  /**
   * Filter data for warnings parse
   * is data from host in bootStrap
   * @param {object} data
   * @return {Object}
   * @method filterBootHosts
   */
  filterBootHosts: function (data) {
    var bootHostNames = {};
    this.get('bootHosts').forEach(function (bootHost) {
      bootHostNames[bootHost.get('name')] = true;
    });
    var filteredData = {
      href: data.href,
      items: []
    };
    data.items.forEach(function (host) {
      if (bootHostNames[host.Hosts.host_name]) {
        filteredData.items.push(host);
      }
    });
    return filteredData;
  },

  /**
   * Set metrics to host object
   * Used when <code>App.skipBootstrap</code> is true
   * @param {Ember.Object} host
   * @returns {object}
   * @private
   * @methos _setHostDataWithSkipBootstrap
   */
  _setHostDataWithSkipBootstrap: function(host) {
    host.set('cpu', 2);
    host.set('memory', ((parseInt(2000000))).toFixed(2));
    host.set('disk_info', [
      {"mountpoint": "/", "type": "ext4"},
      {"mountpoint": "/grid/0", "type": "ext4"},
      {"mountpoint": "/grid/1", "type": "ext4"},
      {"mountpoint": "/grid/2", "type": "ext4"}
    ]);
    return host;
  },

  /**
   * Set loaded metrics to host object
   * @param {object} host
   * @param {object} hostInfo
   * @returns {object}
   * @method _setHostDataFromLoadedHostInfo
   * @private
   */
  _setHostDataFromLoadedHostInfo: function(host, hostInfo) {
    host.set('cpu', Em.get(hostInfo, 'Hosts.cpu_count'));
    host.set('memory', ((parseInt(Em.get(hostInfo, 'Hosts.total_mem')))).toFixed(2));
    host.set('disk_info', Em.get(hostInfo, 'Hosts.disk_info').filter(function (h) {
      return h.mountpoint != "/boot"
    }));
    host.set('os_type', Em.get(hostInfo, 'Hosts.os_type'));
    host.set('os_family', Em.get(hostInfo, 'Hosts.os_family'));
    host.set('os_arch', Em.get(hostInfo, 'Hosts.os_arch'));
    host.set('ip', Em.get(hostInfo, 'Hosts.ip'));
    return host;
  },

  /**
   * Check if the customized os group contains the registered host os type. If not the repo on that host is invalid.
   * @param {string} osType
   * @param {string} hostName
   * @return {string} error-message or empty string
   * @method checkHostOSType
   */
  checkHostOSType: function (osFamily, hostName) {
    if (this.get('content.stacks')) {
      var selectedStack = this.get('content.stacks').findProperty('isSelected', true);
      var selectedOS = [];
      var isValid = false;
      if (selectedStack && selectedStack.get('operatingSystems')) {
        selectedStack.get('operatingSystems').filterProperty('isSelected', true).forEach(function (os) {
          selectedOS.pushObject(os.get('osType'));
          if (os.get('osType') === osFamily) {
            isValid = true;
          }
        });
      }
      if (isValid) {
        return '';
      } else {
        return Em.I18n.t('installer.step3.hostWarningsPopup.repositories.context').format(hostName, osFamily, selectedOS.uniq());
      }
    } else {
      return '';
    }
  },

  /**
   * Check if current host has enough free disk usage.
   * @param {string} hostName
   * @param {object} diskInfo
   * @return {string} error-message or empty string
   * @method checkHostDiskSpace
   */
  checkHostDiskSpace: function (hostName, diskInfo) {
    var minFreeRootSpace = App.minDiskSpace * 1024 * 1024; //in kilobyte
    var minFreeUsrLibSpace = App.minDiskSpaceUsrLib * 1024 * 1024; //in kilobyte
    var warningString = '';

    diskInfo.forEach(function (info) {
      switch (info.mountpoint) {
        case '/':
          warningString = info.available < minFreeRootSpace ?
          Em.I18n.t('installer.step3.hostWarningsPopup.disk.context2').format(App.minDiskSpace + 'GB', info.mountpoint) + ' ' + warningString :
            warningString;
          break;
        case '/usr':
        case '/usr/lib':
          warningString = info.available < minFreeUsrLibSpace ?
          Em.I18n.t('installer.step3.hostWarningsPopup.disk.context2').format(App.minDiskSpaceUsrLib + 'GB', info.mountpoint) + ' ' + warningString :
            warningString;
          break;
        default:
          break;
      }
    });
    if (warningString) {
      return Em.I18n.t('installer.step3.hostWarningsPopup.disk.context1').format(hostName) + ' ' + warningString;
    } else {
      return '';
    }
  },

  /**
   * Check if the 'Transparent Huge Pages' enabled.
   * @param {string} transparentHugePage
   * @param {string} hostName
   * @return {string} error-message or empty string
   * @method checkTHP
   */
  checkTHP: function (hostName, transparentHugePage) {
    if (transparentHugePage == "always") {
      return Em.I18n.t('installer.step3.hostWarningsPopup.thp.context').format(hostName);
    } else {
      return '';
    }
  },

  /**
   * Enable or disable submit/retry buttons according to hosts boot statuses
   * @method stopRegistration
   */
  stopRegistration: function () {
    this.set('isSubmitDisabled', !this.get('bootHosts').someProperty('bootStatus', 'REGISTERED'));
  },

  /**
   * Filter data for warnings parse
   * is data from host in bootStrap
   * @param {object} data
   * @return {Object}
   * @method filterBootHosts
   */
  filterHostsData: function (data) {
    var bootHostNames = {};
    this.get('bootHosts').forEach(function (bootHost) {
      bootHostNames[bootHost.get('name')] = true;
    });
    var filteredData = {
      href: data.href,
      tasks: []
    };
    data.tasks.forEach(function (_task) {
      if (bootHostNames[_task.Tasks.host_name]) {
        filteredData.tasks.push(_task);
      }
    });
    return filteredData;
  },
});