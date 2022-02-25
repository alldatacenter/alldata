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

App.version = ''; // filled out by set-ambari-version.sh script
App.testMode = (location.port == '3333'); // test mode is automatically enabled if running on brunch server
App.testModeDelayForActions = 10000;
App.skipBootstrap = false;
App.alwaysGoToInstaller = false;
App.testEnableSecurity = true; // By default enable security is tested; turning it false tests disable security
App.testNameNodeHA = true;
App.appURLRoot = '{proxy_root}/'.replace(/\{.+\}/g, ''); // determines application root path name, not related to hash route
App.apiPrefix = '/api/v1';
App.defaultStackVersion = 'HDP-2.3';
App.defaultWindowsStackVersion = 'HDPWIN-2.1';

App.defaultJavaHome = '/usr/jdk/jdk1.6.0_31';
App.timeout = 180000; // default AJAX timeout
App.maxRetries = 3; // max number of retries for certain AJAX calls
App.sessionKeepAliveInterval  = 60000;
App.bgOperationsUpdateInterval = 6000;
App.componentsUpdateInterval = 6000;
App.contentUpdateInterval = 15000;
App.hostStatusCountersUpdateInterval = 10000;
App.alertDefinitionsUpdateInterval = 10000;
App.alertInstancesUpdateInterval = 10000;
App.alertGroupsUpdateInterval = 10000;
App.clusterEnvUpdateInterval = 10000;
App.pageReloadTime = 3600000;
App.nnCheckpointAgeAlertThreshold = 12; // in hours
App.minDiskSpace = 2.0; // minimum disk space required for '/' for each host before install, unit GB
App.minDiskSpaceUsrLib = 1.0; // minimum disk space for '/usr/lib' for each host before install, unit GB
App.healthIconClassGreen = 'glyphicon glyphicon-ok-sign'; // bootstrap icon class for healthy/started service/host/host-component
App.healthIconClassRed = 'glyphicon glyphicon-warning-sign'; // bootstrap icon class for master down/stopped service/host/host-component
App.healthIconClassOrange = 'glyphicon glyphicon-minus-sign'; // bootstrap icon class for slave down/decommissioned host/host-component
App.healthIconClassYellow = 'glyphicon glyphicon-question-sign'; // bootstrap icon class for heartbeat lost service/host/host-component
App.isManagedMySQLForHiveEnabled = false;
App.isStormMetricsSupported = true;
App.healthStatusRed = '#EF6162';
App.healthStatusGreen = '#1EB475';
App.healthStatusOrange = '#E98A40';
App.widgetContentColor = '#666666';
App.gaugeWidgetRemainingAreaColor = '#DDDDDD';
App.dataVisualizationColorScheme = ['#41bfae', '#79e3d1', '#63c2e5', '#c4aeff', '#b991d9', '#ffb9bf', '#ffae65', '#f6d151', '#a7cf82', '#abdfd5', '#3aac9c', '#6dccbc', '#59aece', '#b09ce5', '#a682c3', '#e5a6ac', '#e59c5b', '#ddbc49', '#96ba75', '#9ac8bf', '#83d5ca', '#a8ede1', '#99d7ee', '#d9caff', '#d1b7e6', '#ffd1d5', '#ffca9b', '#f9e18e', '#c6e0ae', '#c8eae4'];
App.inactivityRemainTime = 60; // in seconds
App.enableLogger = true;
App.stackVersionsAvailable = true;
App.upgradeHistoryAvailable = false;
App.enableDigitalClock = false;

// experimental features are automatically enabled if running on brunch server
App.enableExperimental = false;

App.supports = {
  preUpgradeCheck: true,
  displayOlderVersions: false,
  autoRollbackHA: false,
  alwaysEnableManagedMySQLForHive: false,
  preKerberizeCheck: false,
  customizeAgentUserAccount: false,
  installGanglia: false,
  opsDuringRollingUpgrade: false,
  customizedWidgetLayout: false,
  showPageLoadTime: false,
  skipComponentStartAfterInstall: false,
  preInstallChecks: false,
  serviceAutoStart: true,
  logSearch: true,
  redhatSatellite: false,
  addingNewRepository: false,
  kerberosStackAdvisor: true,
  logCountVizualization: false,
  createAlerts: false,
  enabledWizardForHostOrderedUpgrade: true,
  manageJournalNode: true,
  enableToggleKerberos: true,
  enableAddDeleteServices: true,
  regenerateKeytabsOnSingleHost: false,
  enableNewServiceRestartOptions: false
};

if (App.enableExperimental) {
  for (var support in App.supports) {
    App.supports[support] = true;
  }
}

// this is to make sure that IE does not cache data when making AJAX calls to the server
if (!$.mocho) {
  $.ajaxSetup({
    cache: false,
    headers: {"X-Requested-By": "X-Requested-By"}
  });
}

/**
 * Test Mode values
 */
App.test_hostname = 'hostname';
