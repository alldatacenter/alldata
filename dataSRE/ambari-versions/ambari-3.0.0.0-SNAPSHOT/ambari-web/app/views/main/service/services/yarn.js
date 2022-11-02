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
var date = require('utils/date/date');
var numberUtils = require('utils/number_utils');

App.MainDashboardServiceYARNView = App.MainDashboardServiceView.extend({
  templateName: require('templates/main/service/services/yarn'),
  serviceName: 'YARN',

  nodeHeap: App.MainDashboardServiceView.formattedHeap('dashboard.services.yarn.nodes.heapUsed', 'service.jvmMemoryHeapUsed', 'service.jvmMemoryHeapMax'),
  nodeHeapPercent: App.MainDashboardServiceView.formattedHeapPercent('dashboard.services.yarn.nodes.heapUsedPercent', 'service.jvmMemoryHeapUsed', 'service.jvmMemoryHeapMax'),

  nodeManagerComponent: Em.Object.create({
    componentName: 'NODEMANAGER'
  }),
  
  yarnClientComponent: Em.Object.create({
    componentName: 'YARN_CLIENT'
  }),

  hasManyYarnClients: Em.computed.gt('service.installedClients', 1),

  nodeUptime: function () {
    var uptime = this.get('service.resourceManagerStartTime');
    if (uptime && uptime > 0) {
      var diff = App.dateTime() - uptime;
      if (diff < 0) {
        diff = 0;
      }
      var formatted = date.timingFormat(diff);
      return this.t('dashboard.services.uptime').format(formatted);
    }
    return this.t('services.service.summary.notRunning');
  }.property("service.resourceManagerStartTime"),

  nodeManagerText: Em.computed.countBasedMessage('service.nodeManagersTotal', '', Em.I18n.t('services.service.summary.viewHost'), Em.I18n.t('services.service.summary.viewHosts')),

  _nmActive: Em.computed.formatUnavailable('service.nodeManagersCountActive'),
  _nmLost: Em.computed.formatUnavailable('service.nodeManagersCountLost'),
  _nmUnhealthy: Em.computed.formatUnavailable('service.nodeManagersCountUnhealthy'),
  _nmRebooted: Em.computed.formatUnavailable('service.nodeManagersCountRebooted'),
  _nmDecom: Em.computed.formatUnavailable('service.nodeManagersCountDecommissioned'),

  _allocated: Em.computed.formatUnavailable('service.containersAllocated'),
  _pending: Em.computed.formatUnavailable('service.containersPending'),
  _reserved: Em.computed.formatUnavailable('service.containersReserved'),

  _appsSubmitted: Em.computed.formatUnavailable('service.appsSubmitted'),
  _appsRunning: Em.computed.formatUnavailable('service.appsRunning'),
  _appsPending: Em.computed.formatUnavailable('service.appsPending'),
  _appsCompleted: Em.computed.formatUnavailable('service.appsCompleted'),
  _appsKilled: Em.computed.formatUnavailable('service.appsKilled'),
  _appsFailed: Em.computed.formatUnavailable('service.appsFailed'),

  allocatedMemoryFormatted: function() {
    return numberUtils.bytesToSize(this.get('service.allocatedMemory'), 1, 'parseFloat', 1024 * 1024);
  }.property('service.allocatedMemory'),

  reservedMemoryFormatted: function() {
    return numberUtils.bytesToSize(this.get('service.reservedMemory'), 1, 'parseFloat', 1024 * 1024);
  }.property('service.reservedMemory'),

  availableMemoryFormatted: function() {
    return numberUtils.bytesToSize(this.get('service.availableMemory'), 1, 'parseFloat', 1024 * 1024);
  }.property('service.availableMemory'),

  _queuesCountFormatted: Em.computed.formatUnavailable('service.queuesCount'),
  queues: Em.computed.i18nFormat('dashboard.services.yarn.queues.msg', '_queuesCountFormatted'),

  didInsertElement: function(){
    App.tooltip($("[rel='queue-tooltip']"), {html: true, placement: "right"});
    App.tooltip($("[rel='tooltip']"));
  },

  willDestroyElement: function(){
    $("[rel='queue-tooltip']").tooltip('destroy');
  },

  isNodeManagerCreated: function () {
    return App.SlaveComponent.find('NODEMANAGER').get('totalCount') > 0;
  }.property('App.router.clusterController.isComponentsStateLoaded')
});
