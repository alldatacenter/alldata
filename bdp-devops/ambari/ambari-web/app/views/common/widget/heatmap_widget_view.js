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

const App = require('app');
const numberUtils = require('utils/number_utils');

App.HeatmapWidgetView = Em.View.extend(App.WidgetMixin, {
  templateName: require('templates/common/widget/heatmap_widget'),

  /**
   * common metrics container
   * @type {Array}
   */
  metrics: [],

  /**
   *  racks container bound in the template
   *  @type {Array}
   */
  racks: [],

  /**
   * @type {$.ajax|null}
   * @default null
   */
  activeRequest: null,

  onMetricsLoaded: function () {
    if (!this.get('isLoaded')) {
      this.set('controller.inputMaximum', this.get('content.properties.max_limit'));
    }
    this._super();
  },

  willDestroyElement: function () {
    if ($.isPlainObject(this.get('activeRequest'))) {
      this.get('activeRequest').abort();
      this.set('activeRequest', null);
    }
  },

  getHostComponentsMetrics: function (request) {
    var ajax = this._super(request);
    this.set('activeRequest', ajax);
    return ajax;
  },

  getHostsMetrics: function (request) {
    var ajax = this._super(request);
    this.set('activeRequest', ajax);
    return ajax;
  },

  /**
   * skip metrics loading if AMBARI METRICS service is not started
   */
  loadMetrics: function () {
    if (App.Service.find('AMBARI_METRICS').get('isStarted')) {
      this._super();
    } else {
      this.onMetricsLoaded();
    }
  },

  /**
   * draw widget
   */
  drawWidget: function () {
    if (this.get('isLoaded')) {
      var hostToValueMap = this.calculateValues();
      var hostNames = [];
      if (this.get('racks').everyProperty('isLoaded', true)) {
        this.get('racks').forEach(function (rack) {
          hostNames = hostNames.concat(rack.hosts.mapProperty('hostName'));
        });
      }

      var metricObject = App.MainChartHeatmapMetric.create({
        name: this.get('content.widgetName'),
        units: this.get('content.properties.display_unit'),
        maximumValue: this.get('controller.inputMaximum'),
        hostNames: hostNames,
        hostToValueMap: hostToValueMap
      });

      this.set('controller.selectedMetric', metricObject);
      App.loadTimer.finish('Heatmaps Page');
      App.loadTimer.finish('Service Heatmaps Page');
    }
  }.observes('racks.@each.isLoaded'),

  /**
   * calculate value for heatmap widgets
   * @returns {Object}
   */
  calculateValues: function () {
    return this.computeExpression(this.extractExpressions(this.get('content.values')[0]), this.get('metrics'));
  },


  /**
   * compute expression
   * @param {Array} expressions
   * @param {Array} metrics
   * @returns {object}
   */
  computeExpression: function (expressions, metrics) {
    var hostToValueMap = {};
    var hostNames = metrics.mapProperty('hostName');
    var metricsMap = {};

    metrics.forEach(function (_metric) {
      this.convertDataWhenMB(_metric);
      metricsMap[_metric.name + "_" + _metric.hostName] = _metric;
    }, this);

    hostNames.forEach(function (_hostName) {
      expressions.forEach(function (_expression) {
        var validExpression = true;

        //replace values with metrics data
        var beforeCompute = _expression.replace(this.get('VALUE_NAME_REGEX'), function (match) {
          var _metric;
          if (window.isNaN(match)) {
            _metric = metricsMap[match + "_" + _hostName];
            if (_metric) {
              return _metric.data;
            } else {
              validExpression = false;
              console.warn('Metrics with name "' + match + '" not found to compute expression');
            }
          } else {
            return match;
          }
        });

        if (validExpression && this.get('MATH_EXPRESSION_REGEX').test(beforeCompute)) {
          hostToValueMap[_hostName] = Number(window.eval(beforeCompute)).toString();
        } else if (beforeCompute.contains('undefined')) {
          // Data not available
          hostToValueMap[_hostName] = undefined;
        } else {
          console.error('Value for metric is not correct mathematical expression: ' + beforeCompute);
        }
      }, this);
    }, this);

    return hostToValueMap;
  },

  /**
   * If metric has value in MB convert it to bytes
   * @param _metric
   */
  convertDataWhenMB: function(_metric) {
    if (_metric.metric_path.endsWith('M') && !Em.isNone(_metric.data) && isFinite(_metric.data)) {
      _metric.originalData = _metric.data;
      _metric.data = Math.round(_metric.data * numberUtils.BYTES_IN_MB);
    }
  }
});
