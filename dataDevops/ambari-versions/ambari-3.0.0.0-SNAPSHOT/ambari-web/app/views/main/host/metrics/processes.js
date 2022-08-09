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
 * This is a view for showing host process counts
 * 
 * @extends App.ChartLinearTimeView
 * @extends Ember.Object
 * @extends Ember.View
 */
App.ChartHostMetricsProcesses = App.ChartLinearTimeView.extend({
  id: "host-metrics-processes",
  title: Em.I18n.t('hosts.host.metrics.processes'),
  renderer: 'line',

  ajaxIndex: 'host.metrics.processes',

  loadGroup: {
    name: 'host.metrics.aggregated',
    fields: ['metrics/process/proc_total', 'metrics/process/proc_run']
  },

  seriesTemplate: {
    path: 'metrics.process',
    displayName: function (name) {
      var displayNameMap = {
        proc_total: Em.I18n.t('hosts.host.metrics.processes.displayNames.proc_total'),
        proc_run: Em.I18n.t('hosts.host.metrics.processes.displayNames.proc_run')
      };
      return displayNameMap[name];
    }
  }
});