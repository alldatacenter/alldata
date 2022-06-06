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
var string_utils = require('utils/string_utils');
var dateUtils = require('utils/date/date');
var chartUtils = require('utils/chart_utils');

/**
 * @class
 *
 * This is a view which GETs data from a URL and shows it as a time based line
 * graph. Time is shown on the X axis with data series shown on Y axis. It
 * optionally also has the ability to auto refresh itself over a given time
 * interval.
 *
 * This is an abstract class which is meant to be extended.
 *
 * Extending classes should override the following:
 * <ul>
 * <li>url - from where the data can be retrieved
 * <li>title - Title to be displayed when showing the chart
 * <li>id - which uniquely identifies this chart in any page
 * <li>seriesTemplate - template used by getData method to process server data
 * </ul>
 *
 * Extending classes could optionally override the following:
 * <ul>
 * <li>#getData(jsonData) - function to map server data into series format
 * ready for export to graph and JSON formats
 * <li>#colorForSeries(series) - function to get custom colors per series
 * </ul>
 *
 * @extends Ember.Object
 * @extends Ember.View
 */
App.ChartLinearTimeView = Ember.View.extend(App.ExportMetricsMixin, {
  templateName: require('templates/main/charts/linear_time'),

  /**
   * The URL from which data can be retrieved.
   *
   * This property must be provided for the graph to show properly.
   *
   * @type String
   * @default null
   */
  url: null,

  /**
   * A unique ID for this chart.
   *
   * @type String
   * @default null
   */
  id: null,

  /**
   * Title to be shown under the chart.
   *
   * @type String
   * @default null
   */
  title: null,

  /**
   * @private
   *
   * @type Rickshaw.Graph
   * @default null
   */
  _graph: null,

  /**
   * Array of classnames for each series (in widget)
   * @type Rickshaw.Graph
   */
  _popupGraph: null,

  /**
   * Array of classnames for each series
   * @type Array
   */
  _seriesProperties: null,

  /**
   * Array of classnames for each series (in widget)
   * @type Array
   */
  _seriesPropertiesWidget: null,


  /**
   * Renderer type
   * See <code>Rickshaw.Graph.Renderer</code> for more info
   * @type String
   */
  renderer: 'area',

  /**
   * Suffix used in DOM-elements selectors
   * @type String
   */
  popupSuffix: '-popup',

  /**
   * Is popup for current graph open
   * @type Boolean
   */
  isPopup: false,

  /**
   * Is graph ready
   * @type Boolean
   */
  isReady: false,

  /**
   * Is popup-graph ready
   * @type Boolean
   */
  isPopupReady: false,

  /**
   * Is data for graph available
   * @type Boolean
   */
  hasData: true,

  /**
   * chart height
   * @type {number}
   * @default 150
   */
  height: 150,

  /**
   * @type {string}
   * @default null
   */
  displayUnit: null,

  /**
   * Object containing information to get metrics data from API response
   * Supported properties:
   * <ul>
   * <li>path - exact path to metrics data in response JSON (required)
   * <li>displayName(name, hostName) - returns display name for metrics
   * depending on property name in response JSON and host name for Flume Agents
   * <li>factor - number that metrics values should be multiplied by
   * <li>flumePropertyName - property name to access certain Flume metrics
   * </ul>
   * @type {Object}
   */
  seriesTemplate: null,

  /**
   * Incomplete metrics requests
   * @type {array}
   * @default []
   */
  runningRequests: [],

  /**
   * Incomplete metrics requests for detailed view
   * @type {array}
   * @default []
   */
  runningPopupRequests: [],

  _containerSelector: Em.computed.format('#{0}-container', 'id'),

  _popupSelector: Em.computed.concat('', '_containerSelector', 'popupSuffix'),

  /**
   * @type {boolean}
   */
  isRequestRunning: function() {
    var requestsArrayName = this.get('isPopup') ? 'runningPopupRequests' : 'runningRequests';
    return this.get(requestsArrayName).mapProperty('ajaxIndex').contains(this.get('ajaxIndex'));
  }.property('runningPopupRequests', 'runningRequests'),

  didInsertElement: function () {
    var self = this;
    this.setYAxisFormatter();
    this.loadData();
    this.registerGraph();
    this.$().parent().on('mouseleave', function () {
      self.set('isExportMenuHidden', true);
    });
    App.tooltip(this.$("[rel='ZoomInTooltip']"), {
      placement: 'left',
      template: '<div class="tooltip"><div class="tooltip-arrow"></div><div class="tooltip-inner graph-tooltip"></div></div>'
    });
  },

  setCurrentTimeIndex: function () {
    this.set('currentTimeIndex', this.get('parentView.currentTimeRangeIndex'));
  }.observes('parentView.currentTimeRangeIndex'),

  /**
   * Maps server data into series format ready for export to graph and JSON formats
   * @param jsonData
   * @returns {Array}
   */
  getData: function (jsonData) {
    var dataArray = [],
      template = this.get('seriesTemplate'),
      data = Em.get(jsonData, template.path);
    if (data) {
      for (var name in data) {
        var currentData = data[name];
        if (currentData) {
          if (Array.isArray(currentData)) {
            currentData = currentData.slice(0);
          }
          var factor = template.factor,
            displayName = template.displayName ? template.displayName(name) : name;
          if (!Em.isNone(factor)) {
            var dataLength = currentData.length;
            for (var i = dataLength; i--;) {
              currentData[i] = [currentData[i][0] * factor, currentData[i][1]];
            }
          }
          dataArray.push({
            name: displayName,
            data: currentData
          });
        }
      }
    }
    return dataArray;
  },

  /**
   * Maps server data for certain Flume metrics into series format ready for export
   * to graph and JSON formats
   * @param jsonData
   * @returns {Array}
   */
  getFlumeData: function (jsonData) {
    var dataArray = [];
    if (jsonData && jsonData.host_components) {
      jsonData.host_components.forEach(function (hc) {
        var hostName = hc.HostRoles.host_name,
          host = App.Host.find(hostName),
          template = this.get('template'),
          data = Em.get(hc, template.path);
        if (host && host.get('publicHostName')) {
          hostName = host.get('publicHostName');
        }
        if (data) {
          for (var cname in data) {
            var seriesName = template.displayName ? template.displayName(cname, hostName) : cname,
              seriesData = template.flumePropertyName ? data[cname][template.flumePropertyName] : data[cname];
            if (seriesData) {
              var factor = template.factor;
              if (!Em.isNone(factor)) {
                var dataLength = seriesData.length;
                for (var i = dataLength; i--;) {
                  seriesData[i][0] *= factor;
                }
              }
              dataArray.push({
                name: seriesName,
                data: seriesData
              });
            }
          }
        }
      }, this);
    }
    return dataArray;
  },

  /**
   * Function to map data into graph series
   * @param jsonData
   * @returns {Array}
   */
  transformToSeries: function (jsonData) {
    var seriesArray = [],
      seriesData = this.getData(jsonData),
      dataLength = seriesData.length;
    for (var i = 0; i < dataLength; i++) {
      seriesArray.push(this.transformData(seriesData[i].data, seriesData[i].name));
    }
    return seriesArray;
  },

  setExportTooltip: function () {
    if (this.get('isReady')) {
      Em.run.next(this, function () {
        var icon = this.$('.corner-icon');
        if (icon) {
          icon.on('mouseover', function () {
            $(this).closest("[rel='ZoomInTooltip']").trigger('mouseleave');
          });
          App.tooltip(icon.children('.glyphicon-save'), {
            title: Em.I18n.t('common.export')
          });
        }
      });
    }
  }.observes('isReady'),

  willDestroyElement: function () {
    this.$("[rel='ZoomInTooltip']").tooltip('destroy');
    $(this.get('_containerSelector') + ' li.line').off();
    $(this.get('_popupSelector') + ' li.line').off();
    App.ajax.abortRequests(this.get('runningRequests'));
  },

  registerGraph: function () {
    var graph = {
      name: this.get('title'),
      id: this.get('elementId'),
      popupId: this.get('id')
    };
    App.router.get('updateController.graphs').push(graph);
  },

  loadData: function () {
    var self = this,
      isPopup = this.get('isPopup');
    if (this.get('loadGroup') && !isPopup) {
      return App.ChartLinearTimeView.LoadAggregator.add(this, this.get('loadGroup'));
    } else {
      var requestsArrayName = isPopup ? 'runningPopupRequests' : 'runningRequests',
        request = App.ajax.send({
          name: this.get('ajaxIndex'),
          sender: this,
          data: this.getDataForAjaxRequest(),
          success: 'loadDataSuccessCallback',
          error: 'loadDataErrorCallback',
          callback: function () {
            self.set(requestsArrayName, self.get(requestsArrayName).reject(function (item) {
              return item === request;
            }));
          }
        });
      if (request) request.ajaxIndex = this.get('ajaxIndex');
      this.get(requestsArrayName).push(request);
      return request;
    }
  },

  getDataForAjaxRequest: function () {
    var fromSeconds,
      toSeconds,
      hostName = (this.get('content')) ? this.get('content.hostName') : "",
      YARNService = App.YARNService.find().objectAt(0),
      resourceManager = YARNService ? YARNService.get('resourceManager.hostName') : "";
    if (this.get('currentTimeIndex') === 8 && !Em.isNone(this.get('customStartTime')) && !Em.isNone(this.get('customEndTime'))) {
      // Custom start and end time is specified by user
      toSeconds = this.get('customEndTime') / 1000;
      fromSeconds = this.get('customStartTime') / 1000;
    } else {
      // Preset time range is specified by user
      var timeUnit = this.get('timeUnitSeconds');
      toSeconds = Math.round(App.dateTime() / 1000);
      fromSeconds = toSeconds - timeUnit;
    }
    return {
      toSeconds: toSeconds,
      fromSeconds: fromSeconds,
      stepSeconds: 15,
      hostName: hostName,
      resourceManager: resourceManager
    };
  },

  loadDataSuccessCallback: function (response) {
    this._refreshGraph(response);
  },

  loadDataErrorCallback: function (xhr, textStatus, errorThrown) {
    if (!xhr.isForcedAbort) {
      this.set('isReady', true);
      if (xhr.readyState == 4 && xhr.status) {
        textStatus = xhr.status + " " + textStatus;
      }
      this._showMessage('warn', this.t('graphs.error.title'), this.t('graphs.error.message').format(textStatus, errorThrown));
      this.setProperties({
        hasData: false,
        isExportButtonHidden: true
      });
    }
  },

  /**
   * Shows a yellow warning message in place of the chart.
   *
   * @param type  Can be any of 'warn', 'error', 'info', 'success'
   * @param title Bolded title for the message
   * @param message String representing the message
   * @param tooltip Tooltip content
   * @type: Function
   */
  _showMessage: function (type, title, message, tooltip) {
    var popupSuffix = this.get('isPopup') ? this.get('popupSuffix') : '';
    var chartOverlay = '#' + this.get('id');
    var chartOverlayId = chartOverlay + '-chart' + popupSuffix;
    var chartOverlayY = chartOverlay + '-yaxis' + popupSuffix;
    var chartOverlayX = chartOverlay + '-xaxis' + popupSuffix;
    var chartOverlayLegend = chartOverlay + '-legend' + popupSuffix;
    var chartOverlayTimeline = chartOverlay + '-timeline' + popupSuffix;
    var tooltipTitle = tooltip ? tooltip : Em.I18n.t('graphs.tooltip.title');
    var chartContent = '';

    var typeClass;
    switch (type) {
      case 'error':
        typeClass = 'alert-danger';
        break;
      case 'success':
        typeClass = 'alert-success';
        break;
      case 'info':
        typeClass = 'alert-info';
        break;
      default:
        typeClass = 'alert-warning';
        break;
    }
    $(chartOverlayId + ', ' + chartOverlayY + ', ' + chartOverlayX + ', ' + chartOverlayLegend + ', ' + chartOverlayTimeline).html('');
    chartContent += '<div class=\"alert ' + typeClass + '\">';
    if (title) {
      chartContent += '<strong>' + title + '</strong> ';
    }
    chartContent += message + '</div>';
    $(chartOverlayId).append(chartContent);
    $(chartOverlayId).parent().attr('data-original-title', tooltipTitle);
  },

  /**
   * Transforms the JSON data retrieved from the server into the series
   * format that Rickshaw.Graph understands.
   *
   * The series object is generally in the following format: [ { name :
   * "Series 1", data : [ { x : 0, y : 0 }, { x : 1, y : 1 } ] } ]
   *
   * Extending classes should override this method.
   *
   * @param seriesData
   *          Data retrieved from the server
   * @param displayName
   *          Graph title
   * @type: Function
   *
   */
  transformData: function (seriesData, displayName) {
    if (!Em.isNone(seriesData)) {
      // Is it a string?
      if ("string" == typeof seriesData) {
        seriesData = JSON.parse(seriesData);
      }
      // Is it a number?
      if ("number" == typeof seriesData) {
        // Same number applies to all time.
        var number = seriesData;
        seriesData = [];
        seriesData.push([number, App.dateTime() - (60 * 60)]);
        seriesData.push([number, App.dateTime()]);
      }
      // We have valid data
      var series = {};
      series.name = displayName;
      series.data = [];
      var timeDiff = App.dateTimeWithTimeZone(seriesData[0][1] * 1000) / 1000 - seriesData[0][1];
      for (var index = 0; index < seriesData.length; index++) {
        series.data.push({
          x: seriesData[index][1] + timeDiff,
          y: seriesData[index][0]
        });
      }
      return series;
    }
    return null;
  },

  /**
   * Provides the formatter to use in displaying Y axis.
   *
   * By default, uses the App.ChartLinearTimeView.DefaultFormatter which shows 10K,
   * 300M etc.
   *
   * @type Function
   */
  yAxisFormatter: function (y) {
    return App.ChartLinearTimeView.DefaultFormatter(y);
  },

  /**
   * Sets the formatter to use in displaying Y axis depending on graph unit.
   *
   * @type Function
   */
  setYAxisFormatter: function () {
    var method,
      formatterMap = {
        '%': 'PercentageFormatter',
        '/s': 'CreateRateFormatter',
        'B': 'BytesFormatter',
        'ms': 'TimeElapsedFormatter'
      },
      methodName = formatterMap[this.get('displayUnit')];
    if (methodName) {
      method = (methodName == 'CreateRateFormatter') ?
        App.ChartLinearTimeView.CreateRateFormatter('', App.ChartLinearTimeView.DefaultFormatter) :
        App.ChartLinearTimeView[methodName];
      this.set('yAxisFormatter', method);
    }
  },

  /**
   * Provides the color (in any HTML color format) to use for a particular
   * series.
   * May be redefined in child views
   *
   * @param series
   *          Series for which color is being requested
   * @return color String. Returning null allows this chart to pick a color
   *         from palette.
   * @default null
   * @type Function
   */
  colorForSeries: function (series) {
    return null;
  },

  /**
   * Check whether seriesData is correct data for chart drawing
   * @param {Array} seriesData
   * @return {Boolean}
   */
  checkSeries: function (seriesData) {
    if (!seriesData || !seriesData.length) {
      return false;
    }
    var result = true;
    seriesData.forEach(function (item) {
      if (Em.isNone(Em.get(item, 'data.0.x'))) {
        result = false;
      }
    });
    return result;
  },

  /**
   * @private
   *
   * Refreshes the graph with the latest JSON data.
   *
   * @type Function
   */
  _refreshGraph: function (jsonData, graphView) {
    if (this.get('isDestroyed')) {
      return;
    }
    console.time('_refreshGraph');
    var seriesData = this.transformToSeries(jsonData);

    //if graph opened as modal popup
    var popup_path = $(this.get('_popupSelector'));
    var graph_container = $(this.get('_containerSelector'));
    var seconds = this.get('parentView.graphSeconds');
    var container;
    if (!Em.isNone(seconds)) {
      this.set('timeUnitSeconds', seconds);
      this.set('parentView.graphSeconds', null);
    }
    if (popup_path.length) {
      popup_path.children().each(function () {
        $(this).children().remove();
      });
      this.set('isPopup', true);
    }
    else {
      graph_container.children().each(function () {
        if (!($(this).is('.export-graph-list-container, .corner-icon'))) {
          $(this).children().remove();
        }
      });
    }
    var hasData = this.checkSeries(seriesData);
    var view = graphView || this;
    view.set('isExportButtonHidden', !hasData);
    if (hasData) {
      // Check container exists (may be not, if we go to another page and wait while graphs loading)
      if (graph_container.length) {
        container = $(this.get('_containerSelector'));
        this.draw(seriesData);
        this.set('hasData', true);
        //move yAxis value lower to make them fully visible
        container.find('.y_axis text').attr('y', 8);
        container.attr('data-original-title', Em.I18n.t('graphs.tooltip.title'));
      }
    }
    else {
      this.set('isPopupReady', true);
      this.set('isReady', true);
      //if Axis X time interval is default(60 minutes)
      if (this.get('timeUnitSeconds') === 3600) {
        this._showMessage('info', null, this.t('graphs.noData.message'), this.t('graphs.noData.tooltip.title'));
        this.set('hasData', false);
      }
      else {
        this._showMessage('info', this.t('graphs.noData.title'), this.t('graphs.noDataAtTime.message'));
      }
    }
    graph_container = null;
    container = null;
    popup_path = null;
    console.timeEnd('_refreshGraph');
  },

  /**
   * Returns a custom time unit, that depends on X axis interval length, for the graph's X axis.
   * This is needed as Rickshaw's default time X axis uses UTC time, which can be confusing
   * for users expecting locale specific time.
   *
   * If <code>null</code> is returned, Rickshaw's default time unit is used.
   *
   * @type Function
   * @return Rickshaw.Fixtures.Time
   */
  localeTimeUnit: function (timeUnitSeconds) {
    var timeUnit = new Rickshaw.Fixtures.Time(),
      unitName;
    if (timeUnitSeconds < 172800) {
      timeUnit = {
        name: timeUnitSeconds / 240 + ' minute',
        seconds: timeUnitSeconds / 4,
        formatter: function (d) {
          // format locale specific time
          var minutes = dateUtils.dateFormatZeroFirst(d.getMinutes());
          var hours = dateUtils.dateFormatZeroFirst(d.getHours());
          return hours + ":" + minutes;
        }
      };
    } else if (timeUnitSeconds < 1209600) {
      timeUnit = timeUnit.unit('day');
    } else if (timeUnitSeconds < 5184000) {
      timeUnit = timeUnit.unit('week');
    } else if (timeUnitSeconds < 62208000) {
      timeUnit = timeUnit.unit('month');
    } else {
      timeUnit = timeUnit.unit('year');
    }
    return timeUnit;
  },

  /**
   * calculate statistic data for popup legend and set proper colors for series
   * @param {Array} data
   */
  dataPreProcess: function (data) {
    var self = this;
    var palette = new Rickshaw.Color.Palette({scheme: chartUtils.getColorSchemeForChart(data.length)});
    // Format series for display
    var series_min_length = 100000000;
    data.forEach(function (series) {
      var displayUnit = self.get('displayUnit');
      var seriesColor = self.colorForSeries(series);
      if (Em.isNone(seriesColor)) {
        seriesColor = palette.color();
      }
      series.color = seriesColor;
      series.stroke = 'rgba(0,0,0,0.3)';
      // calculate statistic data for popup legend
      var avg = 0;
      var min = Number.MAX_VALUE;
      var max = Number.MIN_VALUE;
      var numberOfNotNullValues = 0;
      series.isZero = true;
      for (var i = 0; i < series.data.length; i++) {
        avg += series.data[i]['y'];
        if (series.data[i]['y'] !== null) {
          numberOfNotNullValues++;
        }
        if (!Em.isNone(series.data[i]['y'])) {
          if (series.data[i]['y'] < min) {
            min = series.data[i]['y'];
          }
        }
        if (series.data[i]['y'] > max) {
          max = series.data[i]['y'];
        }
        if (series.data[i]['y'] > 1) {
          series.isZero = false;
        }
      }
      if (self.get('isPopup')) {
        series.name = string_utils.pad(series.name.length > 36 ? series.name.substr(0, 36) + '...' : series.name, 40, '&nbsp;', 2) + '|&nbsp;' +
          string_utils.pad('min', 5, '&nbsp;', 3) +
          string_utils.pad(self.get('yAxisFormatter')(min), 12, '&nbsp;', 3) +
          string_utils.pad('avg', 5, '&nbsp;', 3) +
          string_utils.pad(self.get('yAxisFormatter')(avg / numberOfNotNullValues), 12, '&nbsp;', 3) +
          string_utils.pad('max', 12, '&nbsp;', 3) +
          string_utils.pad(self.get('yAxisFormatter')(max), 5, '&nbsp;', 3);
      }
      if (series.isZero) {
        series.stroke = series.color;
      }
      if (series.data.length < series_min_length) {
        series_min_length = series.data.length;
      }
    });

    // All series should have equal length
    data.forEach(function (series) {
      if (series.data.length > series_min_length) {
        series.data.length = series_min_length;
      }
    });
  },

  draw: function (seriesData) {
    var self = this;
    var isPopup = this.get('isPopup');
    var p = isPopup ? this.get('popupSuffix') : '';

    this.dataPreProcess(seriesData);

    var chartElement = document.querySelector("#" + this.get('id') + "-chart" + p);
    var overlayElement = document.querySelector(+this.get('id') + "-container" + p);
    var xaxisElement = document.querySelector("#" + this.get('id') + "-xaxis" + p);
    var yaxisElement = document.querySelector("#" + this.get('id') + "-yaxis" + p);
    var legendElement = document.querySelector("#" + this.get('id') + "-legend" + p);

    var graphSize = this._calculateGraphSize();

    var _graph = new Rickshaw.GraphReopened({
      height: graphSize.height,
      width: graphSize.width,
      element: chartElement,
      series: seriesData,
      interpolation: 'step-after',
      stroke: true,
      renderer: this.get('renderer'),
      strokeWidth: 'area' === this.get('renderer') ? 1 : 2,
      max: seriesData.everyProperty('isZero') ? 1 : null
    });

    if ('area' === this.get('renderer')) {
      _graph.renderer.unstack = false;
    }

    new Rickshaw.Graph.Axis.Time({
      graph: _graph,
      timeUnit: this.localeTimeUnit(this.get('timeUnitSeconds'))
    });

    new Rickshaw.Graph.Axis.Y({
      tickFormat: this.yAxisFormatter,
      pixelsPerTick: isPopup ? 75 : 40,
      element: yaxisElement,
      orientation: isPopup ? 'left' : 'right',
      graph: _graph
    });

    var legend = new Rickshaw.Graph.Legend({
      graph: _graph,
      element: legendElement,
      description: self.get('description')
    });

    new Rickshaw.Graph.Behavior.Series.Toggle({
      graph: _graph,
      legend: legend
    });

    new Rickshaw.Graph.Behavior.Series.Order({
      graph: _graph,
      legend: legend
    });

    if (!isPopup) {
      $(overlayElement).on('mousemove', function () {
        $(xaxisElement).removeClass('hide');
        $(legendElement).removeClass('hide');
        $(chartElement).children("div").removeClass('hide');
      });
      $(overlayElement).on('mouseout', function () {
        $(legendElement).addClass('hide');
      });
      _graph.onUpdate(function () {
        $(legendElement).addClass('hide');
      });
    }

    this.$().on('remove', function () {
      $(overlayElement).off();
    });

    //show the graph when it's loaded
    _graph.onUpdate(function () {
      self.set('isReady', true);
    });
    _graph.render();

    if (isPopup) {
      new Rickshaw.Graph.HoverDetail({
        graph: _graph,
        yFormatter: function (y) {
          return self.yAxisFormatter(y);
        },
        xFormatter: function (x) {
          return (new Date(x * 1000)).toLocaleTimeString();
        },
        formatter: function (series, x, y, formattedX, formattedY) {
          return formattedY + '<br />' + formattedX;
        }
      });
    }

    _graph = this.updateSeriesInGraph(_graph);
    if (isPopup) {
      //show the graph when it's loaded
      _graph.onUpdate(function () {
        self.set('isPopupReady', true);
      });
      _graph.update();

      var popupSelector = this.get('_popupSelector');
      $(popupSelector + ' li.line').click(function () {
        var series = [];
        $(popupSelector + ' a.action').each(function (index, v) {
          series[index] = v.parentNode.classList;
        });
        self.set('_seriesProperties', series);
      });

      this.set('_popupGraph', _graph);
    }
    else {
      _graph.update();
      var containerSelector = this.get('_containerSelector');
      $(containerSelector + ' li.line').click(function () {
        var series = [];
        $(containerSelector + ' a.action').each(function (index, v) {
          series[index] = v.parentNode.classList;
        });
        self.set('_seriesPropertiesWidget', series);
      });

      this.set('_graph', _graph);
    }
  },

  /**
   * Calculate graph size
   * @returns {{width: number, height: number}}
   * @private
   */
  _calculateGraphSize: function () {
    var isPopup = this.get('isPopup');
    var height = this.get('height');
    var width = 400;
    var diff = 32;

    if (this.get('inWidget')) {
      height = 105; // for widgets view
      diff = 22;
    }
    if (isPopup) {
      height = 180;
      width = 670;
    }
    else {
      // If not in popup, the width could vary.
      // We determine width based on div's size.
      var thisElement = this.get('element');
      if (!Em.isNone(thisElement)) {
        var calculatedWidth = $(thisElement).width();
        if (calculatedWidth > diff) {
          width = calculatedWidth - diff;
        }
      }
    }
    return {
      width: width,
      height: height
    }
  },

  /**
   *
   * @param {Rickshaw.Graph} graph
   * @returns {Rickshaw.Graph}
   */
  updateSeriesInGraph: function (graph) {
    var id = this.get('id');
    var isPopup = this.get('isPopup');
    var popupSuffix = this.get('popupSuffix');
    var _series = isPopup ? this.get('_seriesProperties') : this.get('_seriesPropertiesWidget');
    graph.series.forEach(function (series, index) {
      if (_series && !Em.isNone(_series[index])) {
        if (_series[_series.length - index - 1].length > 1) {
          var s = '#' + id + '-container' + (isPopup ? popupSuffix : '') + ' a.action:eq(' + (_series.length - index - 1) + ')';
          $(s).parent('li').addClass('disabled');
          series.disable();
        }
      }
    });
    return graph;
  },

  showGraphInPopup: function () {
    if (!this.get('hasData') || this.get('isPreview')) {
      return;
    }

    this.set('isPopup', true);
    if (this.get('inWidget') && !this.get('parentView.isClusterMetricsWidget')) {
      this.setProperties({
        currentTimeIndex: this.get('parentView.timeIndex'),
        customStartTime: this.get('parentView.startTime'),
        customEndTime: this.get('parentView.endTime'),
        customDurationFormatted: this.get('parentView.durationFormatted')
      });
    }
    var self = this;

    App.ModalPopup.show({
      bodyClass: Em.View.extend(App.ExportMetricsMixin, App.TimeRangeMixin, {

        containerId: null,
        containerClass: null,
        yAxisId: null,
        yAxisClass: null,
        xAxisId: null,
        xAxisClass: null,
        legendId: null,
        legendClass: null,
        chartId: null,
        chartClass: null,
        titleId: null,
        titleClass: null,

        timeRangeClassName: 'graph-details-time-range',

        isReady: Em.computed.alias('parentView.graph.isPopupReady'),

        currentTimeRangeIndex: self.get('currentTimeIndex'),
        customStartTime: self.get('currentTimeIndex') === 8 ? self.get('customStartTime') : null,
        customEndTime: self.get('currentTimeIndex') === 8 ? self.get('customEndTime') : null,
        customDurationFormatted: self.get('currentTimeIndex') === 8 ? self.get('customDurationFormatted') : null,

        didInsertElement: function () {
          var popupBody = this;
          this._super();
          App.tooltip(this.$('.corner-icon > .glyphicon-save'), {
            title: Em.I18n.t('common.export')
          });
          this.$().closest('.modal').on('click', function (event) {
            if (!($(event.target).is('.corner-icon, .corner-icon span, .glyphicon-save, .export-graph-list-container, .export-graph-list-container *'))) {
              popupBody.set('isExportMenuHidden', true);
            }
          });
          $('#modal').addClass('modal-graph-line');
          var popupSuffix = this.get('parentView.graph.popupSuffix');
          var id = this.get('parentView.graph.id');
          var idTemplate = id + '-{element}' + popupSuffix;

          this.set('containerId', idTemplate.replace('{element}', 'container'));
          this.set('containerClass', 'chart-container' + popupSuffix);
          this.set('yAxisId', idTemplate.replace('{element}', 'yaxis'));
          this.set('yAxisClass', this.get('yAxisId').replace(popupSuffix, ''));
          this.set('xAxisId', idTemplate.replace('{element}', 'xaxis'));
          this.set('xAxisClass', this.get('xAxisId').replace(popupSuffix, ''));
          this.set('legendId', idTemplate.replace('{element}', 'legend'));
          this.set('legendClass', this.get('legendId').replace(popupSuffix, ''));
          this.set('chartId', idTemplate.replace('{element}', 'chart'));
          this.set('chartClass', this.get('chartId').replace(popupSuffix, ''));
          this.set('titleId', idTemplate.replace('{element}', 'title'));
          this.set('titleClass', this.get('titleId').replace(popupSuffix, ''));
        },

        templateName: require('templates/common/chart/linear_time'),
        /**
         * check is time paging feature is enable for graph
         */
        isTimePagingEnable: function () {
          return !self.get('isTimePagingDisable');
        }.property(),

        rightArrowVisible: function () {
          // Time range is neither custom nor the least possible preset
          return (this.get('isReady') && this.get('parentView.currentTimeIndex') != 0 &&
            this.get('parentView.currentTimeIndex') != 8);
        }.property('isReady', 'parentView.currentTimeIndex'),

        leftArrowVisible: function () {
          // Time range is neither custom nor the largest possible preset
          return (this.get('isReady') && this.get('parentView.currentTimeIndex') < 7);
        }.property('isReady', 'parentView.currentTimeIndex'),

        exportGraphData: function (event) {
          this.set('isExportMenuHidden', true);
          var ajaxIndex = this.get('parentView.graph.ajaxIndex'),
            targetView = ajaxIndex ? this.get('parentView.graph') : self.get('parentView');
          targetView.exportGraphData({
            context: event.context
          });
        },

        setTimeRange: function (event) {
          var index = event.context.index,
            callback = this.get('parentView').reloadGraphByTime.bind(this.get('parentView'), index);
          this._super(event, callback, self);

          // Preset time range is specified by user
          if (index !== 8) {
            callback();
          }
        }
      }),
      header: this.get('title'),
      /**
       * App.ChartLinearTimeView
       */
      graph: self,

      secondary: null,

      onPrimary: function () {
        var targetView = Em.isNone(self.get('parentView.currentTimeRangeIndex')) ? self.get('parentView.parentView') : self.get('parentView');
        self.setProperties({
          currentTimeIndex: targetView.get('currentTimeRangeIndex'),
          customStartTime: targetView.get('customStartTime'),
          customEndTime: targetView.get('customEndTime'),
          customDurationFormatted: targetView.get('customDurationFormatted'),
          isPopup: false
        });
        App.ajax.abortRequests(this.get('graph.runningPopupRequests'));
        this._super();
      },

      onClose: function () {
        this.onPrimary();
      },
      /**
       * move graph back by time
       * @param event
       */
      switchTimeBack: function (event) {
        var index = this.get('currentTimeIndex');
        // 7 - number of last preset time state
        if (index < 7) {
          this.reloadGraphByTime(++index);
        }
      },
      /**
       * move graph forward by time
       * @param event
       */
      switchTimeForward: function (event) {
        var index = this.get('currentTimeIndex');
        if (index) {
          this.reloadGraphByTime(--index);
        }
      },
      /**
       * reload graph depending on the time
       * @param index
       */
      reloadGraphByTime: function (index) {
        this.set('childViews.firstObject.currentTimeRangeIndex', index);
        this.set('currentTimeIndex', index);
        self.setProperties({
          currentTimeIndex: index,
          isPopupReady: false
        });
        App.ajax.abortRequests(this.get('graph.runningPopupRequests'));
      },
      currentTimeIndex: self.get('currentTimeIndex'),

      currentTimeState: function () {
        return self.get('timeStates').objectAt(this.get('currentTimeIndex'));
      }.property('currentTimeIndex')

    });
    Em.run.next(function () {
      self.loadData();
      self.set('isPopupReady', false);
    });
  },
  reloadGraphByTime: function () {
    Em.run.once(this, function () {
      this.loadData();
    });
  }.observes('timeUnitSeconds', 'customStartTime', 'customEndTime'),

  timeStates: [
    {name: Em.I18n.t('graphs.timeRange.hour'), seconds: 3600},
    {name: Em.I18n.t('graphs.timeRange.twoHours'), seconds: 7200},
    {name: Em.I18n.t('graphs.timeRange.fourHours'), seconds: 14400},
    {name: Em.I18n.t('graphs.timeRange.twelveHours'), seconds: 43200},
    {name: Em.I18n.t('graphs.timeRange.day'), seconds: 86400},
    {name: Em.I18n.t('graphs.timeRange.week'), seconds: 604800},
    {name: Em.I18n.t('graphs.timeRange.month'), seconds: 2592000},
    {name: Em.I18n.t('graphs.timeRange.year'), seconds: 31104000},
    {name: Em.I18n.t('common.custom'), seconds: 0}
  ],
  // should be set by time range control dropdown list when create current graph
  currentTimeIndex: 0,
  customStartTime: null,
  customEndTime: null,
  customDurationFormatted: null,
  setCurrentTimeIndexFromParent: function () {
    // 8 index corresponds to custom start and end time selection
    var targetView = !Em.isNone(this.get('parentView.currentTimeRangeIndex')) ? this.get('parentView') : this.get('parentView.parentView'),
      index = targetView.get('currentTimeRangeIndex'),
      customStartTime = (index === 8) ? targetView.get('customStartTime') : null,
      customEndTime = (index === 8) ? targetView.get('customEndTime'): null,
      customDurationFormatted = (index === 8) ? targetView.get('customDurationFormatted'): null;
    this.setProperties({
      currentTimeIndex: index,
      customStartTime: customStartTime,
      customEndTime: customEndTime,
      customDurationFormatted: customDurationFormatted
    });
    if (index !== 8 || targetView.get('customStartTime') && targetView.get('customEndTime')) {
      App.ajax.abortRequests(this.get('runningRequests'));
      if (this.get('inWidget') && !this.get('parentView.isClusterMetricsWidget')) {
        this.set('parentView.isLoaded', false);
      } else {
        this.set('isReady', false);
      }
    }
  }.observes('parentView.parentView.currentTimeRangeIndex', 'parentView.currentTimeRangeIndex', 'parentView.parentView.customStartTime', 'parentView.customStartTime', 'parentView.parentView.customEndTime', 'parentView.customEndTime'),
  timeUnitSeconds: 3600,
  timeUnitSecondsSetter: function () {
    var index = this.get('currentTimeIndex'),
      startTime = this.get('customStartTime'),
      endTime = this.get('customEndTime'),
      durationFormatted = this.get('customDurationFormatted'),
      seconds;
    if (index !== 8) {
      // Preset time range is specified by user
      seconds = this.get('timeStates').objectAt(this.get('currentTimeIndex')).seconds;
    } else if (!Em.isNone(startTime) && !Em.isNone(endTime)) {
      // Custom start and end time is specified by user
      seconds = (endTime - startTime) / 1000;
    }
    if (!Em.isNone(seconds)) {
      this.set('timeUnitSeconds', seconds);
      if (this.get('inWidget') && !this.get('parentView.isClusterMetricsWidget')) {
        this.get('parentView').setProperties({
          graphSeconds: seconds,
          timeIndex: index,
          startTime: startTime,
          endTime: endTime,
          durationFormatted: durationFormatted
        });
      }
    }
  }.observes('currentTimeIndex', 'customStartTime', 'customEndTime')

});

/**
 * A formatter which will turn a number into computer storage sizes of the
 * format '23 GB' etc.
 *
 * @type {Function}
 * @return {string}
 */
App.ChartLinearTimeView.BytesFormatter = function (y) {
  if (0 == y) {
    return '0 B';
  }
  var value = Rickshaw.Fixtures.Number.formatBase1024KMGTP(y);
  if (!y) {
    return '0 B';
  }
  if ("number" == typeof value) {
    value = String(value);
  }
  if ("string" == typeof value) {
    value = value.replace(/\.\d(\d+)/, function ($0, $1) { // Remove only 1-digit after decimal part
      return $0.replace($1, '');
    });
    // Either it ends with digit or ends with character
    value = value.replace(/(\d$)/, '$1 '); // Ends with digit like '120'
    value = value.replace(/([a-zA-Z]$)/, ' $1'); // Ends with character like
    // '120M'
    value = value + 'B'; // Append B to make B, MB, GB etc.
  }
  return value;
};

/**
 * A formatter which will turn a number into percentage display like '42%'
 *
 * @type {Function}
 * @param {number} percentage
 * @return {string}
 */
App.ChartLinearTimeView.PercentageFormatter = function (percentage) {
  return percentage ?
    percentage.toFixed(3).replace(/0+$/, '').replace(/\.$/, '') + '%' :
    '0 %';
};

/**
 * A formatter which will turn a number into percentage display like '42%'
 *
 * @type {Function}
 * @param {number} value
 * @param {string} displayUnit
 * @return {string}
 */
App.ChartLinearTimeView.DisplayUnitFormatter = function (value, displayUnit) {
  return value ?
    value.toFixed(3).replace(/0+$/, '').replace(/\.$/, '') + " " + displayUnit :
    '0 ' + displayUnit;
};

/**
 * A formatter which will turn elapsed time into display time like '50 ms',
 * '5s', '10 m', '3 hr' etc. Time is expected to be provided in milliseconds.
 *
 * @type {Function}
 * @return {string}
 */
App.ChartLinearTimeView.TimeElapsedFormatter = function (millis) {
  if (!millis) {
    return '0 ms';
  }
  if ('number' == Em.typeOf(millis)) {
    var seconds = millis > 1000 ? Math.round(millis / 1000) : 0;
    var minutes = seconds > 60 ? Math.round(seconds / 60) : 0;
    var hours = minutes > 60 ? Math.round(minutes / 60) : 0;
    var days = hours > 24 ? Math.round(hours / 24) : 0;
    if (days > 0) {
      return days + ' d';
    }
    if (hours > 0) {
      return hours + ' hr';
    }
    if (minutes > 0) {
      return minutes + ' m';
    }
    if (seconds > 0) {
      return seconds + ' s';
    }
    return millis.toFixed(3).replace(/0+$/, '').replace(/\.$/, '') + ' ms';
  }
  return millis;
};

/**
 * The default formatter which uses Rickshaw.Fixtures.Number.formatKMBT
 * which shows 10K, 300M etc.
 *
 * @type {Function}
 * @return {string|number}
 */
App.ChartLinearTimeView.DefaultFormatter = function (y) {
  if (!y) {
    return 0;
  }
  var value = Rickshaw.Fixtures.Number.formatKMBT(y);
  if ('' == value) return '0';
  value = String(value);
  var c = value[value.length - 1];
  return isNaN(parseInt(c)) ?
  parseFloat(value.substr(0, value.length - 1)).toFixed(3).replace(/0+$/, '').replace(/\.$/, '') + c :
    parseFloat(value).toFixed(3).replace(/0+$/, '').replace(/\.$/, '');
};


/**
 * Creates and returns a formatter that can convert a 'value'
 * to 'value units/s'.
 *
 * @param unitsPrefix Prefix which will be used in 'unitsPrefix/s'
 * @param valueFormatter  Value itself will need further processing
 *        via provided formatter. Ex: '10M requests/s'. Generally
 *        should be App.ChartLinearTimeView.DefaultFormatter.
 * @return {Function}
 */
App.ChartLinearTimeView.CreateRateFormatter = function (unitsPrefix, valueFormatter) {
  var suffix = " " + unitsPrefix + "/s";
  return function (value) {
    value = valueFormatter(value) + suffix;
    return value;
  };
};

Rickshaw.GraphReopened = function (a) {
  Rickshaw.Graph.call(this, a);
};

//reopened in order to exclude "null" value from validation
Rickshaw.GraphReopened.prototype = Object.create(Rickshaw.Graph.prototype, {
  validateSeries: {
    value: function (series) {

      if (!(series instanceof Array) && !(series instanceof Rickshaw.Series)) {
        var seriesSignature = Object.prototype.toString.apply(series);
        throw "series is not an array: " + seriesSignature;
      }

      var pointsCount;

      series.forEach(function (s) {

        if (!(s instanceof Object)) {
          throw "series element is not an object: " + s;
        }
        if (!(s.data)) {
          throw "series has no data: " + JSON.stringify(s);
        }
        if (!(s.data instanceof Array)) {
          throw "series data is not an array: " + JSON.stringify(s.data);
        }

        pointsCount = pointsCount || s.data.length;

        if (pointsCount && s.data.length != pointsCount) {
          throw "series cannot have differing numbers of points: " +
          pointsCount + " vs " + s.data.length + "; see Rickshaw.Series.zeroFill()";
        }
      })
    },
    enumerable: true,
    configurable: false,
    writable: false
  }
});

//show no line if point is "null"
Rickshaw.Graph.Renderer.Line.prototype.seriesPathFactory = function () {

  var graph = this.graph;

  return d3.svg.line()
    .x(function (d) {
      return graph.x(d.x)
    })
    .y(function (d) {
      return graph.y(d.y)
    })
    .defined(function (d) {
      return d.y != null;
    })
    .interpolate(this.graph.interpolation).tension(this.tension);
};

//show no area if point is null
Rickshaw.Graph.Renderer.Stack.prototype.seriesPathFactory = function () {

  var graph = this.graph;

  return d3.svg.area()
    .x(function (d) {
      return graph.x(d.x)
    })
    .y0(function (d) {
      return graph.y(d.y0)
    })
    .y1(function (d) {
      return graph.y(d.y + d.y0)
    })
    .defined(function (d) {
      return d.y != null;
    })
    .interpolate(this.graph.interpolation).tension(this.tension);
};


/**
 * aggregate requests to load metrics by component name
 * requests can be added via add method
 * input example:
 * {
 *   data: request,
 *   context: this,
 *   startCallName: this.getServiceComponentMetrics,
 *   successCallback: this.getMetricsSuccessCallback,
 *   completeCallback: function () {
 *     requestCounter--;
 *     if (requestCounter === 0) this.onMetricsLoaded();
 *   }
 * }
 * @type {Em.Object}
 */
App.ChartLinearTimeView.LoadAggregator = Em.Object.create({
  /**
   * @type {Array}
   */
  requests: [],

  /**
   * @type {number|null}
   */
  timeoutId: null,

  /**
   * time interval within which calls get collected
   * @type {number}
   * @const
   */
  BULK_INTERVAL: 1000,

  /**
   * add request
   * every {{BULK_INTERVAL}} requests get collected, aggregated and sent to server
   *
   * @param {object} context
   * @param {object} requestData
   */
  add: function (context, requestData) {
    var self = this;

    requestData.context = context;
    this.get('requests').push(requestData);
    if (Em.isNone(this.get('timeoutId'))) {
      this.set('timeoutId', window.setTimeout(function () {
        self.runRequests(self.get('requests'));
        self.get('requests').clear();
        clearTimeout(self.get('timeoutId'));
        self.set('timeoutId', null);
      }, this.get('BULK_INTERVAL')));
    }
  },

  /**
   * return requests which grouped into bulks
   * @param {Array} requests
   * @returns {object} bulks
   */
  groupRequests: function (requests) {
    var bulks = {};

    requests.forEach(function (request) {
      var id = request.name;

      if (Em.isNone(bulks[id])) {
        bulks[id] = {
          name: request.name,
          fields: request.fields.slice(),
          context: request.context
        };
        bulks[id].subRequests = [{
          context: request.context
        }];
      } else {
        bulks[id].fields.pushObjects(request.fields);
        bulks[id].subRequests.push({
          context: request.context
        });
      }
    }, this);
    return bulks;
  },

  /**
   * run aggregated requests
   * @param {Array} requests
   */
  runRequests: function (requests) {
    var bulks = this.groupRequests(requests);
    var self = this;

    for (var id in bulks) {
      (function (_request) {
        var fields = self.formatRequestData(_request);
        var hostName = (_request.context.get('content')) ? _request.context.get('content.hostName') : "";
        var xhr = App.ajax.send({
          name: _request.name,
          sender: _request.context,
          data: {
            fields: fields,
            hostName: hostName
          }
        });

        xhr.done(function (response) {
          console.time('==== runRequestsDone');
          _request.subRequests.forEach(function (subRequest) {
            subRequest.context._refreshGraph.call(subRequest.context, response);
          }, this);
          console.timeEnd('==== runRequestsDone');
        }).fail(function (jqXHR, textStatus, errorThrown) {
          if (!jqXHR.isForcedAbort) {
            _request.subRequests.forEach(function (subRequest) {
              subRequest.context.loadDataErrorCallback.call(subRequest.context, jqXHR, textStatus, errorThrown);
            }, this);
          }
        }).always(function () {
          _request.context.set('runningRequests', Em.tryInvoke(_request.context.get('runningRequests'), 'without', [xhr]));
        });
        Em.tryInvoke(_request.context.get('runningRequests'), 'push', [xhr]);
      })(bulks[id]);
    }
  },

  /**
   *
   * @param {object} request
   * @returns {number[]}
   */
  formatRequestData: function (request) {
    var fromSeconds, toSeconds;
    if (request.context.get('currentTimeIndex') === 8 && !Em.isNone(request.context.get('customStartTime')) && !Em.isNone(request.context.get('customEndTime'))) {
      // Custom start and end time is specified by user
      toSeconds = request.context.get('customEndTime') / 1000;
      fromSeconds = request.context.get('customStartTime') / 1000;
    } else {
      var timeUnit = request.context.get('timeUnitSeconds');
      toSeconds = Math.round(App.dateTime() / 1000);
      fromSeconds = toSeconds - timeUnit;
    }
    var fields = request.fields.uniq().map(function (field) {
      return field + "[" + (fromSeconds) + "," + toSeconds + "," + 15 + "]";
    });

    return fields.join(",");
  }
});
