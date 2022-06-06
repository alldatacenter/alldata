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
var uiEffects = require('utils/ui_effects');
var numberUtils = require('utils/number_utils');
require('views/main/service/info/components_list_view');

App.MainDashboardServiceHealthView = Em.View.extend({
  classNameBindings: ["healthStatus", "healthStatusClass"],
  //template: Em.Handlebars.compile(""),
  blink: false,
  tagName: 'span',
  attributeBindings: ['rel', 'title', 'data-original-title'],
  rel: 'HealthTooltip',
  'data-original-title': '',

  updateToolTip: function () {
    this.set('data-original-title', this.get('service.toolTipContent'));
  }.observes('service.toolTipContent'),

  startBlink: function () {
    this.set('blink', true);
  },

  doBlink: function () {
    var self = this;
    if (this.get('blink') && this.get("state") === "inDOM") {
      uiEffects.pulsate(self.$(), 1000, function () {
        self.doBlink();
      });
    }
  }.observes('blink'),

  stopBlink: function () {
    this.set('blink', false);
  },

  healthStatus: function () {
    var isClientOnlyService = App.get('services.clientOnly').contains(this.get('service.serviceName'));
    if (isClientOnlyService) {
      return 'glyphicon glyphicon-blackboard';
    }
    if (this.get('service.passiveState') !== 'OFF') {
      return 'icon-medkit';
    }
    var status = this.get('service.healthStatus');
    switch (status) {
      case 'green':
        status = App.Service.Health.live;
        this.stopBlink();
        break;
      case 'green-blinking':
        status = App.Service.Health.live;
        this.startBlink();
        break;
      case 'red-blinking':
        status = App.Service.Health.dead;
        this.startBlink();
        break;
      case 'yellow':
        status = App.Service.Health.unknown;
        break;
      default:
        status = App.Service.Health.dead;
        this.stopBlink();
        break;
    }

    return 'health-status-' + status;
  }.property('service.healthStatus', 'service.passiveState', 'service.serviceName'),

  healthStatusClass: function () {
    if (this.get('service.passiveState') !== 'OFF' || App.get('services.clientOnly').contains(this.get('service.serviceName'))) {
      return '';
    }
    switch (this.get('service.healthStatus')) {
      case 'green':
      case 'green-blinking':
        return App.healthIconClassGreen;
        break;
      case 'red':
      case 'red-blinking':
        return App.healthIconClassRed;
        break;
      case 'yellow':
        return App.healthIconClassYellow;
      default:
        return '';
    }
  }.property('service.healthStatus', 'service.passiveState', 'service.serviceName'),

  didInsertElement: function () {
    this.updateToolTip();
    App.tooltip($("[rel='HealthTooltip']"), {html: true});
    this.doBlink(); // check for blink availability
  }
});

App.ComponentLiveTextView = Em.View.extend({
  liveComponents: null,
  totalComponents: null,
  color: function () {
    return this.get("liveComponents") === 0 && this.get('totalComponents') !== 0;
  }.property("liveComponents", 'totalComponents')
});

App.MainDashboardServiceViewWrapper = Em.Mixin.create({
  layoutName: require('templates/main/service/service')
});

App.MainDashboardServiceView = Em.View.extend(App.MainDashboardServiceViewWrapper, {
  classNames: ['service', 'clearfix'],

  data: function () {
    return this.get('controller.data.' + this.get('serviceName'));
  }.property('controller.data'),

  dashboardMasterComponentView: App.SummaryMasterComponentsView.extend({
    mastersComp: Em.computed.alias('parentView.parentView.mastersObj')
  }),

  alertsCount: Em.computed.alias('service.alertsCount'),

  hasCriticalAlerts: Em.computed.alias('service.hasCriticalAlerts'),

  isCollapsed: false,

  masters: Em.computed.filterBy('service.hostComponents', 'isMaster', true),

  clients: function () {
    var clients = this.get('service.hostComponents').filterProperty('isClient', true);
    var len = clients.length;
    var template = 'dashboard.services.{0}.client'.format(this.get('serviceName').toLowerCase());
    if (len > 1) {
      template += 's';
    }

    return {
      title: this.t(template).format(len),
      component: clients.objectAt(0)
    };
  }.property('service'),

  hasMultipleMasterGroups: Em.computed.gt('parentView.mastersObj.length', 1),

  showComponentsTitleForNonMasters: Em.computed.or('!masters.length', 'hasMultipleMasterGroups'),

  /**
   * Check if service component is created
   * @param componentName
   * @returns {Boolean}
   */
  isServiceComponentCreated: function (componentName) {
    return App.MasterComponent.find().mapProperty('componentName').concat(
      App.ClientComponent.find().mapProperty('componentName'),
      App.SlaveComponent.find().mapProperty('componentName')
    ).contains(componentName);
  }

});

App.MainDashboardServiceView.reopenClass({
  formattedHeapPercent: function (i18nKey, heapUsedKey, heapMaxKey) {
    return Em.computed(heapUsedKey, heapMaxKey, function () {
      var memUsed = Em.get(this, heapUsedKey);
      var memMax = Em.get(this, heapMaxKey);
      var percent = memMax > 0 ? 100 * memUsed / memMax : 0;
      return Em.I18n.t(i18nKey).format(percent.toFixed(1));
    });
  },
  formattedHeap: function (i18nKey, heapUsedKey, heapMaxKey) {
    return Em.computed(heapUsedKey, heapMaxKey, function () {
      var memUsed = Em.get(this, heapUsedKey);
      var memMax = Em.get(this, heapMaxKey);
      return Em.I18n.t(i18nKey).format(
        numberUtils.bytesToSize(memUsed, 1, 'parseFloat'),
        numberUtils.bytesToSize(memMax, 1, 'parseFloat'));
    });
  }
});