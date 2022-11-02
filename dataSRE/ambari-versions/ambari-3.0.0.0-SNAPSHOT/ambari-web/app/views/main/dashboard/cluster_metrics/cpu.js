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
 * This is a view for showing cluster CPU metrics
 * 
 * @extends App.ChartLinearTimeView
 * @extends Ember.Object
 * @extends Ember.View
 */
App.ChartClusterMetricsCPU = App.ChartLinearTimeView.extend({
  id: "cluster-metrics-cpu",

  ajaxIndex: 'dashboard.cluster_metrics.cpu',

  title: Em.I18n.t('dashboard.clusterMetrics.cpu'),
  displayUnit: '%',
  isTimePagingDisable: false,
  seriesTemplate: {
    path: 'metrics.cpu'
  },

  getData: function (jsonData) {
    var dataArray = [],
      idle = null,
      data = Em.get(jsonData, this.get('seriesTemplate.path'));
    if (data) {
      Object.keys(data).forEach(function (name) {
        var seriesData = data[name];
        if (seriesData) {
          var s = {
            name: name,
            data: seriesData
          };
          if (name.contains('Idle')) {
            //CPU idle metric should be the last in series array
            idle = s;
            return;
          }
          dataArray.push(s);
        }
      });
      if (idle) {
        dataArray.push(idle);
      }
    }
    return dataArray;
  }
});