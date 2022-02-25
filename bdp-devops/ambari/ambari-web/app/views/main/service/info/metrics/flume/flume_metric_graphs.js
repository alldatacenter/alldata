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
require('views/main/service/service');

App.MainServiceInfoFlumeGraphsView = App.MainServiceInfoSummaryMetricGraphsView.extend({

  serviceMetricGraphs: [],

  loadMetrics: function () {
    var viewData = this.get('viewData');
    if (viewData != null) {
      var metricType = viewData.metricType;
      var hostName = viewData.agent.get('hostName');
      return App.ajax.send({
        'name': 'host.host_component.flume.metrics',
        'sender': this,
        'success': 'onLoadMetricsSuccess',
        'data': {
          hostName: hostName,
          flumeComponent: metricType
        }
      });
    } else {
      return $.Deferred().reject().promise();
    }
  }.observes('viewData', 'metricType'),

  onLoadMetricsSuccess: function (data) {
    var graphRows = [];
    var viewData = this.get('viewData');
    var metricType = viewData.metricType;
    var hostName = viewData.agent.get('hostName');
    var metricNames = {};
    var metricItems = [];
    if (data != null && data.metrics != null && data.metrics.flume != null && data.metrics.flume.flume != null && data.metrics.flume.flume[metricType] != null) {
      for ( var name in data.metrics.flume.flume[metricType]) {
        if (data.metrics.flume.flume[metricType].hasOwnProperty(name)) {
          for ( var metricName in data.metrics.flume.flume[metricType][name]) {
            if (data.metrics.flume.flume[metricType][name].hasOwnProperty(metricName)) {
              metricNames[metricName] = name;
            }
          }
          metricItems.push(name);
        }
      }
    }
    // Now that we have collected all metric names, we create
    // views for each of them and store them 4 in a row.
    graphRows.push([]);
    var graphs = graphRows[0];
    for (var metricName in metricNames) {
      if (graphs.length > 1) {
        graphRows.push([]);
        graphs = graphRows[graphRows.length - 1];
      }
      graphs.push(App.ChartServiceFlumeMetricGraph.extend({
        metricType: metricType,
        metricName: metricName,
        hostName: hostName,
        metricItems: metricItems
      }));
    }
    this.set('serviceMetricGraphs', graphRows);
  },

  didInsertElement: function () {
    this.loadMetrics();
  }

});
