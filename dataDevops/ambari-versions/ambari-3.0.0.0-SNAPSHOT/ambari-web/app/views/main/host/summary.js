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

App.MainHostSummaryView = Em.View.extend(App.HiveInteractiveCheck, App.TimeRangeMixin, {

  templateName: require('templates/main/host/summary'),

  /**
   * List of custom view for some host components
   * @type {Em.Object}
   * Format:
   *  <code>
   *    {
   *      COMPONENT_NAME1: VIEW1,
   *      COMPONENT_NAME2: VIEW2
   *      ....
   *    }
   *  </code>
   */
  hostComponentViewMap: Em.Object.create({
    'DATANODE': App.DataNodeComponentView,
    'NODEMANAGER': App.NodeManagerComponentView,
    'HBASE_REGIONSERVER': App.RegionServerComponentView,
    'TASKTRACKER': App.TaskTrackerComponentView
  }),

  /**
   * @type {bool}
   */
  isStopCommand: true,

  /**
   * @type {App.Host}
   */
  content: Em.computed.alias('App.router.mainHostDetailsController.content'),

  /**
   * Host metrics panel not displayed when Metrics service (ex:Ganglia) is not in stack definition.
   */
  hasHostMetricsService: Em.computed.gt('App.services.hostMetrics.length', 0),

  nameNodeComponent: Em.computed.findBy('content.hostComponents', 'componentName', 'NAMENODE'),

  hasNameNode: Em.computed.bool('nameNodeComponent'),

  showHostMetricsBlock: Em.computed.or('hasHostMetricsService', 'hasNameNode'),

  nameNodeWidgets: function () {
    const hasNameNode = this.get('hasNameNode');
    let widgets = [];
    if (hasNameNode) {
      const model = App.HDFSService.find('HDFS'),
        hostName = this.get('content.hostName'),
        widgetsDefinitions = require('data/dashboard_widgets').toMapByProperty('viewName');
      widgets.pushObjects([
        App.NameNodeHeapPieChartView.extend({
          model,
          hostName,
          widgetHtmlId: 'nn-heap',
          title: Em.I18n.t('dashboard.widgets.NameNodeHeap'),
          showActions: false,
          widget: {
            threshold: widgetsDefinitions.NameNodeHeapPieChartView.threshold,
          }
        }),
        App.NameNodeCpuPieChartView.extend({
          widgetHtmlId: 'nn-cpu',
          title: Em.I18n.t('dashboard.widgets.NameNodeCpu'),
          showActions: false,
          widget: {
            threshold: widgetsDefinitions.NameNodeCpuPieChartView.threshold
          },
          subGroupId: this.get('nameNodeComponent.haNameSpace'),
          activeNameNodes: [this.get('nameNodeComponent')],
          nameNode: this.get('nameNodeComponent')
        }),
        App.NameNodeRpcView.extend({
          model,
          hostName,
          widgetHtmlId: 'nn-rpc',
          title: Em.I18n.t('dashboard.widgets.NameNodeRpc'),
          showActions: false,
          widget: {
            threshold: widgetsDefinitions.NameNodeRpcView.threshold
          }
        }),
        App.NameNodeUptimeView.extend({
          model,
          hostName,
          widgetHtmlId: 'nn-uptime',
          title: Em.I18n.t('dashboard.widgets.NameNodeUptime'),
          showActions: false,
          subGroupId: this.get('nameNodeComponent.haNameSpace')
        })
      ]);
    }
    return widgets;
  }.property('hasNameNode'),

  /**
   * Message for "restart" block
   * @type {String}
   */
  needToRestartMessage: function() {
    var componentsCount, word;
    componentsCount = this.get('content.componentsWithStaleConfigsCount');
    if (componentsCount > 1) {
      word = Em.I18n.t('common.components').toLowerCase();
    } else {
      word = Em.I18n.t('common.component').toLowerCase();
    }
    return Em.I18n.t('hosts.host.details.needToRestart').format(this.get('content.componentsWithStaleConfigsCount'), word);
  }.property('content.componentsWithStaleConfigsCount'),

  willInsertElement: function() {
    this.sortedComponentsFormatter();
    this.addObserver('content.hostComponents.length', this, 'sortedComponentsFormatter');
    if (this.get('installedServices').indexOf('HIVE') !== -1) {
      this.loadHiveConfigs();
    }
  },

  didInsertElement: function () {
    this._super();
    this.addToolTip();
  },

  /**
   * Create tooltip for "Add" button if nothing to add to the current host
   */
  addToolTip: function() {
    if (this.get('addComponentDisabled')) {
      App.tooltip($('#add_component'), {title: Em.I18n.t('services.nothingToAdd')});
    }
  }.observes('addComponentDisabled'),

  /**
   * List of installed services
   * @type {String[]}
   */
  installedServices: function () {
    return App.Service.find().mapProperty('serviceName');
  }.property('App.router.clusterController.dataLoadList.services'),

  /**
   * List of installed masters and slaves
   * Masters first, then slaves
   * @type {App.HostComponent[]}
   */
  sortedComponents: [],

  /**
   * Update <code>sortedComponents</code>
   * Master components first, then slaves and clients
   */
  sortedComponentsFormatter: function() {
    const hostComponentViewMap = this.get('hostComponentViewMap');
    let sortedComponentsArray = [];
    this.get('content.hostComponents').forEach(function (component) {
      component.set('viewClass', hostComponentViewMap[component.get('componentName')] ? hostComponentViewMap[component.get('componentName')] : App.HostComponentView);
      if (component.get('isClient')) {
        component.set('isLast', true);
        component.set('isInstallFailed', ['INSTALL_FAILED', 'INIT'].contains(component.get('workStatus')));
      }
      sortedComponentsArray.push(component);
    });

    sortedComponentsArray = sortedComponentsArray.sort((a, b) => a.get('displayName').localeCompare(b.get('displayName')));
    this.set('sortedComponents', sortedComponentsArray);
  },

  /**
   * Template for addable component
   * @type {Em.Object}
   */
  addableComponentObject: Em.Object.extend({
    componentName: '',
    displayName: function () {
      return App.format.role(this.get('componentName'), false);
    }.property('componentName')
  }),

  /**
   * If host lost heartbeat, components can't be added on it
   * @type {bool}
   */
  isAddComponent: Em.computed.notEqual('content.healthClass', 'health-status-DEAD-YELLOW'),

  /**
   * Disable "Add" button if components can't be added to the current host
   * @type {bool}
   */
  addComponentDisabled: Em.computed.or('!isAddComponent', '!addableComponents.length'),

  /**
   * List of components that may be added to the current host
   * @type {Em.Object[]}
   */
  addableComponents: function () {
    var components = [];
    var self = this;
    if (this.get('content.hostComponents')) {
      var installedComponents = this.get('content.hostComponents').mapProperty('componentName');
      var addableToHostComponents = App.StackServiceComponent.find().filterProperty('isAddableToHost');
      var installedServices = this.get('installedServices');

      addableToHostComponents.forEach(function (addableComponent) {
        if (installedServices.contains(addableComponent.get('serviceName'))
            && !installedComponents.contains(addableComponent.get('componentName'))
            && !this.hasCardinalityConflict(addableComponent.get('componentName'))) {
          if ((addableComponent.get('componentName') === 'OOZIE_SERVER') && !App.router.get('mainHostDetailsController.isOozieServerAddable') ||
            addableComponent.get('componentName') === 'HIVE_SERVER_INTERACTIVE' && !self.get('enableHiveInteractive')) {
            return;
          }
          if (installedServices.includes('HDFS') && addableComponent.get('componentName') === 'OZONE_DATANODE') {
            return;
          }
          components.pushObject(self.addableComponentObject.create({
            'componentName': addableComponent.get('componentName'),
            'serviceName': addableComponent.get('serviceName')
          }));
        }
      }, this);
    }
    return components;
  }.property('content.hostComponents.length', 'App.components.addableToHost.@each', 'enableHiveInteractive'),

  /**
   *
   * @param {string} componentName
   * @returns {boolean}
   */
  hasCardinalityConflict: function(componentName) {
    var totalCount = App.HostComponent.getCount(componentName, 'totalCount');
    var maxToInstall = App.StackServiceComponent.find(componentName).get('maxToInstall');
    return !(totalCount < maxToInstall);
  },

  /**
   * Formatted with <code>$.timeago</code> value of host's last heartbeat
   * @type {String}
   */
  timeSinceHeartBeat: function () {
    if (this.get('content.isNotHeartBeating')) {
      const d = this.get('content.lastHeartBeatTime');
      return d ? $.timeago(d) : '';
    }
    //when host hasn't lost heartbeat we assume that last heartbeat was a minute ago
    return Em.I18n.t('common.minute.ago');
  }.property('content.lastHeartBeatTime', 'content.isNotHeartBeating'),

  /**
   * Get clients with custom commands
   */
  clientsWithCustomCommands: function() {
    var clients = this.get('clients').rejectProperty('componentName', 'KERBEROS_CLIENT');
    var options = [];
    var clientWithCommands;
    clients.forEach(function(client) {
      var componentName = client.get('componentName');
      var customCommands = App.StackServiceComponent.find(componentName).get('customCommands');

      if (customCommands.length) {
        clientWithCommands = {
          label: client.get('displayName'),
          commands: []
        };
        customCommands.forEach(function(command) {
          clientWithCommands.commands.push({
            label: Em.I18n.t('services.service.actions.run.executeCustomCommand.menu').format(command),
            service: client.get('service.serviceName'),
            hosts: client.get('hostName'),
            component: componentName,
            command: command
          });
        });

        options.push(clientWithCommands);
      }
    });

    return options;
  }.property('controller'),

  /**
   * Call installClients method from controller for not installed client components
   */
  installClients: function () {
    this.get('controller').installClients(this.get('notInstalledClientComponents'));
  },

  /**
   * Call installClients method from controller for not install failed client components
   */
  reinstallClients: function () {
    this.get('controller').installClients(this.get('installFailedClients'));
  }
});
