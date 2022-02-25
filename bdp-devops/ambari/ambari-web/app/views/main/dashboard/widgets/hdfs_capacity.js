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
var numberUtils = require('utils/number_utils');

App.NameNodeCapacityPieChartView = App.PieChartDashboardWidgetView.extend({

  modelValueMax: Em.computed.alias('model.capacityTotal'),
  /**
   * HDFS model has 'remaining' value, but not 'used'
   */
  modelValueUsed: Em.computed.alias('model.capacityRemaining'),
  modelValueCapacityUsed: Em.computed.alias('model.capacityUsed'),
  modelValueNonDfsUsed: Em.computed.alias('model.capacityNonDfsUsed'),
  widgetHtmlId: Em.computed.format('widget-nn-capacity'),
  hiddenInfoClass: "hidden-info-six-line",

  didInsertElement: function() {
    this._super();
    this.calc();
  },

  calcHiddenInfo: function () {
    var total = this.get('modelValueMax') + 0;
    var remaining = this.get('modelValueUsed');
    var dfsUsed = this.get('modelValueCapacityUsed');
    var nonDfsUsed = this.get('modelValueNonDfsUsed');
    var dfsPercent = total > 0 ? (dfsUsed * 100 / total).toFixed(2) : 0;
    var nonDfsPercent = total > 0 ? (nonDfsUsed * 100 / total).toFixed(2) : 0;
    var remainingPercent = total > 0 ? (remaining * 100 / total).toFixed(2) : 0;
    if (dfsPercent === "NaN" || dfsPercent < 0) {
      dfsPercent = Em.I18n.t('services.service.summary.notAvailable') + " ";
    }
    if (nonDfsPercent === "NaN" || nonDfsPercent < 0) {
      nonDfsPercent = Em.I18n.t('services.service.summary.notAvailable') + " ";
    }
    if (remainingPercent === "NaN" || remainingPercent < 0) {
      remainingPercent = Em.I18n.t('services.service.summary.notAvailable') + " ";
    }
    return [
      Em.I18n.t('dashboard.widgets.HDFSDiskUsage.DFSused'),
      Em.I18n.t('dashboard.widgets.HDFSDiskUsage.info').format(numberUtils.bytesToSize(dfsUsed, 1, 'parseFloat'), dfsPercent),
      Em.I18n.t('dashboard.widgets.HDFSDiskUsage.nonDFSused'),
      Em.I18n.t('dashboard.widgets.HDFSDiskUsage.info').format(numberUtils.bytesToSize(nonDfsUsed, 1, 'parseFloat'), nonDfsPercent),
      Em.I18n.t('dashboard.widgets.HDFSDiskUsage.remaining'),
      Em.I18n.t('dashboard.widgets.HDFSDiskUsage.info').format(numberUtils.bytesToSize(remaining, 1, 'parseFloat'), remainingPercent)
    ];
  },

  calcDataForPieChart: function() {
    var total = this.get('modelValueMax') * 1024 * 1024;
    var used = total - this.get('modelValueUsed') * 1024 * 1024;
    var percent = total > 0 ? (used * 100 / total).toFixed() : 0;
    var percentPrecise = total > 0 ? (used * 100 / total).toFixed(1) : 0;
    return [percent, percentPrecise];
  }

});
