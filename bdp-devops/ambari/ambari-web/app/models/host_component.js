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

App.HostComponent = DS.Model.extend({
  workStatus: DS.attr('string'),
  passiveState: DS.attr('string'),
  componentName: DS.attr('string'),
  displayName: DS.attr('string'),
  haStatus: DS.attr('string'),
  displayNameAdvanced: DS.attr('string'),
  staleConfigs: DS.attr('boolean'),
  host: DS.belongsTo('App.Host'),
  componentLogs: DS.belongsTo('App.HostComponentLog'),
  hostName: DS.attr('string'),
  publicHostName: DS.attr('string'),
  service: DS.belongsTo('App.Service'),
  adminState: DS.attr('string'),
  haNameSpace: DS.attr('string'),
  clusterIdValue: DS.attr('string'),

  serviceDisplayName: Em.computed.truncate('service.displayName', 14, 11),

  getDisplayName: Em.computed.truncate('displayName', 30, 25),

  getDisplayNameAdvanced:Em.computed.truncate('displayNameAdvanced', 30, 25),

  summaryLabelClassName:function(){
    return 'label_for_'+this.get('componentName').toLowerCase();
  }.property('componentName'),

  summaryValueClassName:function(){
    return 'value_for_'+this.get('componentName').toLowerCase();
  }.property('componentName'),
  /**
   * Determine if component is client
   * @returns {bool}
   */
  isClient: function () {
    return App.HostComponent.isClient(this.get('componentName'));
  }.property('componentName'),
  /**
   * Determine if component is running now
   * Based on <code>workStatus</code>
   * @returns {bool}
   */
  isRunning: Em.computed.existsIn('workStatus', ['STARTED', 'STARTING']),

  /**
   * Determines if component is not installed
   * Based on <code>workStatus</code>
   *
   * @type {boolean}
   */
  isNotInstalled: Em.computed.existsIn('workStatus', ['INIT', 'INSTALL_FAILED']),

  /**
   * Determine if component is master
   * @returns {bool}
   */
  isMaster: function () {
    return App.HostComponent.isMaster(this.get('componentName'));
  }.property('componentName', 'App.components.masters'),

  /**
   * Determine if component is slave
   * @returns {bool}
   */
  isSlave: function () {
    return App.HostComponent.isSlave(this.get('componentName'));
  }.property('componentName'),
  /**
   * Only certain components can be deleted.
   * They include some from master components,
   * some from slave components, and rest from
   * client components.
   * @returns {bool}
   */
  isDeletable: function () {
    return App.get('components.deletable').contains(this.get('componentName'));
  }.property('componentName'),
  /**
   * A host-component is decommissioning when it is in HDFS service's list of
   * decomNodes.
   * @returns {bool}
   */
  isDecommissioning: function () {
    var decommissioning = false;
    var hostName = this.get('hostName');
    var componentName = this.get('componentName');
    var hdfsSvc = App.HDFSService.find().objectAt(0);
    if (componentName === 'DATANODE' && hdfsSvc) {
      var decomNodes = hdfsSvc.get('decommissionDataNodes');
      var decomNode = decomNodes != null ? decomNodes.findProperty("hostName", hostName) : null;
      decommissioning = decomNode != null;
    }
    return decommissioning;
  }.property('componentName', 'hostName', 'App.router.clusterController.isLoaded', 'App.router.updateController.isUpdated'),
  /**
   * User friendly host component status
   * @returns {String}
   */
  isActive: function() {
    let passiveState = this.get('passiveState');
    if (passiveState === 'IMPLIED_FROM_HOST') {
      passiveState = this.get('host.passiveState');
    } else if (passiveState === 'IMPLIED_FROM_SERVICE') {
      passiveState = this.get('service.passiveState');
    } else if (passiveState === 'IMPLIED_FROM_SERVICE_AND_HOST') {
      return this.get('service.passiveState') === 'OFF' && this.get('host.passiveState') === 'OFF';
    }
    return passiveState === 'OFF';
  }.property('passiveState', 'host.passiveState', 'service.passiveState'),

  /**
   * Determine if passiveState is implied from host or/and service
   * @returns {Boolean}
   */
  isImpliedState: Em.computed.existsIn('passiveState', ['IMPLIED_FROM_SERVICE_AND_HOST', 'IMPLIED_FROM_HOST', 'IMPLIED_FROM_SERVICE']),

  passiveTooltip: Em.computed.ifThenElse('isActive', '', Em.I18n.t('hosts.component.passive.mode')),
  /**
   * Determine if component is a HDP component
   * @returns {bool}
   */
  isHDPComponent: function () {
    return !App.get('components.nonHDP').contains(this.get('componentName'));
  }.property('componentName', 'App.components.nonHDP'),

  /**
   * @type {number}
   */
  warningCount: 0,
  /**
   * @type {number}
   */
  criticalCount: 0,

  /**
   * Does component have Critical Alerts
   * @type {boolean}
   */
  hasCriticalAlerts: Em.computed.gte('criticalCount', 0),

  /**
   * Number of the Critical and Warning alerts for current component
   * @type {number}
   */
  alertsCount: Em.computed.sumProperties('warningCount', 'criticalCount'),

  statusClass: function () {
    return this.get('isActive') ? this.get('workStatus') : 'icon-medkit';
  }.property('workStatus', 'isActive'),

  statusIconClass: Em.computed.getByKey('statusIconClassMap', 'statusClass', ''),

  statusIconClassMap: {
    STARTED: App.healthIconClassGreen,
    STARTING: App.healthIconClassGreen,
    INSTALLED: App.healthIconClassRed,
    STOPPING: App.healthIconClassRed,
    UNKNOWN: App.healthIconClassYellow
  },

  componentTextStatus: function () {
    if (this.get('isClient') && this.get("workStatus") === 'INSTALLED') {
      return Em.I18n.t('common.installed');
    }
    return App.HostComponentStatus.getTextStatus(this.get("workStatus"));
  }.property('workStatus', 'isDecommissioning')
});

App.HostComponent.FIXTURES = [];

App.HostComponent.isClient = function(componentName) {
  return App.get('components.clients').contains(componentName);
};

App.HostComponent.isMaster = function(componentName) {
  return App.get('components.masters').contains(componentName);
};

App.HostComponent.isSlave = function(componentName) {
  return App.get('components.slaves').contains(componentName);
};

/**
 * get particular counter of host-component by name
 * @param {string} componentName
 * @param {string} type (installedCount|startedCount|totalCount)
 * @returns {number}
 */
App.HostComponent.getCount = function (componentName, type) {
  switch (App.StackServiceComponent.find(componentName).get('componentCategory')) {
    case 'MASTER':
      return Number(App.MasterComponent.find(componentName).get(type));
    case 'SLAVE':
      return Number(App.SlaveComponent.find(componentName).get(type));
    case 'CLIENT':
      return Number(App.ClientComponent.find(componentName).get(type));
    default:
      return 0;
  }
};

/**
 * @param {string} componentName
 * @param {string} hostName
 * @returns {string}
 */
App.HostComponent.getId = function(componentName, hostName) {
  return componentName + '_' + hostName;
};

App.HostComponentStatus = {
  started: "STARTED",
  starting: "STARTING",
  stopped: "INSTALLED",
  stopping: "STOPPING",
  install_failed: "INSTALL_FAILED",
  installing: "INSTALLING",
  upgrade_failed: "UPGRADE_FAILED",
  unknown: "UNKNOWN",
  disabled: "DISABLED",
  init: "INIT",

  /**
   * Get host component status in "machine" format
   * @param {String} value
   * @returns {String}
   */
  getKeyName: function (value) {
    switch (value) {
      case this.started:
        return 'started';
      case this.starting:
        return 'starting';
      case this.stopped:
        return 'installed';
      case this.stopping:
        return 'stopping';
      case this.install_failed:
        return 'install_failed';
      case this.installing:
        return 'installing';
      case this.upgrade_failed:
        return 'upgrade_failed';
      case this.disabled:
      case this.unknown:
        return 'unknown';
    }
    return 'unknown';
  },

  /**
   * Get user-friendly host component status
   * @param {String} value
   * @returns {String}
   */
  getTextStatus: function (value) {
    switch (value) {
      case this.installing:
        return 'Installing...';
      case this.install_failed:
        return 'Install Failed';
      case this.stopped:
        return 'Stopped';
      case this.started:
        return 'Started';
      case this.starting:
        return 'Starting...';
      case this.stopping:
        return 'Stopping...';
      case this.unknown:
        return 'Heartbeat Lost';
      case this.upgrade_failed:
        return 'Upgrade Failed';
      case this.disabled:
        return 'Disabled';
      case this.init:
        return 'Install Pending...';
    }
    return 'Unknown';
  },

  /**
   * Get list of possible <code>App.HostComponent</code> statuses
   * @returns {String[]}
   */
  getStatusesList: function () {
    var ret = [];
    for (var st in this) {
      if (this.hasOwnProperty(st) && Em.typeOf(this[st]) == 'string') {
        ret.push(this[st]);
      }
    }
    return ret;
  }
};

App.HostComponentActionMap = {
  getMap: function (ctx) {
    const NN = ctx.get('controller.content.hostComponents').findProperty('componentName', 'NAMENODE'),
      RM = ctx.get('controller.content.hostComponents').findProperty('componentName', 'RESOURCEMANAGER'),
      RA = ctx.get('controller.content.hostComponents').findProperty('componentName', 'RANGER_ADMIN'),
      HM = ctx.get('controller.content.hostComponents').findProperty('componentName', 'HAWQMASTER'),
      HS = ctx.get('controller.content.hostComponents').findProperty('componentName', 'HAWQSTANDBY'),
      HMComponent = App.MasterComponent.find('HAWQMASTER'),
      HSComponent = App.MasterComponent.find('HAWQSTANDBY'),
      hasMultipleMasterComponentGroups = ctx.get('service.hasMultipleMasterComponentGroups'),
      isClientsOnlyService = ctx.get('controller.isClientsOnlyService'),
      isStartDisabled = ctx.get('controller.isStartDisabled'),
      isStopDisabled = ctx.get('controller.isStopDisabled');

    return {
      START_ALL: {
        action: hasMultipleMasterComponentGroups ? '' : 'startService',
        label: Em.I18n.t('services.service.start'),
        cssClass: `glyphicon glyphicon-play ${isStartDisabled ? 'disabled' : 'enabled'}`,
        isHidden: isClientsOnlyService,
        disabled: isStartDisabled,
        hasSubmenu: !isStartDisabled && hasMultipleMasterComponentGroups,
        submenuOptions: !isStartDisabled && hasMultipleMasterComponentGroups ? this.getMastersSubmenu(ctx, 'startCertainHostComponents') : []
      },
      STOP_ALL: {
        action: hasMultipleMasterComponentGroups ? '' : 'stopService',
        label: Em.I18n.t('services.service.stop'),
        cssClass: `glyphicon glyphicon-stop ${isStopDisabled ? 'disabled' : 'enabled'}`,
        isHidden: isClientsOnlyService,
        disabled: isStopDisabled,
        hasSubmenu: !isStopDisabled && hasMultipleMasterComponentGroups,
        submenuOptions: !isStopDisabled && hasMultipleMasterComponentGroups ? this.getMastersSubmenu(ctx, 'stopCertainHostComponents') : []
      },
      RESTART_ALL: {
        action: hasMultipleMasterComponentGroups ? '' : 'restartAllHostComponents',
        context: ctx.get('serviceName'),
        label: hasMultipleMasterComponentGroups ? Em.I18n.t('common.restart') : Em.I18n.t('restart.service.all'),
        cssClass: 'glyphicon glyphicon-time',
        disabled: false,
        hasSubmenu: hasMultipleMasterComponentGroups,
        submenuOptions: hasMultipleMasterComponentGroups ? this.getMastersSubmenu(ctx, 'restartCertainHostComponents') : []
      },
      //Ongoing feature. Will later replace RESTART_ALL
      RESTART_SERVICE: {
        action: 'restartServiceAllComponents',
        context: ctx.get('serviceName'),
        label: Em.I18n.t('restart.service.rest.context').format(ctx.get('displayName')),
        cssClass: 'glyphicon glyphicon-time',
        disabled: false,
        hasSubmenu: true,
        submenuOptions: ctx.get('controller.restartOptions')
      },
      RESTART_NAMENODES: {
        action: '',
        label: Em.I18n.t('rollingrestart.dialog.title').format(pluralize(App.format.role('NAMENODE', false))),
        cssClass: 'glyphicon glyphicon-time',
        isHidden: !hasMultipleMasterComponentGroups,
        disabled: false,
        hasSubmenu: true,
        submenuOptions: this.getMastersSubmenu(ctx, 'restartCertainHostComponents', ['NAMENODE'])
      },
      RUN_SMOKE_TEST: {
        action: 'runSmokeTest',
        label: Em.I18n.t('services.service.actions.run.smoke'),
        cssClass: 'glyphicon glyphicon-thumbs-up',
        disabled: ctx.get('controller.isSmokeTestDisabled')
      },
      REFRESH_CONFIGS: {
        action: 'refreshConfigs',
        label: Em.I18n.t('hosts.host.details.refreshConfigs'),
        cssClass: 'glyphicon glyphicon-refresh',
        disabled: false
      },
      REGENERATE_KEYTAB_FILE_OPERATIONS: {
        action: 'regenerateKeytabFileOperations',
        label: Em.I18n.t('admin.kerberos.button.regenerateKeytabs'),
        cssClass: 'glyphicon glyphicon-repeat',
        isHidden: !App.get('isKerberosEnabled')
      },
      REFRESHQUEUES: {
        action: 'refreshYarnQueues',
        customCommand: 'REFRESHQUEUES',
        context : Em.I18n.t('services.service.actions.run.yarnRefreshQueues.context'),
        label: Em.I18n.t('services.service.actions.run.yarnRefreshQueues.menu'),
        cssClass: 'glyphicon glyphicon-refresh',
        disabled: false
      },
      ROLLING_RESTART: {
        action: 'rollingRestart',
        context: ctx.get('rollingRestartComponent'),
        label: Em.I18n.t('rollingrestart.dialog.title'),
        cssClass: 'glyphicon glyphicon-time',
        disabled: false
      },
      TOGGLE_PASSIVE: {
        action: 'turnOnOffPassive',
        context: ctx.get('isPassive') ? Em.I18n.t('passiveState.turnOffFor').format(ctx.get('displayName')) : Em.I18n.t('passiveState.turnOnFor').format(ctx.get('displayName')),
        label: ctx.get('isPassive') ? Em.I18n.t('passiveState.turnOff') : Em.I18n.t('passiveState.turnOn'),
        cssClass: 'icon-medkit',
        disabled: false
      },
      MANAGE_JN: {
        action: 'manageJournalNode',
        label: Em.I18n.t('admin.manageJournalNode.label'),
        cssClass: 'icon-cog',
        isHidden: !App.get('supports.manageJournalNode') || !App.get('isHaEnabled')
        || (App.router.get('mainHostController.totalCount') == 3 && App.HostComponent.find().filterProperty('componentName', 'JOURNALNODE').get('length') == 3)
      },
      TOGGLE_NN_HA: {
        action: App.get('isHaEnabled') ? 'disableHighAvailability' : 'enableHighAvailability',
        label: App.get('isHaEnabled') ? Em.I18n.t('admin.highAvailability.button.disable') : Em.I18n.t('admin.highAvailability.button.enable'),
        cssClass: App.get('isHaEnabled') ? 'glyphicon glyphicon-arrow-down' : 'glyphicon glyphicon-arrow-up',
        isHidden: App.get('isHaEnabled'),
        disabled: App.get('isSingleNode') || !NN || NN.get('isNotInstalled')
      },
      TOGGLE_RM_HA: {
        action: 'enableRMHighAvailability',
        label: App.get('isRMHaEnabled') ? Em.I18n.t('admin.rm_highAvailability.button.disable') : Em.I18n.t('admin.rm_highAvailability.button.enable'),
        cssClass: App.get('isRMHaEnabled') ? 'glyphicon glyphicon-arrow-down' : 'glyphicon glyphicon-arrow-up',
        isHidden: App.get('isRMHaEnabled'),
        disabled: App.get('isSingleNode') || !RM || RM.get('isNotInstalled')
      },
      TOGGLE_RA_HA: {
        action: 'enableRAHighAvailability',
        label: Em.I18n.t('admin.ra_highAvailability.button.enable'),
        cssClass: 'glyphicon glyphicon-arrow-up',
        isHidden: App.get('isRAHaEnabled'),
        disabled: App.get('isSingleNode') || !RA || RA.get('isNotInstalled')
      },
      MOVE_COMPONENT: {
        action: 'reassignMaster',
        context: '',
        isHidden: !App.isAuthorized('SERVICE.MOVE'),
        label: Em.I18n.t('services.service.actions.reassign.master'),
        cssClass: 'glyphicon glyphicon-share-alt'
      },
      STARTDEMOLDAP: {
        action: 'startLdapKnox',
        customCommand: 'STARTDEMOLDAP',
        context: Em.I18n.t('services.service.actions.run.startLdapKnox.context'),
        label: Em.I18n.t('services.service.actions.run.startLdapKnox.context'),
        cssClass: 'icon icon-play-sign',
        disabled: false
      },
      STOPDEMOLDAP: {
        action: 'stopLdapKnox',
        customCommand: 'STOPDEMOLDAP',
        context: Em.I18n.t('services.service.actions.run.stopLdapKnox.context'),
        label: Em.I18n.t('services.service.actions.run.stopLdapKnox.context'),
        cssClass: 'glyphicon glyphicon-stop',
        disabled: false
      },
      RESTART_LLAP: {
        action: 'restartLLAP',
        customCommand: 'RESTART_LLAP',
        context: Em.I18n.t('services.service.actions.run.restartLLAP'),
        label: Em.I18n.t('services.service.actions.run.restartLLAP') + ' ∞',
        cssClass: 'glyphicon glyphicon-refresh'
      },
      REBALANCEHDFS: {
        action: 'rebalanceHdfsNodes',
        customCommand: 'REBALANCEHDFS',
        context: Em.I18n.t('services.service.actions.run.rebalanceHdfsNodes.context'),
        label: Em.I18n.t('services.service.actions.run.rebalanceHdfsNodes'),
        cssClass: 'glyphicon glyphicon-refresh',
        disabled: false
      },
      DOWNLOAD_CLIENT_CONFIGS: {
        action: ctx.get('controller.isSeveralClients') ? '' : 'downloadClientConfigs',
        label: Em.I18n.t('services.service.actions.downloadClientConfigs'),
        cssClass: 'glyphicon glyphicon-download-alt',
        isHidden: !!ctx.get('controller.content.clientComponents') ? ctx.get('controller.content.clientComponents').rejectProperty('totalCount', 0).length == 0 : false,
        disabled: false,
        hasSubmenu: ctx.get('controller.isSeveralClients'),
        submenuOptions: ctx.get('controller.clientComponents')
      },
      DELETE_SERVICE: {
        action: 'deleteService',
        context: ctx.get('serviceName'),
        label: Em.I18n.t('services.service.actions.deleteService'),
        cssClass: 'glyphicon glyphicon-remove',
        isHidden: !App.get('services.supportsDeleteViaUI').contains(ctx.get('serviceName')) //hide the menu item when the service has a custom behavior setting in its metainfo.xml to disallow Delete Services via UI
      },
      IMMEDIATE_STOP_HAWQ_SERVICE: {
        action: 'executeHawqCustomCommand',
        customCommand: 'IMMEDIATE_STOP_HAWQ_SERVICE',
        context: Em.I18n.t('services.service.actions.run.immediateStopHawqService.context'),
        label: Em.I18n.t('services.service.actions.run.immediateStopHawqService.label'),
        cssClass: 'glyphicon glyphicon-stop',
        disabled: !HM || HM.get('workStatus') != App.HostComponentStatus.started
      },
      IMMEDIATE_STOP_HAWQ_SEGMENT: {
        customCommand: 'IMMEDIATE_STOP_HAWQ_SEGMENT',
        context: Em.I18n.t('services.service.actions.run.immediateStopHawqSegment.context'),
        label: Em.I18n.t('services.service.actions.run.immediateStopHawqSegment.label'),
        cssClass: 'glyphicon glyphicon-stop'
      },
      RESYNC_HAWQ_STANDBY: {
        action: 'executeHawqCustomCommand',
        customCommand: 'RESYNC_HAWQ_STANDBY',
        context: Em.I18n.t('services.service.actions.run.resyncHawqStandby.context'),
        label: Em.I18n.t('services.service.actions.run.resyncHawqStandby.label'),
        cssClass: 'glyphicon glyphicon-refresh',
        isHidden : App.get('isSingleNode') || !HS ,
        disabled: !((!!HMComponent && HMComponent.get('startedCount') === 1) && (!!HSComponent && HSComponent.get('startedCount') === 1))
      },
      TOGGLE_ADD_HAWQ_STANDBY: {
        action: 'addHawqStandby',
        label: Em.I18n.t('admin.addHawqStandby.button.enable'),
        cssClass: 'glyphicon glyphicon-plus',
        isHidden: App.get('isSingleNode') || HS,
        disabled: false
      },
      REMOVE_HAWQ_STANDBY: {
        action: 'removeHawqStandby',
        context: Em.I18n.t('admin.removeHawqStandby.button.enable'),
        label: Em.I18n.t('admin.removeHawqStandby.button.enable'),
        cssClass: 'glyphicon glyphicon-minus',
        isHidden: App.get('isSingleNode') || !HS,
        disabled: !HM || HM.get('workStatus') != App.HostComponentStatus.started,
        hideFromComponentView: true
      },
      ACTIVATE_HAWQ_STANDBY: {
        action: 'activateHawqStandby',
        label: Em.I18n.t('admin.activateHawqStandby.button.enable'),
        context: Em.I18n.t('admin.activateHawqStandby.button.enable'),
        cssClass: 'glyphicon glyphicon-arrow-up',
        isHidden: App.get('isSingleNode') || !HS,
        disabled: false,
        hideFromComponentView: true
      },
      HAWQ_CLEAR_CACHE: {
        action: 'executeHawqCustomCommand',
        customCommand: 'HAWQ_CLEAR_CACHE',
        context: Em.I18n.t('services.service.actions.run.clearHawqCache.label'),
        label: Em.I18n.t('services.service.actions.run.clearHawqCache.label'),
        cssClass: 'glyphicon glyphicon-refresh',
        isHidden : false,
        disabled: !HM || HM.get('workStatus') != App.HostComponentStatus.started
      },
      RUN_HAWQ_CHECK: {
        action: 'executeHawqCustomCommand',
        customCommand: 'RUN_HAWQ_CHECK',
        context: Em.I18n.t('services.service.actions.run.runHawqCheck.label'),
        label: Em.I18n.t('services.service.actions.run.runHawqCheck.label'),
        cssClass: 'glyphicon glyphicon-thumbs-up',
        isHidden : false,
        disabled: false
      },
      MASTER_CUSTOM_COMMAND: {
        action: 'executeCustomCommand',
        cssClass: 'glyphicon glyphicon-play-circle',
        isHidden: false,
        disabled: false
      },
      TOGGLE_NN_FEDERATION: {
        action: 'openNameNodeFederationWizard',
        label: Em.I18n.t('admin.nameNodeFederation.button.enable'),
        cssClass: 'icon icon-sitemap',
        disabled: !App.get('isHaEnabled')
      },
      UPDATE_REPLICATION: {
        action: 'updateHBaseReplication',
        customCommand: 'UPDATE_REPLICATION',
        context: Em.I18n.t('services.service.actions.run.updateHBaseReplication.context'),
        label: Em.I18n.t('services.service.actions.run.updateHBaseReplication.label'),
        cssClass: 'glyphicon glyphicon-refresh',
        disabled: false
      },
      STOP_REPLICATION: {
        action: 'stopHBaseReplication',
        customCommand: 'STOP_REPLICATION',
        context: Em.I18n.t('services.service.actions.run.stopHBaseReplication.context'),
        label: Em.I18n.t('services.service.actions.run.stopHBaseReplication.label'),
        cssClass: 'glyphicon glyphicon-refresh',
        disabled: false
      }
    };
  },

  getMastersSubmenu: function (context, action, components) {
    const serviceName = context.get('service.serviceName'),
      groups = context.get('service.masterComponentGroups') || [],
      allItem = {
        action,
        context: Object.assign({
          label: Em.I18n.t('services.service.allComponents')
        }, components ? {
          components,
          hosts: groups.mapProperty('hosts').reduce((acc, groupHosts) => [...acc, ...groupHosts], []).uniq()
        } : {
          serviceName
        }),
        disabled: false
      },
      groupItems = groups.map(group => {
        return {
          action,
          context: {
            label: group.title,
            hosts: group.hosts,
            components: components || group.components,
            serviceName
          },
          disabled: false,
          tooltip: group.title
        };
      });
    return [allItem, ...groupItems];
  }
};
