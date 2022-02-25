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

App.WizardStep10Controller = Em.Controller.extend({

  /**
   * List of data about installed cluster components (hosts, services etc)
   * @type {Ember.Object[]}
   */
  clusterInfo: [],

  /**
   * is Add service wizard the ongoing wizard
   * @type {bool}
   */
  isAddServiceWizard: Em.computed.equal('content.controllerName', 'addServiceController'),

  /**
   * Clear <code>clusterInfo</code>
   * @method clearStep
   */
  clearStep: function () {
    this.get('clusterInfo').clear();
  },

  /**
   * @method loadStep
   */
  loadStep: function () {
    this.clearStep();
    this.loadInstalledHosts(this.loadRegisteredHosts());
    var installFlag = true;
    var startFlag = true;
    if (this.get('content.controllerName') == 'installerController') {
      installFlag = this.loadMasterComponents();
      startFlag = this.loadStartedServices();
    }
    if (installFlag && startFlag) {
      this.loadInstallTime();
    }
  },

  /**
   * Get list of clusterInfo object about registered hosts
   * @returns {{id: number, color: string, displayStatement: string, status: array}}
   * @method loadRegisteredHosts
   */
  loadRegisteredHosts: function () {
    var masterHosts = this.get('content.masterComponentHosts').mapProperty('hostName').uniq();
    var slaveHosts = this.get('content.slaveComponentHosts');
    var hostObj = [];
    slaveHosts.forEach(function (_hosts) {
      hostObj = hostObj.concat(_hosts.hosts);
    }, this);
    slaveHosts = hostObj.mapProperty('hostName').uniq();
    var registeredHosts = App.Host.find().mapProperty('hostName').concat(masterHosts.concat(slaveHosts)).uniq();
    var registerHostsStatement = Em.I18n.t('installer.step10.hostsSummary').format(registeredHosts.length);
    if (this.get('content.controllerName') === 'addHostController') {
      registerHostsStatement = Em.I18n.t('installer.step10.hostsSummary').format(Object.keys(this.get('content.hosts') || {}).length);
    }
    var registerHostsObj = Em.Object.create({
      id: 1,
      color: 'text-info',
      displayStatement: registerHostsStatement,
      status: []
    });
    this.get('clusterInfo').pushObject(registerHostsObj);

    return registerHostsObj;
  },

  /**
   * Push info about installed hosts to <code>clusterInfo</code>
   * @method loadInstalledHosts
   */
  loadInstalledHosts: function () {
    var hosts = this.get('content.hosts');
    var hostsInfo = [];
    for (var index in hosts) {
      if (hosts.hasOwnProperty(index)) {
        hostsInfo.pushObject(hosts[index]);
      }
    }
    var succeededHosts = hostsInfo.filterProperty('status', 'success');
    var warnedHosts = hostsInfo.filter(function(host) {
      return ['warning', 'failed'].contains(host.status);
    });
    if (succeededHosts.length) {
      var successMessage = this.get('content.cluster.status') === 'START_SKIPPED' ? Em.I18n.t('installer.step10.installed') : Em.I18n.t('installer.step10.installedAndStarted');
      var successStatement = successMessage.format(succeededHosts.length) + ((succeededHosts.length > 1) ? Em.I18n.t('installer.step8.hosts') : Em.I18n.t('installer.step8.host'));
      this.get('clusterInfo').findProperty('id', 1).get('status').pushObject(Em.Object.create({
        id: 1,
        color: 'text-success',
        displayStatement: successStatement
      }));
    }

    if (warnedHosts.length) {
      var warnStatement = warnedHosts.length + Em.I18n.t('installer.step10.warnings');
      this.get('clusterInfo').findProperty('id', 1).get('status').pushObject(Em.Object.create({
        id: 2,
        color: 'text-warning',
        displayStatement: warnStatement,
        statements: []
      }));
      warnedHosts.forEach(function (_host) {
        var clusterState = '';
        if (this.get('content.cluster.status') === 'INSTALL FAILED') {
          clusterState = Em.I18n.t('installer.step10.clusterState.installing');
        }
        else {
          if (this.get('content.cluster.status') === 'START FAILED') {
            clusterState = Em.I18n.t('installer.step10.clusterState.starting');
          }
        }

        var self = this;
        Em.A([
            {Tst: 'FAILED', st: 'failed'},
            {Tst: 'ABORTED', st: 'aborted'},
            {Tst: 'TIMEDOUT', st: 'timedout'}
          ]).forEach(function (s) {
            _host.tasks.filterProperty('Tasks.status', s.Tst).forEach(function (_task) {
              var statement = clusterState + App.format.role(_task.Tasks.role, false) + Em.I18n.t('installer.step10.taskStatus.failed') + _host.name;
              self.get('clusterInfo').findProperty('id', 1).get('status').findProperty('id', 2).get('statements').pushObject(Em.Object.create({
                status: s.st,
                color: 'text-info',
                displayStatement: statement
              }));
            });
          });
      }, this);
    }
  },

  /**
   * Push info about installed/failed master components to <code>clusterInfo</code>
   * @returns {bool}
   * @method loadMasterComponents
   */
  loadMasterComponents: function () {
    var components = this.get('content.masterComponentHosts');
    if (this.get('content.cluster.status') === 'INSTALL FAILED') {
      this.get('clusterInfo').pushObject(Em.Object.create({
        id: 2,
        displayStatement: Em.I18n.t('installer.step10.installStatus.failed'),
        color: 'text-danger',
        status: []
      }));
      return false;
    }
    else {
      this.get('clusterInfo').pushObject(Em.Object.create({
        id: 2,
        displayStatement: Em.I18n.t('installer.step10.installStatus.installed'),
        color: 'text-success',
        status: []
      }));
    }

    components.forEach(function (_component) {
      var component = Em.Object.create(_component);
      if (['NAMENODE', 'SECONDARY_NAMENODE', 'JOBTRACKER', 'HISTORYSERVER', 'RESOURCEMANAGER', 'HBASE_MASTER',
        'HIVE_SERVER', 'OOZIE_SERVER', 'GANGLIA_SERVER'].contains(component.component)) {
        this.loadMasterComponent(component);
      }
    }, this);
    return true;
  },

  /**
   * Push component info to <code>clusterInfo</code>
   * @param {Ember.Object} component
   * @method loadMasterComponent
   */
  loadMasterComponent: function (component) {
    if (component.get('hostName')) {
      var statement = Em.I18n.t('installer.step10.master.installedOn').format(component.get('display_name'), component.get('hostName'));
      this.get('clusterInfo').findProperty('id', 2).get('status').pushObject(Em.Object.create({
        id: 1,
        color: 'text-info',
        displayStatement: statement
      }));
    }
  },

  /**
   * Push info about installed/started/failed services to <code>clusterInfo</code>
   * @returns {bool}
   * @method loadStartedServices
   */
  loadStartedServices: function () {
    var status = this.get('content.cluster.status');
    if (status === 'STARTED') {
      this.get('clusterInfo').pushObject(Em.Object.create({
        id: 3,
        color: 'text-success',
        displayStatement: Em.I18n.t('installer.step10.startStatus.started'),
        status: []
      }));
      this.get('clusterInfo').pushObject(Em.Object.create({
        id: 4,
        color: 'text-success',
        displayStatement: Em.I18n.t('installer.step10.startStatus.passed'),
        status: []
      }));
      return true;
    } else if (status === 'START_SKIPPED') {
      this.get('clusterInfo').pushObject(Em.Object.create({
        id: 3,
        color: 'text-warning',
        displayStatement: Em.I18n.t('installer.step10.startStatus.skipped'),
        status: []
      }));
      return false
    } else {
      this.get('clusterInfo').pushObject(Em.Object.create({
        id: 3,
        color: 'text-danger',
        displayStatement: Em.I18n.t('installer.step10.startStatus.failed'),
        status: []
      }));
      return false;
    }
  },

  /**
   * Push install time to <code>clusterInfo</code>
   * @method loadInstallTime
   */
  loadInstallTime: function () {
    var statement, time;
    if (this.get('content.cluster.installTime')) {
      time = this.calculateInstallTime(this.get('content.cluster.installTime'));
      if (time.minutes !== 0) {
        statement = Em.I18n.t('installer.step10.installTime.minutes').format(time.minutes, time.seconds);
      } else {
        statement = Em.I18n.t('installer.step10.installTime.seconds').format(time.seconds);
      }
      this.get('clusterInfo').pushObject(Em.Object.create({
        id: 5,
        color: 'text-info',
        displayStatement: statement,
        status: []
      }));
    }
  },

  /**
   * Get used time for install process
   * @param {number} installTime
   * @returns {{minutes: *, seconds: *}}
   * @method calculateInstallTime
   */
  calculateInstallTime: function (installTime) {
    var secondsPerMinute = 60;
    var minutes = Math.floor(installTime);
    var seconds = Math.floor((installTime - minutes) * secondsPerMinute);
    return {
      minutes: minutes,
      seconds: seconds
    }
  }

});
