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

var fileUtils = require('utils/file_utils');

var CUSTOM_TIME_INDEX = 8;

App.GraphWidgetView = Em.View.extend(App.WidgetMixin, App.ExportMetricsMixin, {
  templateName: require('templates/common/widget/graph_widget'),

  /**
   *  type of metric query from which the widget is comprised
   */

  metricType: 'TEMPORAL',

  /**
   * common metrics container
   * @type {Array}
   */
  metrics: [],

  /**
   * 3600 sec in 1 hour
   * @const
   */
  TIME_FACTOR: 3600,

  /**
   * custom time range, set when graph opened in popup
   * @type {number|null}
   */
  customTimeRange: null,

  /**
   * value in seconds
   * @type {number}
   */
  timeRange: function () {
    var timeRange = parseInt(this.get('content.properties.time_range'));
    if (isNaN(timeRange)) {
      //1h - default time range
      timeRange = 1;
    }

    // Custom start and end time is specified by user
    if (this.get('exportTargetView.currentTimeIndex') === CUSTOM_TIME_INDEX) {
      return 0;
    }

    return this.get('customTimeRange') || timeRange * this.get('TIME_FACTOR');
  }.property('content.properties.time_range', 'customTimeRange'),

  /**
   * value in ms
   * @type {number}
   */
  timeStep: 15,

  /**
   * @type {Array}
   */
  data: [],

  /**
   * time range index for graph
   * @type {number}
   */
  timeIndex: 0,

  /**
   * custom start time for graph
   * @type {number|null}
   */
  startTime: null,

  /**
   * custom end time for graph
   * @type {number|null}
   */
  endTime: null,

  /**
   * graph time range duration in seconds
   * @type {number|null}
   */
  graphSeconds: null,

  /**
   * time range duration as string
   * @type {string|null}
   */
  durationFormatted: null,

  exportTargetView: Em.computed.alias('childViews.lastObject'),

  drawWidget: function () {
    if (this.get('isLoaded')) {
      this.set('data', this.calculateValues());
    }
  },

  /**
   * calculate series datasets for graph widgets
   */
  calculateValues: function () {
    var metrics = this.get('metrics');
    var seriesData = [];
    if (this.get('content.values')) {
      this.get('content.values').forEach(function (value) {
        var expression = this.extractExpressions(value)[0];
        var computedData;
        var datasetKey;

        if (expression) {
          datasetKey = value.value.match(this.get('EXPRESSION_REGEX'))[0];
          computedData = this.computeExpression(expression, metrics)[datasetKey];
          //exclude empty datasets
          if (computedData.length > 0) {
            seriesData.push({
              name: value.name,
              data: computedData
            });
          }
        }
      }, this);
    }
    return seriesData;
  },

  /**
   * compute expression
   *
   * @param {string} expression
   * @param {object} metrics
   * @returns {object}
   */
  computeExpression: function (expression, metrics) {
    var validExpression = true,
      value = [],
      dataLinks = {},
      dataLength = -1,
      beforeCompute,
      result = {},
      isDataCorrupted = false,
      isPointNull = false;

    //replace values with metrics data
    expression.match(this.get('VALUE_NAME_REGEX')).forEach(function (match) {
      if (isNaN(match)) {
        if (metrics.someProperty('name', match)) {
          dataLinks[match] = metrics.findProperty('name', match).data;
          if (!isDataCorrupted) {
            isDataCorrupted = (dataLength !== -1 && dataLength !== dataLinks[match].length);
          }
          dataLength = (dataLinks[match].length > dataLength) ? dataLinks[match].length : dataLength;
        } else {
          validExpression = false;
          console.warn('Metrics with name "' + match + '" not found to compute expression');
        }
      }
    });

    if (validExpression) {
      if (isDataCorrupted) {
        this.adjustData(dataLinks, dataLength);
      }
      for (var i = 0, timestamp; i < dataLength; i++) {
        isPointNull = false;
        beforeCompute = expression.replace(this.get('VALUE_NAME_REGEX'), function (match) {
          if (isNaN(match)) {
            timestamp = dataLinks[match][i][1];
            isPointNull = (isPointNull) ? true : (Em.isNone(dataLinks[match][i][0]));
            return dataLinks[match][i][0];
          } else {
            return match;
          }
        });
        var dataLinkPointValue = isPointNull ? null : Number(window.eval(beforeCompute));
        // expression resulting into `0/0` will produce NaN Object which is not a valid series data value for RickShaw graphs
        if (isNaN(dataLinkPointValue)) {
          dataLinkPointValue = 0;
        }
        value.push([dataLinkPointValue, timestamp]);
      }
    }

    result['${' + expression + '}'] = value;
    return result;
  },

  /**
   *  add missing points, with zero value, to series
   *
   * @param {object} dataLinks
   * @param {number} length
   */
  adjustData: function(dataLinks, length) {
    //series with full data taken as original
    var original = [];
    var substituteValue = null;

    for (var i in dataLinks) {
      if (dataLinks[i].length === length) {
        original = dataLinks[i];
        break;
      }
    }
    original.forEach(function(point, index) {
      for (var i in dataLinks) {
        if (!dataLinks[i][index] || dataLinks[i][index][1] !== point[1]) {
          dataLinks[i].splice(index, 0, [substituteValue, point[1]]);
        }
      }
    }, this);
  },

  /**
   * add time properties
   * @param {Array} metricPaths
   * @returns {Array} result
   */
  addTimeProperties: function (metricPaths) {
    var toSeconds,
      fromSeconds,
      step = this.get('timeStep'),
      timeRange = this.get('timeRange'),
      result = [],
      targetView = this.get('exportTargetView.isPopup') ? this.get('exportTargetView') : this.get('parentView');

    //if view destroyed then no metrics should be asked
    if (Em.isNone(targetView)) return result;

    if (timeRange === 0 &&
      !Em.isNone(targetView.get('customStartTime')) &&
      !Em.isNone(targetView.get('customEndTime'))) {
      // Custom start/end time is specified by user
      toSeconds = targetView.get('customEndTime') / 1000;
      fromSeconds = targetView.get('customStartTime') / 1000;
    } else {
      // Preset time range is specified by user
      toSeconds = Math.round(App.dateTime() / 1000);
      fromSeconds = toSeconds - timeRange;
    }

    metricPaths.forEach(function (metricPath) {
      result.push(metricPath + '[' + fromSeconds + ',' + toSeconds + ',' + step + ']');
    }, this);

    return result;
  },

  /**
   * @type {Em.View}
   * @class
   */
  graphView: App.ChartLinearTimeView.extend({

    noTitleUnderGraph: true,
    inWidget: true,
    description: Em.computed.alias('parentView.content.description'),
    isPreview: Em.computed.alias('parentView.isPreview'),
    displayUnit: Em.computed.alias('parentView.content.properties.display_unit'),
    setYAxisFormatter: function () {
      var displayUnit = this.get('displayUnit');
      if (displayUnit) {
        this.set('yAxisFormatter', function (value) {
          return App.ChartLinearTimeView.DisplayUnitFormatter(value, displayUnit);
        });
      }
    }.observes('displayUnit'),

    /**
     * set custom time range for graph widget
     */
    setTimeRange: function () {
      if (this.get('isPopup')) {
        if (this.get('currentTimeIndex') === CUSTOM_TIME_INDEX) {
          // Custom start and end time is specified by user
          this.get('parentView').propertyDidChange('customTimeRange');
        } else {
          // Preset time range is specified by user
          this.set('parentView.customTimeRange', this.get('timeUnitSeconds'));
        }
      } else {
        this.set('parentView.customTimeRange', null);
      }
    }.observes('isPopup', 'timeUnitSeconds'),

    /**
     * graph height
     * @type {number}
     */
    height: 95,

    /**
     * @type {string}
     */
    id: function () {
      return 'widget_'+ this.get('parentView.content.id') + '_graph';
    }.property('parentView.content.id'),

    /**
     * @type {string}
     */
    renderer: function () {
      return this.get('parentView.content.properties.graph_type') === 'STACK' ? 'area' : 'line';
    }.property('parentView.content.properties.graph_type'),

    title: Em.computed.alias('parentView.content.widgetName'),

    transformToSeries: function (seriesData) {
      var seriesArray = [];

      seriesData.forEach(function (_series) {
        seriesArray.push(this.transformData(_series.data, _series.name));
      }, this);
      return seriesArray;
    },

    loadData: function () {
      var self = this;
      Em.run.next(function () {
        self._refreshGraph(self.get('parentView.data'), self.get('parentView'));
      });
    },

    didInsertElement: function () {
      var self = this;
      this.$().closest('.graph-widget').on('mouseleave', function () {
        self.set('parentView.isExportMenuHidden', true);
      });
      this.setYAxisFormatter();
      if (!arguments.length || this.get('parentView.data.length')) {
        this.loadData();
      }
      Em.run.next(function () {
        if (self.get('isPreview')) {
          App.tooltip(self.$("[rel='ZoomInTooltip']"), 'disable');
        } else {
          App.tooltip(self.$("[rel='ZoomInTooltip']"), {
            placement: 'left',
            template: '<div class="tooltip"><div class="tooltip-arrow"></div><div class="tooltip-inner graph-tooltip"></div></div>'
          });
        }
      });
    }.observes('parentView.data')
  }),

  exportGraphData: function (event) {
    this.set('isExportMenuHidden', true);
    var data,
      isCSV = !!event.context,
      fileType = isCSV ? 'csv' : 'json',
      fileName = 'data.' + fileType,
      metrics = this.get('data'),
      hasData = Em.isArray(metrics) && metrics.some(function (item) {
        return Em.isArray(item.data);
      });
    if (hasData) {
      data = isCSV ? this.prepareCSV(metrics) : JSON.stringify(metrics, this.jsonReplacer(), 4);
      fileUtils.downloadTextFile(data, fileType, fileName);
    } else {
      App.showAlertPopup(Em.I18n.t('graphs.noData.title'), Em.I18n.t('graphs.noData.tooltip.title'));
    }
  }
});
