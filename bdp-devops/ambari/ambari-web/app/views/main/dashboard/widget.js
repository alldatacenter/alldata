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

App.DashboardWidgetView = Em.View.extend({

  /**
   * @type {string}
   * @default null
   */
  title: Em.computed.alias('widget.title'),

  templateName: null,

  sourceName: Em.computed.alias('widget.sourceName'),

  /**
   * Bound from template
   *
   * @type {object}
   */
  widget: null,

  /**
   * Indicates whether dropdown with Edit/Delete/Save actions should be displayed
   * @type {boolean}
   */
  showActions: true,

  /**
   * @type {Em.View}
   */
  widgetsView: Em.computed.alias('parentView'),

  /**
   * @type {object} - record from model that serve as data source
   */
  model: function () {
    var model = Em.Object.create();
    if (Em.isNone(this.get('sourceName'))) {
      return model;
    }
    return this.findModelBySource(this.get('sourceName'));
  }.property('sourceName'),

  /**
   * @type {number}
   * @default null
   */
  id: Em.computed.alias('widget.id'),

  /**
   * html id bind to view-class: widget-(1)
   * used by re-sort
   * @type {string}
   */
  viewID: Em.computed.format('widget-{0}', 'id'),

  classNames: ['span2p4'],

  attributeBindings: ['viewID'],

  /**
   * widget content pieChart/ text/ progress bar/links/ metrics. etc
   * @type {Array}
   * @default null
   */
  content: null,

  /**
   * more info details
   * @type {Array}
   */
  hiddenInfo: [],

  /**
   * @type {string}
   */
  hiddenInfoClass: "hidden-info-two-line",

  /**
   * @type {string}
   */
  hintInfo: Em.computed.i18nFormat('dashboard.widgets.hintInfo.common', 'maxValue'),

  /**
   * @type {number}
   * @default 0
   */
  thresholdMin: function() {
    return Em.isNone(this.get('widget.threshold')) ? 0 : this.get('widget.threshold')[0];
  }.property('widget.threshold'),

  /**
   * @type {number}
   * @default 0
   */
  thresholdMax: function() {
    return Em.isNone(this.get('widget.threshold')) ? 0 : this.get('widget.threshold')[1];
  }.property('widget.threshold'),

  /**
   * @type {Boolean}
   * @default false
   */
  isDataLoadedBinding: 'App.router.clusterController.isServiceContentFullyLoaded',

  didInsertElement: function () {
    App.tooltip(this.$("[rel='ZoomInTooltip']"), {
      placement: 'left',
      template: '<div class="tooltip"><div class="tooltip-arrow"></div><div class="tooltip-inner graph-tooltip"></div></div>'
    });
  },

  /**
   *
   * @param {string} source
   * @returns {App.Service}
   */
  findModelBySource: function (source) {
    if (source === 'HOST_METRICS' && App.get('services.hostMetrics').length > 0) {
      return App.get('services.hostMetrics');
    }
    var extendedModel = App.Service.extendedModel[source];
    if (extendedModel) {
      return App[extendedModel].find(source);
    }
    return App.Service.find(source);
  },

  willDestroyElement : function() {
    $("[rel='ZoomInTooltip']").tooltip('destroy');
  },

  /**
   * delete widget
   */
  deleteWidget: function () {
    this.get('parentView').hideWidget(this.get('id'), this.get('groupId'), this.get('subGroupId'), this.get('isAllItemsSubGroup'));
  },

  /**
   * Update thresholds for widget and save this them to persist
   *
   * @param {number[]} preparedThresholds
   */
  saveWidgetThresholds(preparedThresholds) {
    const widgetId = this.get('id');
    const widgetIdToNumber = Number(widgetId);
    const widgetsView = this.get('widgetsView');
    const userPreferences = widgetsView.get('userPreferences');
    const isGroupWidget = isNaN(widgetIdToNumber);
    if (isGroupWidget) {
      const parsedWidgetId = parseInt(widgetId);
      if (this.get('isAllItemsSubGroup')) {
        userPreferences.groups[this.get('groupId')]['*'].threshold[this.get('subGroupId')][parsedWidgetId] = preparedThresholds;
      } else {
        userPreferences.groups[this.get('groupId')][this.get('subGroupId')].threshold[parsedWidgetId] = preparedThresholds;
      }
    } else {
      userPreferences.threshold[widgetIdToNumber] = preparedThresholds;
      this.set('widget.threshold', userPreferences.threshold[widgetIdToNumber]);
    }
    widgetsView.saveWidgetsSettings(userPreferences);
  },

  /**
   * edit widget
   */
  editWidget: function () {
    return App.EditDashboardWidgetPopup.show({

      widgetView: this,

      sliderHandlersManager: App.EditDashboardWidgetPopup.DoubleHandlers.create({
        thresholdMin: this.get('thresholdMin'),
        thresholdMax: this.get('thresholdMax'),
        maxValue: parseFloat(this.get('maxValue'))
      }),

      sliderColors: [App.healthStatusGreen, App.healthStatusOrange, App.healthStatusRed]

    });
  }

});
