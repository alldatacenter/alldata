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
  var _key = 'model.{0}'.format(key);
  return Em.computed(_key, function () {
    var value = this.get(_key);
    if (Em.isNone(value)) {
      return Em.I18n.t('services.service.summary.notAvailable');
    }
    return value;
  });
}

App.HawqSegmentUpView = App.TextDashboardWidgetView.extend(App.EditableWithLimitWidgetMixin, {

  hiddenInfo: function () {
    return [
      this.get('hawqSegmentsStarted') + ' ' + Em.I18n.t('dashboard.services.components.started'),
      this.get('hawqSegmentsInstalled') + ' ' + Em.I18n.t('dashboard.services.components.stopped'),
      this.get('hawqSegmentsTotal')+ ' ' + Em.I18n.t('dashboard.services.components.total')
    ];
  }.property('hawqSegmentsStarted', 'hawqSegmentsInstalled', 'hawqSegmentsTotal'),

  hiddenInfoClass: "hidden-info-three-line",

  maxValue: 100,

  hawqSegmentsStarted: counterOrNA('hawqSegmentsStarted'),

  hawqSegmentsInstalled: counterOrNA('hawqSegmentsInstalled'),

  hawqSegmentsTotal: counterOrNA('hawqSegmentsTotal'),

  /**
   * @type {?number}
   */
  data: function () {
    if (this.get('someMetricsNA')) {
      return null;
    }
    return (this.get('hawqSegmentsStarted') / this.get('model.hawqSegmentsTotal')).toFixed(2) * 100;
  }.property('model.hawqSegmentsTotal', 'hawqSegmentsStarted', 'someMetricsNA'),

  /**
   * @type {string}
   */
  content: function () {
    if (this.get('someMetricsNA')) {
      return Em.I18n.t('services.service.summary.notAvailable');
    }
    return this.get('hawqSegmentsStarted') + "/" + this.get('model.hawqSegmentsTotal');
  }.property('model.hawqSegmentsTotal', 'hawqSegmentsStarted', 'someMetricsNA'),

  hintInfo: function () {
    var maxTmp = parseFloat(this.get('maxValue'));
    return Em.I18n.t('dashboard.widgets.hintInfo.hint1').format(maxTmp);
  }.property('maxValue'),

  /**
   * @type {boolean}
   */
  someMetricsNA: function() {
    return Em.isNone(this.get('model.hawqSegmentsStarted')) || Em.isNone(this.get('model.hawqSegmentsTotal'));
  }.property('model.hawqSegmentsStarted', 'model.hawqSegmentsTotal')

});
