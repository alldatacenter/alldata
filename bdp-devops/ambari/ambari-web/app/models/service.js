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
require('utils/config');

App.Service = DS.Model.extend({
  serviceName: DS.attr('string', {defaultValue: ''}),
  displayName: function() {
    const displayName = App.format.role(this.get('serviceName'), true);
    if (this.get('hasMasterOrSlaveComponent') || displayName.endsWith('Client')) {
      return displayName;
    } else {
      return displayName + ' Client';
    }
  }.property('serviceName'),
  passiveState: DS.attr('string', {defaultValue: "OFF"}),
  workStatus: DS.attr('string'),
  rand: DS.attr('string'),
  toolTipContent: DS.attr('string'),
  quickLinks: DS.hasMany('App.QuickLinks'),  // mapped in app/mappers/service_metrics_mapper.js method - mapQuickLinks
  hostComponents: DS.hasMany('App.HostComponent'),
  serviceConfigsTemplate: App.config.get('preDefinedServiceConfigs'),
  desiredRepositoryVersionId: DS.attr('number'),
  /**
   * used by services("OOZIE", "ZOOKEEPER", "HIVE", "MAPREDUCE2", "TEZ", "SQOOP", "PIG","FALCON")
   * that have only client components
   */
  installedClients: DS.attr('number'),

  clientComponents: DS.hasMany('App.ClientComponent'),
  slaveComponents: DS.hasMany('App.SlaveComponent'),
  masterComponents: DS.hasMany('App.MasterComponent'),

  masterComponentGroups: DS.attr('array', {
    defaultValue: []
  }),

  hasMultipleMasterComponentGroups: Em.computed.gt('masterComponentGroups.length', 1),

  /**
   * Check master/slave component state of service
   * and general services state to define if it can be removed
   *
   * @type {boolean}
   */
  allowToDelete: function() {
    var workStatus = this.get('workStatus');
    return App.Service.allowUninstallStates.contains(workStatus)
      && this.get('slaveComponents').everyProperty('allowToDelete')
      && this.get('masterComponents').everyProperty('allowToDelete');
  }.property('slaveComponents.@each.allowToDelete', 'masterComponents.@each.allowToDelete', 'workStatus'),

  /**
   * @type {bool}
   */
  isInPassive: Em.computed.equal('passiveState', 'ON'),
  
  /**
   * @type {bool}
   */
  hasMasterOrSlaveComponent: function() {
    if (App.router.get('clusterController.isHostComponentMetricsLoaded')) {
      return this.get('slaveComponents').toArray()
      .concat(this.get('masterComponents').toArray())
      .mapProperty('totalCount')
      .reduce((a, b) => a + b, 0) > 0;
    } else {
      //Assume that service has master or/and slave components until data loaded
      return true;
    }
  }.property('slaveComponents.@each.totalCount', 'masterComponents.@each.totalCount'),

  serviceComponents: function() {
    var clientComponents = this.get('clientComponents').mapProperty('componentName');
    var slaveComponents = this.get('slaveComponents').mapProperty('componentName');
    var masterComponents = this.get('masterComponents').mapProperty('componentName');
    return clientComponents.concat(slaveComponents).concat(masterComponents);
  }.property('clientComponents.@each', 'slaveComponents.@each','masterComponents.@each'),

  healthStatus: Em.computed.getByKey('healthStatusMap', 'workStatus', 'yellow'),

  healthStatusMap: {
    STARTED: 'green',
    STARTING: 'green-blinking',
    INSTALLED: 'red',
    STOPPING: 'red-blinking',
    UNKNOWN: 'yellow'
  },

  isStopped: Em.computed.equal('workStatus', 'INSTALLED'),

  isStarted: Em.computed.equal('workStatus', 'STARTED'),

  /**
   * Indicates when service deleting is in progress
   * Used to stop update service's data and topology
   *
   * @type {boolean}
   * @default false
   */
  deleteInProgress: false,

  /**
   * Service Tagging by their type.
   * @type {String[]}
   **/
  serviceTypes: function() {
    var typeServiceMap = {
      GANGLIA: ['MONITORING'],
      HDFS: ['HA_MODE', 'FEDERATION'],
      YARN: ['HA_MODE'],
      RANGER: ['HA_MODE'],
      HAWQ: ['HA_MODE']
    };
    return typeServiceMap[this.get('serviceName')] || [];
  }.property('serviceName'),

  /**
   * For each host-component, if the desired_configs dont match the
   * actual_configs, then a restart is required.
   */
  isRestartRequired: function () {
    var serviceComponents = this.get('clientComponents').toArray()
      .concat(this.get('slaveComponents').toArray())
      .concat(this.get('masterComponents').toArray());
    var hc = {};

    serviceComponents.forEach(function(component) {
      var displayName = component.get('displayName');
      component.get('staleConfigHosts').forEach(function(hostName) {
        if (!hc[hostName]) {
          hc[hostName] = [];
        }
        hc[hostName].push(displayName);
      });
    });

    this.set('restartRequiredHostsAndComponents', hc);
    return (serviceComponents.filterProperty('staleConfigHosts.length').length > 0);
  }.property('serviceName'),
  
  /**
   * Contains a map of which hosts and host_components
   * need a restart. This is populated when calculating
   * #isRestartRequired()
   * Example:
   * {
   *  'publicHostName1': ['TaskTracker'],
   *  'publicHostName2': ['JobTracker', 'TaskTracker']
   * }
   */
  restartRequiredHostsAndComponents: {},
  
  /**
   * Based on the information in #restartRequiredHostsAndComponents
   */
  restartRequiredMessage: function () {
    var restartHC = this.get('restartRequiredHostsAndComponents');
    var hostCount = 0;
    var hcCount = 0;
    var hostsMsg = "<ul>";
    for(var host in restartHC){
      hostCount++;
      hostsMsg += "<li>"+host+"</li><ul>";
      restartHC[host].forEach(function(c){
        hcCount++;
        hostsMsg += "<li>"+c+"</li>";       
      });
      hostsMsg += "</ul>";
    }
    hostsMsg += "</ul>";
    return this.t('services.service.config.restartService.TooltipMessage').format(hcCount, hostCount, hostsMsg);
  }.property('restartRequiredHostsAndComponents'),

  /**
   * @type {number}
   */
  warningCount: 0,
  /**
   * @type {number}
   */
  criticalCount: 0,

  /**
   * Does service have Critical Alerts
   * @type {boolean}
   */
  hasCriticalAlerts: Em.computed.gte('criticalCount', 0),

  /**
   * Number of the Critical and Warning alerts for current service
   * @type {number}
   */
  alertsCount: Em.computed.sumProperties('warningCount', 'criticalCount')

});

/**
 * Map of all service states
 *
 * @type {Object}
 */
App.Service.statesMap = {
  init: 'INIT',
  installing: 'INSTALLING',
  install_failed: 'INSTALL_FAILED',
  stopped: 'INSTALLED',
  starting: 'STARTING',
  started: 'STARTED',
  stopping: 'STOPPING',
  uninstalling: 'UNINSTALLING',
  uninstalled: 'UNINSTALLED',
  wiping_out: 'WIPING_OUT',
  upgrading: 'UPGRADING',
  maintenance: 'MAINTENANCE',
  unknown: 'UNKNOWN'
};

/**
 * @type {String[]}
 */
App.Service.inProgressStates = [
  App.Service.statesMap.installing,
  App.Service.statesMap.starting,
  App.Service.statesMap.stopping,
  App.Service.statesMap.uninstalling,
  App.Service.statesMap.upgrading,
  App.Service.statesMap.wiping_out
];

/**
 * @type {String[]}
 */
App.Service.allowUninstallStates = [
  App.Service.statesMap.init,
  App.Service.statesMap.install_failed,
  App.Service.statesMap.stopped,
  App.Service.statesMap.unknown
];

App.Service.Health = {
  live: "LIVE",
  dead: "DEAD-RED",
  starting: "STARTING",
  stopping: "STOPPING",
  unknown: "DEAD-YELLOW",

  getKeyName: function (value) {
    switch (value) {
      case this.live:
        return 'live';
      case this.dead:
        return 'dead';
      case this.starting:
        return 'starting';
      case this.stopping:
        return 'stopping';
      case this.unknown:
        return 'unknown';
    }
    return 'none';
  }
};

/**
 * association between service and extended model name
 * @type {Object}
 */
App.Service.extendedModel = {
  'HDFS': 'HDFSService',
  'ONEFS' : 'ONEFSService',
  'HBASE': 'HBaseService',
  'YARN': 'YARNService',
  'MAPREDUCE2': 'MapReduce2Service',
  'STORM': 'StormService',
  'RANGER': 'RangerService',
  'FLUME': 'FlumeService'
};

App.Service.FIXTURES = [];
