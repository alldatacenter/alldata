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

App.SuperVisorUpView = App.TextDashboardWidgetView.extend(App.EditableWithLimitWidgetMixin, {

  hiddenInfo: function () {
    return [
      this.get('superVisorsLive') + ' ' + Em.I18n.t('dashboard.services.hdfs.nodes.live'),
      this.get('superVisorsDead') + ' ' + Em.I18n.t('dashboard.services.hdfs.nodes.dead')
    ];
  }.property('superVisorsLive', 'superVisorsDead'),

  hiddenInfoClass: "hidden-info-two-line",

  maxValue: 100,

  superVisorsLive: Em.computed.alias('model.superVisorsStarted'),

  superVisorsDead: Em.computed.alias('model.superVisorsInstalled'),

  superVisorsTotal: Em.computed.alias('model.superVisorsTotal'),

  data: function () {
    if ( !this.get('superVisorsTotal') || Em.isNone(this.get('superVisorsLive'))) {
      return -1;
    }
    return (this.get('superVisorsLive') / this.get('superVisorsTotal')).toFixed(2) * 100;
  }.property('superVisorsTotal', 'superVisorsLive'),

  content: function () {
    if (!Em.isNone(this.get('superVisorsTotal')) && !Em.isNone(this.get('superVisorsLive'))) {
      return this.get('superVisorsLive') + "/" + this.get('superVisorsTotal');
    }
    return Em.I18n.t('services.service.summary.notAvailable');
  }.property('superVisorsLive', 'superVisorsTotal'),

  hintInfo: function () {
    return Em.I18n.t('dashboard.widgets.hintInfo.hint1').format(parseFloat(this.get('maxValue')));
  }.property('maxValue')
});
