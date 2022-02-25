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
var batchUtils = require('utils/batch_scheduled_requests');
var misc = require('utils/misc');
require('views/main/service/service');
require('data/service_graph_config');

App.MainServiceInfoSummaryView = Em.View.extend({
  templateName: require('templates/main/service/info/summary'),

  attributes: null,

  /**
   * Contain array with list of groups of master components from <code>App.Service.hostComponets</code> which are
   * <code>App.HostComponent</code> models.
   * @type {{title: String, hosts: String[], components: App.HostComponent[]}[]}
   */
  mastersObj: [
    {
      components: []
    }
  ],
  mastersLength: 0,

  /**
   * Contain array with list of slave components models <code>App.SlaveComponent</code>.
   * @type {App.SlaveComponent[]}
   */
  slavesObj: [],
  slavesLength: 0,

  /**
   * Contain array with list of client components models <code>App.ClientComponent</code>.
   * @type {App.ClientComponent[]}
   */
  clientObj: [],
  clientsLength: 0,
  
  /**
   * @type {boolean}
   */
  hasComponents: Em.computed.or('mastersLength', 'slavesLength', 'clientsLength'),

  /**
   *  @property {String} templatePathPrefix - base path for custom templates
   *    if you want to add custom template, add <service_name>.hbs file to
   *    templates/main/service/info/summary folder.
   */
  templatePathPrefix: 'templates/main/service/info/summary/',
  /** @property {Ember.View} serviceSummaryView - view to embed, computed in
   *  <code>loadServiceSummary()</code>
   */
  serviceSummaryView: null,


  // TODO implement filtering of views related to current service
  ///**
  // * @type {App.ViewInstance}
  // */
  /*views: function () {
    return App.router.get('loggedIn') ? App.router.get('mainViewsController.visibleAmbariViews') : [];
  }.property('App.router.mainViewsController.visibleAmbariViews.[]', 'App.router.loggedIn'),*/

  /**
   * @property {Object} serviceCustomViewsMap - custom views to embed
   *
   */
  serviceCustomViewsMap: function() {
    return {
      HBASE: App.MainDashboardServiceHbaseView,
      HDFS: App.MainDashboardServiceHdfsView,
      ONEFS: App.MainDashboardServiceOnefsView,
      STORM: App.MainDashboardServiceStormView,
      YARN: App.MainDashboardServiceYARNView,
      RANGER: App.MainDashboardServiceRangerView,
      FLUME: App.MainDashboardServiceFlumeView,
      HIVE:  App.MainDashboardServiceHiveView
    }
  }.property('serviceName'),
  /** @property collapsedMetrics {object[]} - metrics list for collapsed section
   *    structure of element from list:
   *      @property {string} header - title for section
   *      @property {string} id - id of section for toggling, like: metric1
   *      @property {string} toggleIndex - passed to `data-parent` attribute, like: #metric1
   *      @property {Em.View} metricView - metric view class
   */
  collapsedSections: null,

  servicesHaveClients: Em.computed.alias('App.services.hasClient'),

  hasManyServers: Em.computed.gt('servers.length', 1),

  clientsHostText: function () {
    if (this.get('controller.content.installedClients').length === 0) {
      return '';
    }
    if (this.get("hasManyClients")) {
      return Em.I18n.t('services.service.summary.viewHosts');
    }
    return Em.I18n.t('services.service.summary.viewHost');
  }.property("hasManyClients"),

  hasManyClients: Em.computed.gt('controller.content.installedClients.length', 1),

  servers: function () {
    var result = [];
    var service = this.get('controller.content');
    if (service.get("id") === "ZOOKEEPER" || service.get("id") === "FLUME") {
      var servers = service.get('hostComponents').filterProperty('isMaster');
      if (servers.length > 0) {
        result = [
          {
            'host': servers[0].get('displayName'),
            'isComma': false,
            'isAnd': false
          }
        ];
      }
      if (servers.length > 1) {
        result[0].isComma = true;
        result.push({
          'host': servers[1].get('displayName'),
          'isComma': false,
          'isAnd': false
        });
      }
      if (servers.length > 2) {
        result[1].isAnd = true;
        result.push({
          'host': Em.I18n.t('services.service.info.summary.serversHostCount').format(servers.length - 2),
          'isComma': false,
          'isAnd': false
        });
      }
    }
    return result;
  }.property('controller.content'),

  /**
   * Property related to ZOOKEEPER service, is unused for other services
   * @return {Object}
   */
  serversHost: function() {
    var service = this.get('controller.content');
    if (service.get("id") === "ZOOKEEPER" || service.get("id") === "FLUME") {
      var servers = service.get('hostComponents').filterProperty('isMaster');
      if (servers.length > 0) {
        return servers[0];
      }
    }
    return {};
  }.property('controller.content'),

  showComponentsTitleForNonMasters: Em.computed.or('!mastersLength', 'hasMultipleMasterGroups'),

  componentsLengthDidChange: function() {
    var self = this;
    if (!this.get('service') || this.get('service.deleteInProgress')) return;
    Em.run.once(self, 'setComponentsContent');
  }.observes('service.hostComponents.length', 'service.slaveComponents.@each.totalCount', 'service.clientComponents.@each.totalCount', 'svc.masterComponentGroups.length'),

  loadServiceSummary: function () {
    var serviceName = this.get('serviceName');
    var serviceSummaryView = null;

    if (!serviceName) {
      return;
    }

    if (this.get('oldServiceName')) {
      // do not delete it!
      return;
    }

    var customServiceView = this.get('serviceCustomViewsMap')[serviceName];
    if (customServiceView) {
      serviceSummaryView = customServiceView.extend({
        service: this.get('service')
      });
    } else {
      serviceSummaryView = Em.View.extend(App.MainDashboardServiceViewWrapper, {
        templateName: this.get('templatePathPrefix') + 'base'
      });
    }
    this.set('serviceSummaryView', serviceSummaryView);
    this.set('oldServiceName', serviceName);
  }.observes('serviceName'),

  didInsertElement: function () {
    this._super();
    var svcName = this.get('controller.content.serviceName');
    this.set('service', this.getServiceModel(svcName));
    App.loadTimer.finish('Service Summary Page');
  },

  willDestroyElement: function() {
    this.set('service', null);
    this.get('mastersObj').clear();
    this.get('slavesObj').clear();
    this.get('clientObj').clear();
  },

  setComponentsContent: function() {
    Em.run.next(function() {
      if (Em.isNone(this.get('service'))) {
        return;
      }
      const masters = this.get('service.hostComponents').filterProperty('isMaster'),
        slaves = this.get('service.slaveComponents').toArray(),
        clients = this.get('service.clientComponents').toArray(),
        masterGroups = this.get('svc.masterComponentGroups') ? this.get('svc.masterComponentGroups').toArray() : [];

      if (this.get('mastersLength') !== masters.length || this.get('mastersObj.length') !== masterGroups.length) {
        let mastersInit = this.get('mastersObj').mapProperty('components').reduce((acc, group) => {
          return [...acc, ...group];
        }, []);
        this.updateComponentList(mastersInit, masters);
        this.set('mastersObj', this.getGroupedMasterComponents(mastersInit));
        this.set('mastersLength', masters.length);
      }
      if (this.get('slavesLength') !== slaves.length) {
        this.updateComponentList(this.get('slavesObj'), slaves);
        this.set('slavesLength', slaves.length);
      }
      if (this.get('clientsLength') !== clients.length) {
        this.updateComponentList(this.get('clientObj'), clients);
        this.set('clientsLength', clients.length);
      }
    }.bind(this));
  },


  updateComponentList: function(source, data) {
    var sourceIds = source.mapProperty('id');
    var dataIds = data.mapProperty('id');
    if (sourceIds.length === 0) {
      source.pushObjects(data);
    }
    if (source.length > data.length) {
      sourceIds.forEach(function(item) {
        if (!dataIds.contains(item)) {
          var sourceItem = source.findProperty('id',item);
          source.removeObject(sourceItem);
        }
      });
    } else if (source.length < data.length) {
      dataIds.forEach(function(item, index) {
        if (!sourceIds.contains(item)) {
          source.pushObject(data.objectAt(index));
        }
      });
    }
  },

  data:{
    hive:{
      "database":"PostgreSQL",
      "databaseName":"hive",
      "user":"hive"
    }
  },

  /**
   * @type {Em.View}
   * Wrapper for displayName. used to render correct display name for mysql_server
   */
  componentNameView: Ember.View.extend({
    template: Ember.Handlebars.compile('{{view.displayName}}'),
    comp : null,
    displayName: function () {
      if (this.get('comp.componentName') === 'MYSQL_SERVER') {
        return this.t('services.hive.databaseComponent');
      }
      return this.get('comp.displayName');
    }.property('comp')
  }),

  service: null,

  svc: function () {
    let svc = this.get('controller.content');
    const svcName = svc ? svc.get('serviceName') : null;
    if (svcName) {
      switch (svcName.toLowerCase()) {
        case 'hdfs':
          svc = App.HDFSService.find().objectAt(0);
          break;
        case 'yarn':
          svc = App.YARNService.find().objectAt(0);
          break;
        case 'hbase':
          svc = App.HBaseService.find().objectAt(0);
          break;
        case 'flume':
          svc = App.FlumeService.find().objectAt(0);
          break;
        default:
          break;
      }
    }
    return svc;
  }.property('controller.content.serviceName').volatile(),

  getServiceModel: function (serviceName) {
    var extended = App.Service.extendedModel[serviceName];
    if (extended) {
      return App[extended].find().objectAt(0);
    }
    return App.Service.find(serviceName);
  },

  /**
   * @type {boolean}
   * @default true
   */
  isHide: true,

  /**
   * @type {Em.View}
   */
  moreStatsView: Em.View.extend({
    tagName: "a",
    template: Ember.Handlebars.compile('{{t services.service.summary.moreStats}}'),
    attributeBindings: ['href'],
    classNames: ['more-stats'],
    click: function () {
      this._parentView._parentView.set('isHide', false);
      this.remove();
    },
    href: 'javascript:void(null)'
  }),

  serviceName: Em.computed.alias('service.serviceName'),

  oldServiceName:'',

  /*
   * 'Restart Required bar' start
   */
  componentsCount: null,
  hostsCount: null,

  /**
   * @type {boolean}
   */
  shouldShowNoneCount: true,

  /**
   * Define if service has alert definitions defined
   * @type {Boolean}
   */
  hasAlertDefinitions: function () {
    return App.AlertDefinition.find().someProperty('serviceName', this.get('controller.content.serviceName'));
  }.property('controller.content.serviceName'),

  updateComponentInformation: function () {
    var hc = this.get('controller.content.restartRequiredHostsAndComponents');
    var hostsCount = 0;
    var componentsCount = 0;
    for (var host in hc) {
      hostsCount++;
      componentsCount += hc[host].length;
    }
    this.set('componentsCount', componentsCount);
    this.set('hostsCount', hostsCount);
  }.observes('controller.content.restartRequiredHostsAndComponents'),

  rollingRestartSlaveComponentName: function() {
    return batchUtils.getRollingRestartComponentName(this.get('serviceName'));
  }.property('serviceName'),

  rollingRestartActionName: function() {
    var label = null;
    var componentName = this.get('rollingRestartSlaveComponentName');
    if (componentName) {
      label = Em.I18n.t('rollingrestart.dialog.title').format(App.format.role(componentName, false));
    }
    return label;
  }.property('rollingRestartSlaveComponentName'),

  restartAllStaleConfigComponents: function () {
    var self = this;
    var serviceDisplayName = this.get('service.displayName');
    var bodyMessage = Em.Object.create({
      confirmMsg: Em.I18n.t('services.service.restartAll.confirmMsg').format(serviceDisplayName),
      confirmButton: Em.I18n.t('services.service.restartAll.confirmButton'),
      additionalWarningMsg: this.get('service.passiveState') === 'OFF' ? Em.I18n.t('services.service.restartAll.warningMsg.turnOnMM').format(serviceDisplayName) : null
    });

    var isNNAffected = false;
    var restartRequiredHostsAndComponents = this.get('controller.content.restartRequiredHostsAndComponents');
    for (var hostName in restartRequiredHostsAndComponents) {
      restartRequiredHostsAndComponents[hostName].forEach(function (hostComponent) {
        if (hostComponent === 'NameNode')
          isNNAffected = true;
      })
    }
    if (serviceDisplayName === 'HDFS' && isNNAffected &&
      this.get('controller.content.hostComponents').filterProperty('componentName', 'NAMENODE').someProperty('workStatus', App.HostComponentStatus.started)) {
      App.router.get('mainServiceItemController').checkNnLastCheckpointTime(function () {
        return App.showConfirmationFeedBackPopup(function (query) {
          var selectedService = self.get('service.id');
          batchUtils.restartAllServiceHostComponents(serviceDisplayName, selectedService, true, query);
        }, bodyMessage);
      });
    } else {
      return App.showConfirmationFeedBackPopup(function (query) {
        var selectedService = self.get('service.id');
        batchUtils.restartAllServiceHostComponents(serviceDisplayName, selectedService, true, query);
      }, bodyMessage);
    }
  },

  rollingRestartStaleConfigSlaveComponents: function (componentName) {
    batchUtils.launchHostComponentRollingRestart(componentName.context, this.get('service.displayName'), this.get('service.passiveState') === "ON", true);
  },

  hasMultipleMasterGroups: Em.computed.gt('mastersObj.length', 1),

  getGroupedMasterComponents: function (components) {
    switch (this.get('serviceName')) {
      case 'HDFS':
        const masterComponentGroups = this.get('service.masterComponentGroups'),
          hostComponents = this.get('service.hostComponents'),
          zkfcs = hostComponents.filterProperty('componentName', 'ZKFC'),
          hasNameNodeFederation = this.get('service.hasMultipleMasterComponentGroups');
        let groups = [];
        hostComponents.forEach(component => {
          if (component.get('isMaster') && component.get('componentName') !== 'JOURNALNODE') {
            const hostName = component.get('hostName'),
              zkfc = zkfcs.findProperty('hostName', hostName);
            if (hasNameNodeFederation) {
              const name = component.get('haNameSpace'),
                existingGroup = groups.findProperty('name', name),
                currentGroup = existingGroup || Object.assign({}, masterComponentGroups.findProperty('name', name));
              if (!existingGroup) {
                groups.push(currentGroup);
                Em.setProperties(currentGroup, {
                  components: [],
                  componentWidgetsView: App.HDFSSummaryWidgetsView.extend({
                    nameSpace: name
                  })
                });
              }
              currentGroup.components.push(component);
              if (zkfc) {
                zkfc.set('isSubComponent', true);
                currentGroup.components.push(zkfc);
              }
            } else {
              if (!groups.length) {
                groups.push({
                  components: [],
                  componentWidgetsView: App.HDFSSummaryWidgetsView.extend({
                    nameSpace: component.get('haNameSpace') || 'default',
                    showSlaveComponents: true
                  })
                });
              }
              const defaultGroupComponents = groups[0].components;
              defaultGroupComponents.push(component);
              if (zkfc) {
                zkfc.set('isSubComponent', true);
                defaultGroupComponents.push(zkfc);
              }
            }
          }
        });
        return groups.sortProperty('name');
      default:
        return [
          {
            components
          }
        ];
    }
  }
});
