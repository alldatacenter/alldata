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
 * This is a view for showing host disk usage
 * 
 * @extends App.ChartLinearTimeView
 * @extends Ember.Object
 * @extends Ember.View
 */
App.ChartHostMetricsDisk = App.ChartLinearTimeView.extend({
  id: "host-metrics-disk",
  title: Em.I18n.t('hosts.host.metrics.disk'),
  displayUnit: 'B',
  renderer: 'line',

  ajaxIndex: 'host.metrics.disk',

  loadGroup: {
    name: 'host.metrics.aggregated',
    fields: ['metrics/disk/disk_total', 'metrics/disk/disk_free']
  },

  seriesTemplate: {
    path: 'metrics.disk',
    displayName: function (name) {
      var displayNameMap = {
        disk_total: Em.I18n.t('hosts.host.metrics.disk.displayNames.disk_total'),
        disk_free: Em.I18n.t('hosts.host.metrics.disk.displayNames.disk_free')
      };
      return displayNameMap[name];
    },
    factor: Math.pow(2, 30)
  },

  getData: function (jsonData) {
    var partMaxUsed = Em.get(jsonData, 'metrics.part_max_used');
    if (!Em.isNone(partMaxUsed) && Em.get(jsonData, this.get('seriesTemplate.path'))) {
      Em.set(jsonData, this.get('seriesTemplate.path') + '.part_max_used', partMaxUsed);
    }
    return this._super(jsonData);
  }
});