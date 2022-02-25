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
    if (Em.isNone(this.get('model.' + key)) || this.get('model.metricsNotAvailable')) {
      return Em.I18n.t('services.service.summary.notAvailable');
    }
    return this.get(_key);
  });
}

App.YarnContainersView = App.TextDashboardWidgetView.extend({

  hiddenInfo: function () {
    return [
      this.get('containersAllocated') + ' ' + Em.I18n.t('dashboard.services.yarn.containers.allocated'),
      this.get('containersPending') + ' ' + Em.I18n.t('dashboard.services.yarn.containers.pending'),
      this.get('containersReserved') + ' ' + Em.I18n.t('dashboard.services.yarn.containers.reserved')
    ];
  }.property('containersAllocated', 'containersPending', 'containersReserved'),

  hiddenInfoClass: "hidden-info-three-line",

  maxValue: 'infinity',

  isEditable: false,

  containersAllocated: counterOrNA('containersAllocated'),

  containersPending: counterOrNA('containersPending'),

  containersReserved: counterOrNA('containersReserved'),

  isRed: Em.computed.alias('someMetricsNA'),

  isOrange: false,

  isGreen: Em.computed.equal('someMetricsNA', false),

  /**
   * @type {string}
   */
  content: function () {
    if (this.get('someMetricsNA')) {
      return Em.I18n.t('services.service.summary.notAvailable');
    }
    return this.get('containersAllocated') + "/" + this.get('containersPending') + "/" + this.get('containersReserved');
  }.property('containersAllocated', 'containersPending', 'containersReserved'),

  /**
   * @type {boolean}
   */
  someMetricsNA: function () {
    return this.get('containersAllocated') === Em.I18n.t('services.service.summary.notAvailable') ||
      this.get('containersPending') === Em.I18n.t('services.service.summary.notAvailable') ||
      this.get('containersReserved') === Em.I18n.t('services.service.summary.notAvailable');
  }.property('containersAllocated', 'containersPending', 'containersReserved'),

  /**
   * @type {string}
   */
  hintInfo: function () {
    var maxTmp = parseFloat(this.get('maxValue'));
    return Em.I18n.t('dashboard.widgets.hintInfo.hint1').format(maxTmp);
  }.property('maxValue')
});
