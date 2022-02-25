/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

var App = require('app');

App.MainServiceInfoSummaryController = Em.Controller.extend({
  name: 'mainServiceInfoSummaryController',

  selectedFlumeAgent: null,

  /**
   * Indicates whether Ranger plugins status update polling is active
   * @type {boolean}
   */
  isRangerUpdateWorking: false,

  /**
   * Indicates whether array with initial Ranger plugins data is set
   * @type {boolean}
   */
  isRangerPluginsArraySet: false,

  /**
   * Indicates whether previous AJAX request for Ranger plugins config properties has failed
   * @type {boolean}
   */
  isPreviousRangerConfigsCallFailed: false,

  /**
   * HiveServer2 JDBC connection endpoint data
   * @type {array}
   */
  hiveServerEndPoints: [],

  /**
   * Ranger plugins data
   * @type {array}
   */
  rangerPlugins: [
    {
      serviceName: 'HDFS',
      type: 'ranger-hdfs-plugin-properties',
      propertyName: 'ranger-hdfs-plugin-enabled',
      valueForEnable: 'Yes'
    },
    {
      serviceName: 'YARN',
      type: 'ranger-yarn-plugin-properties',
      propertyName: 'ranger-yarn-plugin-enabled',
      valueForEnable: 'Yes'
    },
    {
      serviceName: 'HBASE',
      type: 'ranger-hbase-plugin-properties',
      propertyName: 'ranger-hbase-plugin-enabled',
      valueForEnable: 'Yes'
    },
    {
      serviceName: 'HIVE',
      type: 'hive-env',
      propertyName: 'hive_security_authorization',
      valueForEnable: 'Ranger'
    },
    {
      serviceName: 'KNOX',
      type: 'ranger-knox-plugin-properties',
      propertyName: 'ranger-knox-plugin-enabled',
      valueForEnable: 'Yes'
    },
    {
      serviceName: 'STORM',
      type: 'ranger-storm-plugin-properties',
      propertyName: 'ranger-storm-plugin-enabled',
      valueForEnable: 'Yes'
    },
    {
      serviceName: 'KAFKA',
      type: 'ranger-kafka-plugin-properties',
      propertyName: 'ranger-kafka-plugin-enabled',
      valueForEnable: 'Yes'
    },
    {
      serviceName: 'ATLAS',
      type: 'ranger-atlas-plugin-properties',
      propertyName: 'ranger-atlas-plugin-enabled',
      valueForEnable: 'Yes'
    },
    {
      serviceName: 'NIFI',
      type: 'ranger-nifi-plugin-properties',
      propertyName: 'ranger-nifi-plugin-enabled',
      valueForEnable: 'Yes'
    }
  ],

  /**
   * Set initial Ranger plugins data
   * @method setRangerPlugins
   */
  setRangerPlugins: function () {
    if (App.get('router.clusterController.isLoaded') && !this.get('isRangerPluginsArraySet')) {
      // Display order of ranger plugin for services should be decided from  App.StackService.displayOrder to keep consistency
      // with display order of services at other places in the application like `select service's page` and `service menu bar`
      var displayOrderLength = App.StackService.displayOrder.length;
      var rangerPlugins = this.get('rangerPlugins').map(function (item, index) {
        var displayOrderIndex = App.StackService.displayOrder.indexOf(item.serviceName);
        return $.extend(item, {
          index: displayOrderIndex == -1 ? displayOrderLength + index : displayOrderIndex
        });
      }).sortProperty('index');

      this.setProperties({
        rangerPlugins: rangerPlugins.map(function (item) {
          var stackService = App.StackService.find().findProperty('serviceName', item.serviceName);
          var displayName = (stackService) ? stackService.get('displayName') : item.serviceName;
          return $.extend(item, {
            pluginTitle: Em.I18n.t('services.service.summary.ranger.plugin.title').format(displayName),
            isDisplayed: App.Service.find().someProperty('serviceName', item.serviceName) &&
              stackService.get('configTypes').hasOwnProperty(item.type),
            status: Em.I18n.t('services.service.summary.ranger.plugin.loadingStatus')
          });
        }),
        isRangerPluginsArraySet: true
      });
    }
  }.observes('App.router.clusterController.isLoaded'),

  /**
   * Get latest config tags
   * @method updateRangerPluginsStatus
   * @param callback
   */
  updateRangerPluginsStatus: function (callback) {
    App.ajax.send({
      name: 'config.tags',
      sender: this,
      success: 'getRangerPluginsStatus',
      callback: callback
    });
  },

  /**
   * Get latest Ranger plugins config properties
   * @method getRangerPluginsStatus
   * @param data
   */
  getRangerPluginsStatus: function (data) {
    var urlParams = [];
    this.get('rangerPlugins').forEach(function (item) {
      if (App.Service.find().someProperty('serviceName', item.serviceName) && data.Clusters.desired_configs.hasOwnProperty(item.type)) {
        var currentTag = data.Clusters.desired_configs[item.type].tag;
        var isTagChanged = item.tag != currentTag;
        Em.set(item, 'isDisplayed', true);
        //Request for properties should be sent either if configs have changed or if previous Ranger plugins config properties has failed
        if (isTagChanged || this.get('isPreviousRangerConfigsCallFailed')) {
          Em.set(item, 'tag', currentTag);
          urlParams.push('(type=' + item.type + '&tag=' + currentTag + ')');
        }
      } else {
        Em.set(item, 'isDisplayed', false);
      }
    }, this);
    if (urlParams.length) {
      App.ajax.send({
        name: 'reassign.load_configs',
        sender: this,
        data: {
          urlParams: urlParams.join('|')
        },
        success: 'getRangerPluginsStatusSuccess',
        error: 'getRangerPluginsStatusError'
      });
    }
  },

  /**
   * Set Ranger plugins statuses
   * @method getRangerPluginsStatusSuccess
   * @param data
   */
  getRangerPluginsStatusSuccess: function (data) {
    this.set('isPreviousRangerConfigsCallFailed', false);
    data.items.forEach(function (item) {
      var serviceName = this.get('rangerPlugins').findProperty('type', item.type).serviceName;
      var propertyName = this.get('rangerPlugins').findProperty('type', item.type).propertyName;
      var propertyValue = this.get('rangerPlugins').findProperty('type', item.type).valueForEnable;
      var statusString;

      if (item.properties[propertyName]) {
        statusString = item.properties[propertyName] == propertyValue ? 'alerts.table.state.enabled' : 'alerts.table.state.disabled';
      }
      else {
        statusString = 'common.unknown';
      }
      Em.set(this.get('rangerPlugins').findProperty('serviceName', serviceName), 'status', Em.I18n.t(statusString));
    }, this);
  },

  /**
   * Method executed if Ranger plugins config properties request has failed
   * @method getRangerPluginsStatusError
   */
  getRangerPluginsStatusError: function () {
    this.set('isPreviousRangerConfigsCallFailed', true);
  },

  /**
   * This method is invoked when hive view is rendered to fetch and display
   * information for JDBC connect string for HiveServer2 instances
   * @method  setHiveEndPointsValue
   * @public
   */
  setHiveEndPointsValue: function() {
    var self = this;
    const sites = ['hive-site', 'hive-interactive-site'];

    var siteToComponentMap = {
      'hive-site': 'HIVE_SERVER',
      'hive-interactive-site': 'HIVE_SERVER_INTERACTIVE'
    };

    App.router.get('configurationController').getCurrentConfigsBySites(sites).done(function (configs) {

      var hiveSiteIndex =  configs.map(function(item){
        return item.type;
      }).indexOf('hive-site');

      // if hive-site is not first item then rotate the array to make it first
      if (!!hiveSiteIndex) {
        configs.push(configs.shift());
      }

      var hiveSiteDynamicDiscovery = configs[0].properties['hive.server2.support.dynamic.service.discovery'];
      var hiveSiteZkQuorom = configs[0].properties['hive.zookeeper.quorum'];
      var hiveSiteServiceDiscorveryMode = 'zooKeeper';
      var hiveSiteZkNameSpace = configs[0].properties['hive.server2.zookeeper.namespace'];

      configs.forEach(function(_config) {
        var masterComponent = App.MasterComponent.find().findProperty('componentName', siteToComponentMap[_config.type]);
        if (_config.type === 'hive-interactive-site') {
          hiveSiteDynamicDiscovery = _config.properties['hive.server2.support.dynamic.service.discovery'] || hiveSiteDynamicDiscovery;
          hiveSiteZkQuorom = _config.properties['hive.zookeeper.quorum'] || hiveSiteZkQuorom;
          hiveSiteZkNameSpace = _config.properties['hive.server2.zookeeper.namespace'] || hiveSiteZkNameSpace;
          if (App.HostComponent.find().filterProperty('componentName','HIVE_SERVER_INTERACTIVE').length > 1) {
            hiveSiteServiceDiscorveryMode = 'zooKeeperHA';
            hiveSiteZkNameSpace = _config.properties['hive.server2.active.passive.ha.registry.namespace'];
          }
        }
        if (masterComponent && !!masterComponent.get('totalCount')) {
          var hiveEndPoint = {
            isVisible: hiveSiteDynamicDiscovery,
            componentName: masterComponent.get('componentName'),
            label: masterComponent.get('displayName') + Em.I18n.t('services.service.summary.hiveserver2.jdbc.url.text'),
            value: Em.I18n.t('services.service.summary.hiveserver2.endpoint.value').format(hiveSiteZkQuorom, hiveSiteServiceDiscorveryMode, hiveSiteZkNameSpace),
            tooltipText: Em.I18n.t('services.service.summary.hiveserver2.endpoint.tooltip.text').format(masterComponent.get('displayName'))
          };
          self.get('hiveServerEndPoints').pushObject(Em.Object.create(hiveEndPoint));
        }
      });
    });
  },

  /**
   * Send start command for selected Flume Agent
   * @method startFlumeAgent
   */
  startFlumeAgent: function () {
    var selectedFlumeAgent = arguments[0].context;
    if (selectedFlumeAgent && selectedFlumeAgent.get('status') === 'NOT_RUNNING') {
      var self = this;
      App.showConfirmationPopup(function () {
        var state = 'STARTED';
        var context = Em.I18n.t('services.service.summary.flume.start.context').format(selectedFlumeAgent.get('name'));
        self.sendFlumeAgentCommandToServer(state, context, selectedFlumeAgent);
      });
    }
  },

  /**
   * Send stop command for selected Flume Agent
   * @method stopFlumeAgent
   */
  stopFlumeAgent: function () {
    var selectedFlumeAgent = arguments[0].context;
    if (selectedFlumeAgent && selectedFlumeAgent.get('status') === 'RUNNING') {
      var self = this;
      App.showConfirmationPopup(function () {
        var state = 'INSTALLED';
        var context = Em.I18n.t('services.service.summary.flume.stop.context').format(selectedFlumeAgent.get('name'));
        self.sendFlumeAgentCommandToServer(state, context, selectedFlumeAgent);
      });
    }
  },

  /**
   * Send command for Flume Agent to server
   * @param {string} state
   * @param {string} context
   * @param {Object} agent
   * @method sendFlumeAgentCommandToServer
   */
  sendFlumeAgentCommandToServer: function (state, context, agent) {
    App.ajax.send({
      name: 'service.flume.agent.command',
      sender: this,
      data: {
        state: state,
        context: context,
        agentName: agent.get('name'),
        host: agent.get('hostName')
      },
      success: 'commandSuccessCallback'
    });
  },

  /**
   * Callback, that shows Background operations popup if request was successful
   */
  commandSuccessCallback: function () {
    // load data (if we need to show this background operations popup) from persist
    App.router.get('userSettingsController').dataLoading('show_bg').done(function (showPopup) {
      if (showPopup) {
        App.router.get('backgroundOperationsController').showPopup();
      }
    });
  },

  gotoConfigs: function () {
    App.router.get('mainServiceItemController').set('routeToConfigs', true);
    App.router.transitionTo('main.services.service.configs', this.get('content'));
    App.router.get('mainServiceItemController').set('routeToConfigs', false);
  },

  showServiceAlertsPopup: function (event) {
    var context = event.context;
    return App.ModalPopup.show({
      header: Em.I18n.t('services.service.summary.alerts.popup.header').format(context.get('displayName')),
      autoHeight: false,
      classNames: ['sixty-percent-width-modal', 'service-alerts-popup'],
      bodyClass: Em.View.extend({
        templateName: require('templates/main/service/info/service_alert_popup'),
        classNames: ['service-alerts'],
        controllerBinding: 'App.router.mainAlertDefinitionsController',
        didInsertElement: function () {
          Em.run.next(this, function () {
            App.tooltip(this.$(".timeago"));
            App.tooltip(this.$(".definition-latest-text"), {placement: 'bottom'});
          });
        },
        willDestroyElement:function () {
          this.$(".timeago").tooltip('destroy');
          this.$(".definition-latest-text").tooltip('destroy');
        },
        alerts: function () {
          var property = context.get('componentName') ? 'componentName' : 'serviceName';
          var serviceDefinitions = this.get('controller.content').filterProperty(property, context.get(property));
          // definitions should be sorted in order: critical, warning, ok, unknown, other
          var criticalDefinitions = [], warningDefinitions = [], okDefinitions = [], unknownDefinitions = [];
          serviceDefinitions.forEach(function (definition) {
            if (definition.get('isCritical')) {
              criticalDefinitions.push(definition);
              serviceDefinitions = serviceDefinitions.without(definition);
            } else if (definition.get('isWarning')) {
              warningDefinitions.push(definition);
              serviceDefinitions = serviceDefinitions.without(definition);
            } else if (definition.get('isOK')) {
              okDefinitions.push(definition);
              serviceDefinitions = serviceDefinitions.without(definition);
            } else if (definition.get('isUnknown')) {
              unknownDefinitions.push(definition);
              serviceDefinitions = serviceDefinitions.without(definition);
            }
          });
          serviceDefinitions = criticalDefinitions.concat(warningDefinitions, okDefinitions, unknownDefinitions, serviceDefinitions);
          return serviceDefinitions;
        }.property('controller.content'),
        gotoAlertDetails: function (e) {
          if (e && e.context) {
            this.get('parentView').hide();
            App.router.transitionTo('main.alerts.alertDetails', e.context);
          }
          return false;
        },
        closePopup: function () {
          this.get('parentView').hide();
        }
      }),
      isHideBodyScroll: false,
      primary: Em.I18n.t('common.close'),
      primaryClass: 'btn-default',
      secondary: null
    });
  },

  goToView: function(event) {
    App.router.route(event.context.get('internalAmbariUrl'));
  }

});