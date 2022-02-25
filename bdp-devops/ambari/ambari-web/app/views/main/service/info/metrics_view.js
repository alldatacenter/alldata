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
var misc = require('utils/misc');
require('views/main/service/service');
require('data/service_graph_config');

App.MainServiceInfoMetricsView = Em.View.extend(App.Persist, App.TimeRangeMixin, {
  templateName: require('templates/main/service/info/metrics'),
  /**
   * @property {Number} chunkSize - number of columns in Metrics section
   */
  chunkSize: 5,

  service: null,

  svc: function () {
    var svc = this.get('controller.content');
    var svcName = svc.get('serviceName');
    if (svcName) {
      switch (svcName.toLowerCase()) {
        case 'hdfs':
          svc = App.HDFSService.find().objectAt(0);
          break;
        case 'yarn':
          svc = App.YARNService.find().objectAt(0);
          break;
        case 'hbase':
          svc = App.HBaseService.find().objectAt(0);
          break;
        case 'flume':
          svc = App.FlumeService.find().objectAt(0);
          break;
        default:
          break;
      }
    }
    return svc;
  }.property('controller.content.serviceName').volatile(),

  getServiceModel: function (serviceName) {
    var extended = App.Service.extendedModel[serviceName];
    if (extended) {
      return App[extended].find().objectAt(0);
    }
    return App.Service.find(serviceName);
  },

  serviceName: Em.computed.alias('service.serviceName'),

  /**
   * Contains graphs for this particular service
   */
  serviceMetricGraphs: [],

  /**
   * @type {boolean}
   * @default false
   */
  serviceHasMetrics: false,

  /**
   * Key-name to store time range in Persist
   * @type {string}
   */
  persistKey: Em.computed.format('time-range-service-{0}', 'service.serviceName'),

  isLoading: false,

  didInsertElement: function () {
    var svcName = this.get('controller.content.serviceName');
    this.set('service', this.getServiceModel(svcName));
    var isMetricsSupported = svcName !== 'STORM' || App.get('isStormMetricsSupported');

    this.loadActiveWidgetLayout();

    if (App.get('supports.customizedWidgetLayout')) {
      this.get('controller').loadWidgetLayouts();
    }

    if (svcName && isMetricsSupported) {
      var allServices = require('data/service_graph_config');
      this.constructGraphObjects(allServices[svcName.toLowerCase()]);
    }
    this.makeSortable('#widget_layout');
    this.makeSortable('#ns_widget_layout', true);
    this.addWidgetTooltip();
    this._super();
  },

  loadActiveWidgetLayout: function () {
    var isHDFS = this.get('controller.content.serviceName') === 'HDFS';
    if (!this.get('isLoading') && (!isHDFS || (isHDFS && App.router.get('clusterController.isHostComponentMetricsLoaded')))) {
      this.set('isLoading', true);
      this.get('controller').getActiveWidgetLayout();
    }
  }.observes('App.router.clusterController.isHostComponentMetricsLoaded'),

  addWidgetTooltip: function() {
    Em.run.later(this, function () {
      App.tooltip($("[rel='add-widget-tooltip']"));
      // enable description show up on hover
      $('.img-thumbnail').hoverIntent(function() {
        if ($(this).is('hover')) {
          $(this).find('.hidden-description').delay(1000).fadeIn(200).end();
        }
      }, function() {
        $(this).find('.hidden-description').stop().hide().end();
      });
    }, 1000);
  },

  willDestroyElement: function() {
    $("[rel='add-widget-tooltip']").tooltip('destroy');
    $('.img-thumbnail').off();
    $('#widget_layout').sortable('destroy');
    $('#ns_widget_layout').sortable('destroy');
    $('.widget.span2p4').detach().remove();
    this.get('serviceMetricGraphs').clear();
    this.set('service', null);
  },

   /*
   * Find the graph class associated with the graph name, and split
   * the array into sections of 5 for displaying on the page
   * (will only display rows with 5 items)
   */
  constructGraphObjects: function (graphNames) {
    var self = this,
        stackService = App.StackService.find(this.get('controller.content.serviceName'));

    if (!graphNames && !stackService.get('isServiceWithWidgets')) {
      this.get('serviceMetricGraphs').clear();
      this.set('serviceHasMetrics', false);
      return;
    }

    // load time range(currentTimeRangeIndex) for current service from server
    this.getUserPref(self.get('persistKey')).complete(function () {
      var result = [], graphObjects = [], chunkSize = self.get('chunkSize');
      if (graphNames) {
        graphNames.forEach(function (graphName) {
          graphObjects.push(App["ChartServiceMetrics" + graphName].extend());
        });
      }
      while (graphObjects.length) {
        result.push(graphObjects.splice(0, chunkSize));
      }
      self.set('serviceMetricGraphs', result);
      self.set('serviceHasMetrics', true);
    });
  },

  getUserPrefSuccessCallback: function (response, request) {
    if (response) {
      this.set('currentTimeRangeIndex', response);
    }
  },

  getUserPrefErrorCallback: function (request) {
    if (request.status === 404) {
      this.postUserPref(this.get('persistKey'), 0);
      this.set('currentTimeRangeIndex', 0);
    }
  },

  /**
   * list of static actions of widget
   * @type {Array}
   */
  staticGeneralWidgetActions: [
    Em.Object.create({
      label: Em.I18n.t('dashboard.widgets.actions.browse'),
      class: 'glyphicon glyphicon-th',
      action: 'goToWidgetsBrowser',
      isAction: true
    })
  ],

  /**
   *list of static actions of widget accessible to Admin/Operator privelege
   * @type {Array}
   */

  staticAdminPrivelegeWidgetActions: [
    Em.Object.create({
      label: Em.I18n.t('dashboard.widgets.create'),
      class: 'glyphicon glyphicon-plus',
      action: 'createWidget',
      isAction: true
    })
  ],

  /**
   * List of static actions related to widget layout
   */
  staticWidgetLayoutActions: [
    Em.Object.create({
      label: Em.I18n.t('dashboard.widgets.layout.save'),
      class: 'glyphicon glyphicon-download-alt',
      action: 'saveLayout',
      isAction: true
    }),
    Em.Object.create({
      label: Em.I18n.t('dashboard.widgets.layout.import'),
      class: 'glyphicon glyphicon-file',
      isAction: true,
      layouts: App.WidgetLayout.find()
    })
  ],

  /**
   * @type {Array}
   */
  widgetActions: function() {
    var options = [];
    if (App.isAuthorized('SERVICE.MODIFY_CONFIGS')) {
      if (App.supports.customizedWidgetLayout) {
        options.pushObjects(this.get('staticWidgetLayoutActions'));
      }
      options.pushObjects(this.get('staticAdminPrivelegeWidgetActions'));
    }
    options.pushObjects(this.get('staticGeneralWidgetActions'));
    return options;
  }.property(''),

  /**
   * call action function defined in controller
   * @param event
   */
  doWidgetAction: function(event) {
    if($.isFunction(this.get('controller')[event.context])) {
      this.get('controller')[event.context].apply(this.get('controller'));
    }
  },

  /**
   * onclick handler for a time range option
   * @param {object} event
   */
  setTimeRange: function (event) {
    var graphs = this.get('controller.widgets').filterProperty('widgetType', 'GRAPH'),
      callback = function () {
        graphs.forEach(function (widget) {
          widget.set('properties.time_range', event.context.value);
        });
      };
    this._super(event, callback);

    // Preset time range is specified by user
    if (event.context.value !== '0') {
      callback();
    }
  },

  /**
   * Define if some widget is currently moving
   * @type {boolean}
   */
  isMoving: false,

  /**
   * Make widgets' list sortable on New Dashboard style
   */
  makeSortable: function (selector, isNSLayout) {
    var self = this;
    var controller = this.get('controller');
    $('html').on('DOMNodeInserted', selector, function () {
      $(this).sortable({
        items: "> div",
        cursor: "move",
        tolerance: "pointer",
        scroll: false,
        update: function () {
          var layout = isNSLayout ? controller.get('selectedNSWidgetLayout') : controller.get('activeWidgetLayout');
          var widgets = misc.sortByOrder($(selector + " .widget").map(function () {
            return this.id;
          }), layout.get('widgets').toArray());
          controller.saveWidgetLayout(widgets, layout);
        },
        activate: function () {
          self.set('isMoving', true);
        },
        deactivate: function () {
          self.set('isMoving', false);
        }
      }).disableSelection();
      $('html').off('DOMNodeInserted', selector);
    });
  }
});
