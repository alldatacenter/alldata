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

/**
 * @class
 * 
 * This is a view for showing host memory metrics
 * 
 * @extends App.ChartLinearTimeView
 * @extends Ember.Object
 * @extends Ember.View
 */
App.ChartHostMetricsMemory = App.ChartLinearTimeView.extend({
  id: "host-metrics-memory",
  title: Em.I18n.t('hosts.host.metrics.memory'),
  displayUnit: 'B',
  renderer: 'line',

  ajaxIndex: 'host.metrics.memory',

  loadGroup: {
    name: 'host.metrics.aggregated',
    fields: [
      'metrics/memory/swap_free',
      'metrics/memory/mem_shared',
      'metrics/memory/mem_free',
      'metrics/memory/mem_cached',
      'metrics/memory/mem_buffers'
    ]
  },

  seriesTemplate: {
    path: 'metrics.memory',
    displayName: function (name) {
      var displayNameMap = {
        mem_shared: Em.I18n.t('hosts.host.metrics.memory.displayNames.mem_shared'),
        swap_free: Em.I18n.t('hosts.host.metrics.memory.displayNames.swap_free'),
        mem_buffers: Em.I18n.t('hosts.host.metrics.memory.displayNames.mem_buffers'),
        mem_free: Em.I18n.t('hosts.host.metrics.memory.displayNames.mem_free'),
        mem_cached: Em.I18n.t('hosts.host.metrics.memory.displayNames.mem_cached')
      };
      return displayNameMap[name];
    },
    factor: Math.pow(2, 10)
  }
});