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

function counterOrNA(key) {
  var _key = 'model.{0}.length'.format(key);
  return Em.computed(_key, function () {
    if (Em.isNone(this.get('model.'+ key)) || this.get('model.metricsNotAvailable')) {
      return Em.I18n.t('services.service.summary.notAvailable');
    }
    return this.get(_key);
  });
}

App.DataNodeUpView = App.TextDashboardWidgetView.extend(App.EditableWithLimitWidgetMixin, {

  hiddenInfo: function () {
    return [
      this.get('dataNodesLive') + ' ' + Em.I18n.t('dashboard.services.hdfs.nodes.live'),
      this.get('dataNodesDead') + ' ' + Em.I18n.t('dashboard.services.hdfs.nodes.dead'),
      this.get('dataNodesDecom')+ ' ' + Em.I18n.t('dashboard.services.hdfs.nodes.decom')
    ];
  }.property('dataNodesLive', 'dataNodesDead', 'dataNodesDecom'),

  hiddenInfoClass: "hidden-info-three-line",

  maxValue: 100,

  dataNodesLive: counterOrNA('liveDataNodes'),

  dataNodesDead: counterOrNA('deadDataNodes'),

  dataNodesDecom: counterOrNA('decommissionDataNodes'),

  /**
   * @type {?number}
   */
  data: function () {
    if (this.get('someMetricsNA')) {
      return null;
    }
    return (this.get('dataNodesLive') / this.get('model.dataNodesTotal')).toFixed(2) * 100;
  }.property('model.dataNodesTotal', 'dataNodesLive', 'someMetricsNA'),

  /**
   * @type {string}
   */
  content: function () {
    if (this.get('someMetricsNA')) {
      return Em.I18n.t('services.service.summary.notAvailable');
    }
    return this.get('dataNodesLive') + "/" + this.get('model.dataNodesTotal');
  }.property('model.dataNodesTotal', 'dataNodesLive', 'someMetricsNA'),

  /**
   * @type {boolean}
   */
  someMetricsNA: function () {
    return Em.isNone(this.get('model.liveDataNodes')) || Em.isNone(this.get('model.dataNodesTotal')) || this.get('model.metricsNotAvailable');
  }.property('model.liveDataNodes.[]', 'model.dataNodesTotal.[]', 'model.metricsNotAvailable'),

  /**
   * @type {string}
   */
  hintInfo: function () {
    var maxTmp = parseFloat(this.get('maxValue'));
    return Em.I18n.t('dashboard.widgets.hintInfo.hint1').format(maxTmp);
  }.property('maxValue')

});
