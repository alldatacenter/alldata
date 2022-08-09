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
 * This is a view for showing Host CPU metrics
 * 
 * @extends App.ChartLinearTimeView
 * @extends Ember.Object
 * @extends Ember.View
 */
App.ChartHostMetricsCPU = App.ChartLinearTimeView.extend({
  id: "host-metrics-cpu",
  title: Em.I18n.t('hosts.host.metrics.cpu'),
  displayUnit: '%',

  ajaxIndex: 'host.metrics.cpu',

  loadGroup: {
    name: 'host.metrics.aggregated',
    fields: ['metrics/cpu/cpu_user', 'metrics/cpu/cpu_wio', 'metrics/cpu/cpu_nice', 'metrics/cpu/cpu_aidle', 'metrics/cpu/cpu_system', 'metrics/cpu/cpu_idle']
  },

  seriesTemplate: {
    path: 'metrics.cpu',
    displayName: function (name) {
      var displayNameMap = {
        cpu_wio: Em.I18n.t('hosts.host.metrics.cpu.displayNames.cpu_wio'),
        cpu_idle: Em.I18n.t('hosts.host.metrics.cpu.displayNames.cpu_idle'),
        cpu_nice: Em.I18n.t('hosts.host.metrics.cpu.displayNames.cpu_nice'),
        cpu_aidle: Em.I18n.t('hosts.host.metrics.cpu.displayNames.cpu_aidle'),
        cpu_system: Em.I18n.t('hosts.host.metrics.cpu.displayNames.cpu_system'),
        cpu_user: Em.I18n.t('hosts.host.metrics.cpu.displayNames.cpu_user')
      };
      return displayNameMap[name];
    }
  },

  getData: function (jsonData) {
    var dataArray = [],
      cpu_idle,
      template = this.get('seriesTemplate'),
      data = Em.get(jsonData, template.path);
    if (data) {
      for (var name in data) {
        var displayName = template.displayName(name),
          seriesData = data[name];
        if (seriesData) {
          var s = {
            name: displayName,
            data: seriesData
          };
          if (Em.I18n.t('hosts.host.metrics.cpu.displayNames.cpu_idle') == displayName) {
            cpu_idle = s;
          } else {
            dataArray.push(s);
          }
        }
      }
      dataArray.push(cpu_idle);
    }
    return dataArray;
  }
});