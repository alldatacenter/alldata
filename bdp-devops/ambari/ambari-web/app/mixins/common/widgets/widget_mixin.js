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
var stringUtils = require('utils/string_utils');

App.WidgetMixin = Ember.Mixin.create({

  /**
   *  type of metric query from which the widget is comprised
   */

  metricType: 'POINT_IN_TIME',
  /**
   * @type {RegExp}
   * @const
   */
  EXPRESSION_REGEX: /\$\{([\w\s\.\,\+\-\*\/\(\)\:\=\[\]]*)\}/g,

  /**
   * @type {RegExp}
   * @const
   */
  MATH_EXPRESSION_REGEX: /^[\d\s\+\-\*\/\(\)\.]+$/,

  /**
   * @type {RegExp}
   * @const
   */
  VALUE_NAME_REGEX: /[^(\s+\-\*\/\\+\s+)+](\w+\s+\w+)?[\w\.\,\-\:\=\[\]]*/g,

  /**
   * @type {string}
   * @const
   */
  CLONE_SUFFIX: '(Copy)',

  /**
   * @type {number|null}
   */
  timeoutId: null,

  /**
   * common metrics container
   * @type {Array}
   */
  metrics: [],

  /**
   *
   */
  aggregatorFunc: ['._sum', '._avg', '._min', '._max', '._rate'],

  /**
   * @type {boolean}
   */
  isLoaded: false,

  /**
   * @type {App.Widget}
   * @default null
   */
  content: null,

  /**
   * color of content calculated by thresholds
   * @type {string}
   */
  contentColor: Em.computed.ifThenElse('value', 'green', 'grey'),

  beforeRender: function () {
    this.loadMetrics();
  },


  /**
   * load metrics
   */
  loadMetrics: function () {
    var requestData = this.getRequestData(this.get('content.metrics')),
      request,
      requestCounter = 0,
      self = this;

    this.set('metrics', []);

    for (var i in requestData) {
      request = requestData[i];
      requestCounter++;
      if (this.get('content.widgetType') === 'HEATMAP') {
        if (request.service_name === 'STACK') {
          this.getHostsMetrics(request).complete(function () {
            requestCounter--;
            if (requestCounter === 0) self.onMetricsLoaded();
          });
        } else {
          this.getHostComponentsMetrics(request).complete(function () {
            requestCounter--;
            if (requestCounter === 0) self.onMetricsLoaded();
          });
        }
      } else if (request.host_component_criteria) {
        App.WidgetLoadAggregator.add({
          data: request,
          context: this,
          startCallName: 'getHostComponentMetrics',
          successCallback: this.getHostComponentMetricsSuccessCallback,
          errorCallback: this.getMetricsErrorCallback,
          completeCallback: function (xhr) {
            requestCounter--;
            if (requestCounter === 0) this.onMetricsLoaded();
            if (this.get('graphView')) {
              var graph = this.get('childViews') && this.get('childViews').findProperty('runningRequests');
              if (graph) {
                var requestsArrayName = graph.get('isPopup') ? 'runningPopupRequests' : 'runningRequests';
                graph.set(requestsArrayName, graph.get(requestsArrayName).reject(function (item) {
                  return item === xhr;
                }));
              }
            }
          }
        });
      } else {
        App.WidgetLoadAggregator.add({
          data: request,
          context: this,
          startCallName: 'getServiceComponentMetrics',
          successCallback: this.getMetricsSuccessCallback,
          errorCallback: this.getMetricsErrorCallback,
          completeCallback: function (xhr) {
            requestCounter--;
            if (requestCounter === 0) this.onMetricsLoaded();
            if (this.get('graphView')) {
              var graph = this.get('childViews') && this.get('childViews').findProperty('runningRequests');
              if (graph) {
                var requestsArrayName = graph.get('isPopup') ? 'runningPopupRequests' : 'runningRequests';
                graph.set(requestsArrayName, graph.get(requestsArrayName).reject(function (item) {
                  return item === xhr;
                }));
              }
            }
          }
        });
      }
    }
  }.observes('customTimeRange', 'content.properties.time_range'),

  /**
   * get data formatted for request
   * @param {Array} metrics
   */
  getRequestData: function (metrics) {
    var requestsData = {};
    if (metrics) {
      metrics.forEach(function (metric) {
        var key;
        if (metric.host_component_criteria) {
          key = metric.service_name + '_' + metric.component_name + '_' + metric.host_component_criteria;
        } else {
          key = metric.service_name + '_' + metric.component_name;
        }
        var requestMetric = $.extend({}, metric);

        if (requestsData[key]) {
          requestsData[key]["metric_paths"].push({
            metric_path: requestMetric["metric_path"],
            metric_type: this.get('metricType'),
            id: requestMetric["metric_path"] + "_" + this.get('metricType'),
            context: this
          });
        } else {
          requestMetric["metric_paths"] = [{
            metric_path: requestMetric["metric_path"],
            metric_type: this.get('metricType'),
            id: requestMetric["metric_path"] + "_" + this.get('metricType'),
            context: this}];
          delete requestMetric["metric_path"];
          requestMetric.tag = this.get('content.tag');
          requestsData[key] = requestMetric;
        }
      }, this);
    }
    return requestsData;
  },

  /**
   * Tweak host component criteria  from the actual host component criteria
   * @param {object} request
   */
  computeHostComponentCriteria: function (request) {
    return request.host_component_criteria.replace('host_components/', '&').trim();
  },

  /**
   * make GET call to server in order to fetch service-component metrics
   * @param {object} request
   * @returns {$.ajax}
   */
  getServiceComponentMetrics: function (request) {
    var xhr = App.ajax.send({
      name: 'widgets.serviceComponent.metrics.get',
      sender: this,
      data: {
        serviceName: request.service_name,
        componentName: request.component_name,
        metricPaths: this.prepareMetricPaths(request.metric_paths)
      }
    });
    if (this.get('graphView')) {
      var graph = this.get('childViews') && this.get('childViews').findProperty('runningRequests');
      if (graph) {
        var requestsArrayName = graph.get('isPopup') ? 'runningPopupRequests' : 'runningRequests';
        graph.get(requestsArrayName).push(xhr);
      }
    }
    return xhr;
  },

  /**
   * aggregate all metric names in the query. Add time range and step to temporal queries
   * @param {Array} metricPaths
   * @returns {string}
   */
  prepareMetricPaths: function(metricPaths) {
    var temporalMetrics = metricPaths.filterProperty('metric_type', 'TEMPORAL');
    var pointInTimeMetrics = metricPaths.filterProperty('metric_type', 'POINT_IN_TIME');
    var result = temporalMetrics.length ? temporalMetrics[0].context.addTimeProperties(temporalMetrics.mapProperty('metric_path')) : [];

    if (pointInTimeMetrics.length) {
      result = result.concat(pointInTimeMetrics.mapProperty('metric_path'));
    }

    return result.join(',');
  },


  /**
   * make GET call to server in order to fetch specific host-component metrics
   * @param {object} request
   * @returns {$.ajax}
   */
  getHostComponentMetrics: function (request) {
    var metricPaths = this.prepareMetricPaths(request.metric_paths);
    var data = {
      componentName: request.component_name,
      metricPaths: this.prepareMetricPaths(request.metric_paths),
      hostComponentCriteria: this.computeHostComponentCriteria(request)
    };

    if (request.tag) {
      data.selectedHostsParam = '&HostRoles/host_name.in(' + App.HDFSService.find('HDFS').get('masterComponentGroups').findProperty('name', request.tag).hosts.join(',') + ')';
    }

    if (metricPaths.length) {
      var xhr = App.ajax.send({
          name: 'widgets.hostComponent.metrics.get',
          sender: this,
          data: data
        }),
        graph = this.get('graphView') && this.get('childViews') && this.get('childViews').findProperty('runningRequests');
      if (graph) {
        var requestsArrayName = graph.get('isPopup') ? 'runningPopupRequests' : 'runningRequests';
        graph.get(requestsArrayName).push(xhr);
      }
      return xhr;
    }
    return jQuery.Deferred().reject().promise();
  },

  getHostComponentMetricsSuccessCallback: function (data) {
    if (data.items[0]) {
      this.getMetricsSuccessCallback(data.items[0]);
    }
  },

  /**
   * callback on getting aggregated metrics and host component metrics
   * @param data
   */
  getMetricsSuccessCallback: function (data) {
    var atLeastOneMetricPresent = false;

    if (this.get('content.metrics')) {
      this.get('content.metrics').forEach(function (_metric) {
        var metricPath = _metric.metric_path;
        
        var metric_data = Em.get(data, metricPath.replace(/\//g, '.'));
        if (Em.isNone(metric_data)) {
          metric_data = this.parseMetricsWithAggregatorFunc(data, metricPath);
        }
        if (!Em.isNone(metric_data)) {
          atLeastOneMetricPresent = true;
          _metric.data = metric_data;
          this.get('metrics').pushObject(_metric);
        }
      }, this);

      if (!atLeastOneMetricPresent) {
        this.disableGraph();
      }
    }
  },
  
  parseMetricsWithAggregatorFunc: function(data, metric_path) {
    let isAggregatorFunc = false;
    let metric = null;
    this.aggregatorFunc.forEach(function (_item) {
      if (metric_path.endsWith(_item) && !isAggregatorFunc) {
        isAggregatorFunc = true;
        var metricBeanProperty = metric_path.split("/").pop();
        var metricBean;
        metric_path = metric_path.substring(0, metric_path.indexOf(metricBeanProperty));
        
        if (metric_path.endsWith("/")) {
          metric_path = metric_path.slice(0, -1);
        }
        metricBean = Em.get(data, metric_path.replace(/\//g, '.'));
        if (!Em.isNone(metricBean)) {
          metric = metricBean[metricBeanProperty];
        }
      }
    }, this);
    return metric;
  },

  /**
   * if no metrics were received from server then disable graph
   */
  disableGraph: function() {
    if (this.get('graphView')) {
      var graph = this.get('childViews') && this.get('childViews').findProperty('_showMessage');
      if (graph) {
        graph.set('hasData', false);
        this.set('isExportButtonHidden', true);
        graph._showMessage('info', this.t('graphs.noData.title'), this.t('graphs.noDataAtTime.message'));
      }
    }
    this.set('metrics', this.get('metrics').reject(function (item) {
      return this.get('content.metrics').someProperty('name', item.name);
    }, this));
  },

  /**
   * error callback on getting aggregated metrics and host component metrics
   * @param {object} xhr
   * @param {string} textStatus
   * @param {string} errorThrown
   */
  getMetricsErrorCallback: function (xhr, textStatus, errorThrown) {
    if (this.get('graphView') && !xhr.isForcedAbort) {
      var graph = this.get('childViews') && this.get('childViews').findProperty('_showMessage');
      if (graph) {
        if (xhr.readyState == 4 && xhr.status) {
          textStatus = xhr.status + " " + textStatus;
        }
        graph.set('hasData', false);
        this.set('isExportButtonHidden', true);
        graph._showMessage('warn', this.t('graphs.error.title'), this.t('graphs.error.message').format(textStatus, errorThrown));
        this.set('metrics', this.get('metrics').reject(function (item) {
          return this.get('content.metrics').someProperty('name', item.name);
        }, this));
      }
    }
  },

  /**
   * make GET call to get metrics value for all host components
   * @param {object} request
   * @return {$.ajax}
   */
  getHostComponentsMetrics: function (request) {
    request.metric_paths  = request.metric_paths.map((_metric) => {
      return "host_components/" + _metric.metric_path;
    });
    return App.ajax.send({
      name: 'widgets.serviceComponent.metrics.get',
      sender: this,
      data: {
        serviceName: request.service_name,
        componentName: request.component_name,
        metricPaths: request.metric_paths.join(',')
      },
      success: 'getHostComponentsMetricsSuccessCallback'
    });
  },


  getHostComponentsMetricsSuccessCallback: function (data) {
    var metrics = this.get('content.metrics');
    data.host_components.forEach(function (item) {
      metrics.forEach(function (_metric) {
        const metric = $.extend({}, _metric, true);
        metric.hostName = item.HostRoles.host_name;
        if (!Em.isNone(Em.get(item, _metric.metric_path.replace(/\//g, '.')))) {
          metric.data = Em.get(item, _metric.metric_path.replace(/\//g, '.'));
        }
        this.get('metrics').pushObject(metric);
      }, this);
    }, this);
  },

  getHostsMetrics: function (request) {
    var metricPaths = request.metric_paths.mapProperty('metric_path');
    return App.ajax.send({
      name: 'widgets.hosts.metrics.get',
      sender: this,
      data: {
        metricPaths: metricPaths.join(',')
      },
      success: 'getHostsMetricsSuccessCallback'
    });
  },

  getHostsMetricsSuccessCallback: function (data) {
    var metrics = this.get('content.metrics');
    data.items.forEach(function (item) {
      metrics.forEach(function (_metric) {
        const metric = $.extend({}, _metric, true);
        metric.hostName = item.Hosts.host_name;
        if (!Em.isNone(Em.get(item, _metric.metric_path.replace(/\//g, '.')))) {
          metric.data = Em.get(item, _metric.metric_path.replace(/\//g, '.'));
        }
        this.get('metrics').pushObject(metric);
      }, this);
    }, this);
  },

  /**
   * callback on metrics loaded
   */
  onMetricsLoaded: function () {
    var self = this;
    if (!this.get('isLoaded')) this.set('isLoaded', true);
    this.drawWidget();
    clearTimeout(this.get('timeoutId'));
    this.set('timeoutId', setTimeout(function () {
      self.loadMetrics();
    }, App.contentUpdateInterval));
  },


  /**
   * draw widget
   */
  drawWidget: function () {
    if (this.get('isLoaded')) {
      this.calculateValues();
      this.set('value', (this.get('content.values')[0]) ? this.get('content.values')[0].computedValue : '');
    }
  },

  /**
   * initialize tooltips
   */
  initTooltip: function () {
    var self = this;

    if (this.get('isLoaded')) {
      Em.run.next(function(){
        if (self.get('state') === 'inDOM') {
          App.tooltip(self.$(".corner-icon > .glyphicon-copy"), {title: Em.I18n.t('common.clone')});
          App.tooltip(self.$(".corner-icon > .glyphicon-edit"), {title: Em.I18n.t('common.edit')});
          App.tooltip(self.$(".corner-icon > .glyphicon-save"), {title: Em.I18n.t('common.export')});
        }
      });
    }
  }.observes('isLoaded'),

  willDestroyElement: function() {
    this.$(".corner-icon > .glyphicon-copy").tooltip('destroy');
    this.$(".corner-icon > .glyphicon-edit").tooltip('destroy');
    this.$(".corner-icon > .glyphicon-save").tooltip('destroy');
  },

  /**
   * calculate series datasets for graph widgets
   */
  calculateValues: function () {
    this.get('content.values').forEach(function (value) {
      var computeExpression = this.computeExpression(this.extractExpressions(value), this.get('metrics'));
      value.computedValue = value.value.replace(this.get('EXPRESSION_REGEX'), function (match) {
        var float = parseFloat(computeExpression[match]);
        if (isNaN(float)) {
          return computeExpression[match] || "<span class=\"grey\">n/a</span>";
        } else {
          return String((float % 1 !== 0) ? float.toFixed(2) : float);
        }
      });
    }, this);
  },


  /**
   * extract expressions
   * Example:
   *  input: "${a/b} equal ${b+a}"
   *  expressions: ['a/b', 'b+a']
   *
   * @param {object} input
   * @returns {Array}
   */
  extractExpressions: function (input) {
    var pattern = this.get('EXPRESSION_REGEX'),
      expressions = [],
      match;

    if (Em.isNone(input)) return expressions;

    while (match = pattern.exec(input.value)) {
      expressions.push(match[1]);
    }
    return expressions;
  },


  /**
   * compute expression
   * @param expressions
   * @param metrics
   * @returns {object}
   */
  computeExpression: function (expressions, metrics) {
    var result = {};

    expressions.forEach(function (_expression) {
      var validExpression = true;
      var value = "";

      //replace values with metrics data
      var beforeCompute = _expression.replace(this.get('VALUE_NAME_REGEX'), function (match) {
        if (window.isNaN(match)) {
          if (metrics.someProperty('name', match)) {
            return metrics.findProperty('name', match).data;
          } else {
            validExpression = false;
          }
        } else {
          return match;
        }
      });

      //check for correct math expression
      if (!(validExpression && this.get('MATH_EXPRESSION_REGEX').test(beforeCompute))) {
        validExpression = false;
      }

      result['${' + _expression + '}'] = (validExpression) ? Number(window.eval(beforeCompute)).toString() : value;
    }, this);
    return result;
  },

  /*
   * make call when clicking on "remove icon" on widget
   */
  hideWidget: function (event) {
    this.get('controller').hideWidget(
      {
        context: Em.Object.create({
          id: event.contexts[0],
          nsLayout: event.contexts[1]
        })
      }
    );
  },

  /*
   * make call when clicking on "clone icon" on widget
   */
  cloneWidget: function () {
    var self = this;
    return App.showConfirmationPopup(
      function () {
        self.postWidgetDefinition(true);
      },
      Em.I18n.t('widget.clone.body').format(stringUtils.htmlEntities(self.get('content.widgetName'))),
      null,
      null,
      Em.I18n.t('common.clone')
    );
  },

  /**
   * collect all needed data to create new widget
   * @returns {{WidgetInfo: {widget_name: *, widget_type: *, description: *, scope: *, metrics: *, values: *, properties: *}}}
   */
  collectWidgetData: function () {
    var widgetData = {
      WidgetInfo: {
        widget_name: this.get('content.widgetName'),
        widget_type: this.get('content.widgetType'),
        description: this.get('content.widgetDescription'),
        scope: this.get('content.scope'),
        "metrics": this.get('content.metrics').map(function (metric) {
          return {
            "name": metric.name,
            "service_name": metric.service_name,
            "component_name": metric.component_name,
            "host_component_criteria":  metric.host_component_criteria,
            "metric_path": metric.metric_path
          }
        }),
        values: this.get('content.values'),
        properties: this.get('content.properties')
      }
    };

    this.get('content.metrics').forEach(function (metric) {
      if (metric.tag) widgetData.WidgetInfo.tag = metric.tag;
    });

    return widgetData;
  },

  /**
   * post widget definition to server
   * @param {boolean} isClone
   *  * @param {boolean} isEditClonedWidget
   * @returns {$.ajax}
   */
  postWidgetDefinition: function (isClone, isEditClonedWidget) {
    var data = this.collectWidgetData();
    if (isClone) {
      data.WidgetInfo.widget_name += this.get('CLONE_SUFFIX');
      data.WidgetInfo.scope = 'USER';
    }
    var successCallback =  isEditClonedWidget ? 'editNewClonedWidget' :  'postWidgetDefinitionSuccessCallback';
    return App.ajax.send({
      name: 'widgets.wizard.add',
      sender: this,
      data: {
        data: data
      },
      success: successCallback
    });
  },

  postWidgetDefinitionSuccessCallback: function (data) {
    var widgets = this.get('content.layout.widgets').toArray();
    widgets.pushObject(Em.Object.create({
      id: data.resources[0].WidgetInfo.id
    }));
    var mainServiceInfoMetricsController =  App.router.get('mainServiceInfoMetricsController');
    mainServiceInfoMetricsController.saveWidgetLayout(widgets).done(function(){
      mainServiceInfoMetricsController.updateActiveLayout();
    });
  },

  /*
   * enter edit wizard of the newly cloned widget
   */
  editNewClonedWidget: function (data) {
    var controller = this.get('controller');
    var widgets = this.get('content.layout.widgets').toArray();
    var id = data.resources[0].WidgetInfo.id;
    widgets.pushObject(Em.Object.create({
      id: id
    }));
    var mainServiceInfoMetricsController =  App.router.get('mainServiceInfoMetricsController');
    mainServiceInfoMetricsController.saveWidgetLayout(widgets).done(function() {
      mainServiceInfoMetricsController.getActiveWidgetLayout().done(function() {
        controller.editWidget(App.Widget.find(id));
      });
    });
  },

  /*
   * make call when clicking on "edit icon" on widget
   */
  editWidget: function () {
    var self = this;
    var isShared = this.get('content.scope') === 'CLUSTER';
    if (!isShared) {
      self.get('controller').editWidget(self.get('content'));
    } else {
      return App.ModalPopup.show({
        header: Em.I18n.t('common.warning'),
        bodyClass: Em.View.extend({
          template: Ember.Handlebars.compile('{{t widget.edit.body}}')
        }),
        primary: App.isAuthorized('CLUSTER.MANAGE_WIDGETS') ? Em.I18n.t('widget.edit.button.primary') : null,
        secondaryClass: App.isAuthorized('CLUSTER.MANAGE_WIDGETS') ? 'btn-default' : 'btn-success',
        secondary: Em.I18n.t('widget.edit.button.secondary'),
        third: Em.I18n.t('common.cancel'),
        onPrimary: function () {
          this.hide();
          self.get('controller').editWidget(self.get('content'));
        },
        onSecondary: function () {
          this.hide();
          self.postWidgetDefinition(true, true);
        },
        onThird: function () {
          this.hide();
        }
      });
    }
  }

});
App.WidgetPreviewMixin = Ember.Mixin.create({
  beforeRender: Em.K,
  isLoaded: true,
  isPreview: true,
  metrics: [],
  content: Em.Object.create({
    id: 1,
    values: []
  }),
  loadMetrics: function () {
    this.get('content').setProperties({
      'values': this.get('controller.widgetValues'),
      'properties': this.get('controller.widgetProperties'),
      'widgetName': this.get('controller.widgetName'),
      'metrics': this.get('controller.widgetMetrics')
    });
    this._super();
  }.observes('controller.widgetProperties', 'controller.widgetValues', 'controller.widgetMetrics', 'controller.widgetName'),
  onMetricsLoaded: function () {
    this.drawWidget();
  }
});


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
App.WidgetLoadAggregator = Em.Object.create({
  /**
   * @type {Array}
   */
  requests: [],

  /**
   * @type {number|null}
   */
  timeoutId: null,

  /**
   * @type {number}
   * @const
   */
  BULK_INTERVAL: 1000,

  arrayUtils: require('utils/array_utils'),

  /**
   * add request
   * every {{BULK_INTERVAL}} requests get collected, aggregated and sent to server
   *
   * @param {object} request
   */
  add: function (request) {
    var self = this;

    this.get('requests').push(request);
    if (Em.isNone(this.get('timeoutId'))) {
      this.set('timeoutId', window.setTimeout(function () {
        //clear requests that are belongs to destroyed views
        self.set('requests', self.get('requests').filterProperty('context.state', 'inDOM'));

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
      //poll metrics for graph widgets separately
      var graphSuffix = request.context.get('content.widgetType') === "GRAPH" ? "_graph" : '';
      var tagSuffix = request.context.get('content.tag') ? '_' + request.context.get('content.tag') : '';
      var id = request.startCallName + "_" + request.data.component_name + graphSuffix + tagSuffix;

      if (Em.isNone(bulks[id])) {
        bulks[id] = {
          data: request.data,
          context: request.context,
          startCallName: request.startCallName
        };
        bulks[id].subRequests = [{
          context: request.context,
          successCallback: request.successCallback,
          errorCallback: request.errorCallback,
          completeCallback: request.completeCallback
        }];
      } else {
        bulks[id].data.metric_paths.pushObjects(request.data.metric_paths);
        bulks[id].subRequests.push({
          context: request.context,
          successCallback: request.successCallback,
          errorCallback: request.errorCallback,
          completeCallback: request.completeCallback
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
        _request.data.metric_paths = self.arrayUtils.uniqObjectsbyId(_request.data.metric_paths, "id");
        _request.context[_request.startCallName].call(_request.context, _request.data).done(function (response) {
          _request.subRequests.forEach(function (subRequest) {
            subRequest.successCallback.call(subRequest.context, response);
          }, this);
        }).fail(function (xhr, textStatus, errorThrown) {
            _request.subRequests.forEach(function (subRequest) {
              if (subRequest.errorCallback) {
                subRequest.errorCallback.call(subRequest.context, xhr, textStatus, errorThrown);
              }
            }, this);
          }).always(function (xhr) {
              _request.subRequests.forEach(function (subRequest) {
                subRequest.completeCallback.call(subRequest.context, xhr);
              }, this);
            });
      })(bulks[id]);
    }
  }
});
