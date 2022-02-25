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

App.NodeManagersLiveView = App.TextDashboardWidgetView.extend(App.EditableWithLimitWidgetMixin, {

  hiddenInfo: function () {
    var nmActive = this.get('model.nodeManagersCountActive') == null ? Em.I18n.t('services.service.summary.notAvailable') : this.get('model.nodeManagersCountActive');
    var nmLost = this.get('model.nodeManagersCountLost') == null ? Em.I18n.t('services.service.summary.notAvailable') : this.get('model.nodeManagersCountLost');
    var nmUnhealthy = this.get('model.nodeManagersCountUnhealthy') == null ? Em.I18n.t('services.service.summary.notAvailable') : this.get('model.nodeManagersCountUnhealthy');
    var nmRebooted = this.get('model.nodeManagersCountRebooted') == null ? Em.I18n.t('services.service.summary.notAvailable'): this.get('model.nodeManagersCountRebooted');
    var nmDecom = this.get('model.nodeManagersCountDecommissioned') == null ? Em.I18n.t('services.service.summary.notAvailable') : this.get('model.nodeManagersCountDecommissioned');
    return [
      nmActive + " active",
      nmLost + " lost",
      nmUnhealthy + " unhealthy",
      nmRebooted + " rebooted",
      nmDecom + " decommissioned"
    ];
  }.property('model.nodeManagersCountActive', 'model.nodeManagersCountLost',
    'model.nodeManagersCountUnhealthy', 'model.nodeManagersCountRebooted', 'model.nodeManagersCountDecommissioned'),

  hiddenInfoClass: "hidden-info-five-line",

  maxValue: 100,

  isDataAvailable: Em.computed.and('!model.metricsNotAvailable', 'App.router.clusterController.isComponentsStateLoaded'),

  nodeManagersLive: Em.computed.alias('model.nodeManagersCountActive'),

  data: function () {
    var nodeManagers = this.get('model.nodeManagersTotal');
    var nodeManagersLive = this.get('nodeManagersLive');
    if (!nodeManagersLive || !nodeManagers) {
      return null;
    }
    return (nodeManagersLive / nodeManagers).toFixed(2) * 100;
  }.property('model.nodeManagersTotal', 'nodeManagersLive'),

  content: function () {
    return !this.get('nodeManagersLive') || !this.get('model.nodeManagersTotal') ? Em.I18n.t('services.service.summary.notAvailable') : this.get('nodeManagersLive') + '/' + this.get('model.nodeManagersTotal');
  }.property('model.nodeManagersTotal', 'nodeManagersLive'),

  hintInfo: function () {
    var maxTmp = parseFloat(this.get('maxValue'));
    return Em.I18n.t('dashboard.widgets.hintInfo.hint1').format(maxTmp);
  }.property('maxValue')

});
