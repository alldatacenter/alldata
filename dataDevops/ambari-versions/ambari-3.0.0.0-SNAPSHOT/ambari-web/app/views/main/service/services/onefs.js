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
var chartUtils = require('utils/chart_utils');

function diskPart(i18nKey, totalKey, usedKey) {
  return Em.computed(totalKey, usedKey, function () {
    var text = Em.I18n.t(i18nKey);
    var total = this.get(totalKey);
    var used = this.get(usedKey);
    var percent = total > 0 ? ((used * 100) / total).toFixed(2) : 0;
    if (percent == "NaN" || percent < 0) {
      percent = Em.I18n.t('services.service.summary.notAvailable') + " ";
    }
    return text.format(numberUtils.bytesToSize(used, 1, 'parseFloat'), numberUtils.bytesToSize(total, 1, 'parseFloat'));
  });
}

function diskPartPercent(i18nKey, totalKey, usedKey) {
  return Em.computed(totalKey, usedKey, function () {
    var text = Em.I18n.t(i18nKey);
    var total = this.get(totalKey);
    var used = this.get(usedKey);
    var percent = total > 0 ? ((used * 100) / total).toFixed(2) : 0;
    if (percent == "NaN" || percent < 0) {
      percent = Em.I18n.t('services.service.summary.notAvailable') + " ";
    }
    return text.format(percent);
  });
}

App.MainDashboardServiceOnefsView = App.MainDashboardServiceView.extend({
  templateName: require('templates/main/service/services/onefs'),
  serviceName: 'ONEFS',
  Chart: App.ChartPieView.extend({
    service: null,
    color: '#0066B3',
    stroke: '#0066B3',
    palette: new Rickshaw.Color.Palette({
      scheme: chartUtils.getColorSchemeForGaugeWidget()
    }),
    data: function () {
      var remaining = Number(this.get('service.capacityRemaining'));
      var used = Number(this.get('service.capacityTotal')) - remaining;
      return [ used, remaining ];
    }.property('service.capacityUsed', 'service.capacityTotal')
  }),

  metricsNotAvailableObserver: function () {
    if(!this.get("service.metricsNotAvailable")) {
      App.tooltip($("[rel='tooltip']"));
    }
  }.observes("service.metricsNotAvailable"),

  willDestroyElement: function() {
    $("[rel='tooltip']").tooltip('destroy');
  },

  dataNodesDead: Em.computed.alias('service.dataNodesInstalled'),

  journalNodesLive: function () {
    return this.get('service.journalNodes').filterProperty("workStatus", "STARTED").get("length");
  }.property("service.journalNodes.@each.workStatus"),

  journalNodesTotal: Em.computed.alias('service.journalNodes.length'),

  dfsTotalBlocks: Em.computed.formatUnavailable('service.dfsTotalBlocks'),

  dfsTotalFiles: Em.computed.formatUnavailable('service.dfsTotalFiles'),

  dfsCorruptBlocks: Em.computed.formatUnavailable('service.dfsCorruptBlocks'),

  dfsMissingBlocks: Em.computed.formatUnavailable('service.dfsMissingBlocks'),

  dfsUnderReplicatedBlocks: Em.computed.formatUnavailable('service.dfsUnderReplicatedBlocks'),

  nodeUptime: function () {
    var uptime = this.get('service.nameNodeStartTime');
    if (uptime && uptime > 0){
      var diff = App.dateTime() - uptime;
      if (diff < 0) {
        diff = 0;
      }
      var formatted = date.timingFormat(diff);
      return this.t('dashboard.services.uptime').format(formatted);
    }
    return this.t('services.service.summary.notRunning');
  }.property("service.nameNodeStartTime"),

  nodeHeapPercent: App.MainDashboardServiceView.formattedHeapPercent('dashboard.services.hdfs.nodes.heapUsedPercent', 'service.jvmMemoryHeapUsed', 'service.jvmMemoryHeapMax'),
  nodeHeap: App.MainDashboardServiceView.formattedHeap('dashboard.services.hdfs.nodes.heapUsed', 'service.jvmMemoryHeapUsed', 'service.jvmMemoryHeapMax'),

  dfsUsedDisk: diskPart('dashboard.services.hdfs.capacityUsed', 'service.capacityTotal', 'service.capacityUsed'),
  dfsUsedDiskPercent: diskPartPercent('dashboard.services.hdfs.capacityUsedPercent', 'service.capacityTotal', 'service.capacityUsed'),

  nonDfsUsed: function () {
    var total = this.get('service.capacityTotal');
    var remaining = this.get('service.capacityRemaining');
    var dfsUsed = this.get('service.capacityUsed');
    return (Em.isNone(total) || Em.isNone(remaining) || Em.isNone(dfsUsed)) ? null : total - remaining - dfsUsed;
  }.property('service.capacityTotal', 'service.capacityRemaining', 'service.capacityUsed'),

  nonDfsUsedDisk: diskPart('dashboard.services.hdfs.capacityUsed', 'service.capacityTotal', 'nonDfsUsed'),
  nonDfsUsedDiskPercent: diskPartPercent('dashboard.services.hdfs.capacityUsedPercent', 'service.capacityTotal', 'nonDfsUsed'),

  remainingDisk: diskPart('dashboard.services.hdfs.capacityUsed', 'service.capacityTotal', 'service.capacityRemaining'),
  remainingDiskPercent: diskPartPercent('dashboard.services.hdfs.capacityUsedPercent', 'service.capacityTotal', 'service.capacityRemaining'),

  dataNodeComponent: Em.Object.create({
    componentName: 'DATANODE'
  }),

  nfsGatewayComponent: Em.Object.create({
    componentName: 'NFS_GATEWAY'
  }),

  /**
   * Define if NFS_GATEWAY is present in the installed stack
   * @type {Boolean}
   */
  isNfsInStack: function () {
    return App.StackServiceComponent.find().someProperty('componentName', 'NFS_GATEWAY');
  }.property(),

  journalNodeComponent: Em.Object.create({
    componentName: 'JOURNALNODE'
  }),

  /**
   * @type {string}
   */
  safeModeStatus: function () {
    var safeMode = this.get('service.safeModeStatus');
    if (Em.isNone(safeMode)) {
      return Em.I18n.t("services.service.summary.notAvailable");
    } else if (safeMode.length === 0) {
      return Em.I18n.t("services.service.summary.safeModeStatus.notInSafeMode");
    } else {
      return Em.I18n.t("services.service.summary.safeModeStatus.inSafeMode");
    }
  }.property('service.safeModeStatus'),

  /**
   * @type {string}
   */
  upgradeStatus: function () {
    var upgradeStatus = this.get('service.upgradeStatus');
    var healthStatus = this.get('service.healthStatus');
    if (upgradeStatus == 'true') {
      return Em.I18n.t('services.service.summary.pendingUpgradeStatus.notPending');
    } else if (upgradeStatus == 'false' && healthStatus == 'green') {
      return Em.I18n.t('services.service.summary.pendingUpgradeStatus.notFinalized');
    } else {
      // upgrade status == null
      return Em.I18n.t("services.service.summary.notAvailable");
    }
  }.property('service.upgradeStatus', 'service.healthStatus'),

  /**
   * @type {boolean}
   */
  isUpgradeStatusWarning: function () {
    return this.get('service.upgradeStatus') == 'false' && this.get('service.healthStatus') == 'green';
  }.property('service.upgradeStatus', 'service.healthStatus'),

  isDataNodeCreated: function () {
    return this.isServiceComponentCreated('DATANODE');
  }.property('App.router.clusterController.isComponentsStateLoaded'),

  isJournalNodeCreated: function () {
    return this.isServiceComponentCreated('JOURNALNODE');
  }.property('App.router.clusterController.isComponentsStateLoaded')
});
