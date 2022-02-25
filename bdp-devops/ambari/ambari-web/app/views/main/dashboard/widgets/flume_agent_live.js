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

App.FlumeAgentUpView = App.TextDashboardWidgetView.extend(App.EditableWithLimitWidgetMixin, {

  hiddenInfo: function () {
    return [
      this.get('flumeAgentsLive.length') + ' ' + Em.I18n.t('dashboard.services.hdfs.nodes.live'),
      this.get('flumeAgentsDead.length') + ' ' + Em.I18n.t('dashboard.services.hdfs.nodes.dead')
    ];
  }.property('flumeAgentsLive', 'flumeAgentsDead'),

  hiddenInfoClass: "hidden-info-two-line",

  maxValue: 100,

  flumeAgentComponents: Em.computed.filterBy('model.hostComponents', 'componentName', 'FLUME_HANDLER'),

  /**
   * @type {Array}
   */
  flumeAgentsLive: [],

  /**
   * @type {Array}
   */
  flumeAgentsDead: [],

  data: function () {
    if ( !this.get('flumeAgentComponents.length')) {
      return -1;
    }
    return (this.get('flumeAgentsLive').length / this.get('model.hostComponents').filterProperty('componentName', 'FLUME_HANDLER').length).toFixed(2) * 100;
  }.property('model.hostComponents.length', 'flumeAgentsLive'),

  content: Em.computed.concat('/', 'flumeAgentsLive.length', 'flumeAgentComponents.length'),

  statusObserver: function() {
    Em.run.once(this, 'filterStatusOnce');
  }.observes('flumeAgentComponents.@each.workStatus'),

  filterStatusOnce: function() {
    this.set('flumeAgentsLive', this.get('flumeAgentComponents').filterProperty("workStatus", "STARTED"));
    this.set('flumeAgentsDead', this.get('flumeAgentComponents').filterProperty("workStatus", "INSTALLED"));
  },

  hintInfo: function () {
    var maxTmp = parseFloat(this.get('maxValue'));
    return Em.I18n.t('dashboard.widgets.hintInfo.hint1').format(maxTmp);
  }.property('maxValue')

});
