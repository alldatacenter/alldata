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
var chartUtils = require('utils/chart_utils');

App.PieChartDashboardWidgetView = App.DashboardWidgetView.extend({

  templateName: require('templates/main/dashboard/widgets/pie_chart'),

  maxValue: 100,

  modelValueMax: null,
  modelValueUsed: null,

  hiddenInfo: null,

  isPieExist: null,

  dataForPieChart: null,

  widgetHtmlId: null,

  getUsed: function() {
    return this.get('modelValueUsed') || 0;
  },

  getMax: function() {
    return this.get('modelValueMax') || 0;
  },

  calcHiddenInfo: function() {
    var used = this.getUsed();
    var max = this.getMax();
    var percent = max > 0 ? 100 * used / max : 0;
    var result = [];
    result.pushObject(percent.toFixed(1) + '% used');
    result.pushObject(numberUtils.bytesToSize(used, 1, 'parseFloat', 1024 * 1024) + ' of ' + numberUtils.bytesToSize(max, 1, 'parseFloat', 1024 * 1024));
    return result;
  },

  calcIsPieExists: function() {
    return this.get('modelValueMax') > 0;
  },

  calcDataForPieChart: function() {
    var used = this.getUsed();
    var total = this.getMax();
    var percent = total > 0 ? (used * 100 / total).toFixed() : 0;
    var percentPrecise = total > 0 ? (used * 100 / total).toFixed(1) : 0;
    return [percent, percentPrecise];
  },

  calc: function() {
    this.set('hiddenInfo', this.calcHiddenInfo());
    var isPieExists = this.calcIsPieExists();
    this.set('isPieExist', isPieExists);
    if (isPieExists) {
      this.set('dataForPieChart', this.calcDataForPieChart());
    }
  },

  didInsertElement: function() {
    this._super();
    this.addObserver('modelValueMax', this, this.calc);
    this.addObserver('modelValueUsed', this, this.calc);
  },

  content: App.ChartPieView.extend({
    model: null,  //data bind here
    id: Em.computed.alias('parentView.widgetHtmlId'), // html id
    stroke: 'transparent',
    thresholdMin: null, //bind from parent
    thresholdMax: null,
    innerR: 40,

    existCenterText: true,
    centerTextColor: Em.computed.alias('contentColor'),

    palette: new Rickshaw.Color.Palette({
      scheme: chartUtils.getColorSchemeForGaugeWidget()
    }),

    data: function() {
      var oriData = this.get('parentView.dataForPieChart');
      return [oriData[0], 100 - oriData[0]];
    }.property(),

    setData: function() {
      this.set('data', this.get('parentView.dataForPieChart'));
    }.observes('parentView.dataForPieChart'),

    contentColor: function () {
      var used = parseFloat(this.get('parentView.dataForPieChart')[1]);
      var thresholdMin = parseFloat(this.get('thresholdMin'));
      var thresholdMax = parseFloat(this.get('thresholdMax'));
      if (used <= thresholdMin) {
        this.set('palette', new Rickshaw.Color.Palette({
          scheme: chartUtils.getColorSchemeForGaugeWidget(App.healthStatusGreen)
        }));
      }
      else if (used <= thresholdMax) {
        this.set('palette', new Rickshaw.Color.Palette({
          scheme: chartUtils.getColorSchemeForGaugeWidget(App.healthStatusOrange)
        }));
      }
      else {
        this.set('palette', new Rickshaw.Color.Palette({
          scheme: chartUtils.getColorSchemeForGaugeWidget(App.healthStatusRed)
        }));
      }
      return App.widgetContentColor;

    }.property('data', 'thresholdMin', 'thresholdMax'),

    // refresh text and color when data in model changed
    refreshSvg: function () {
      // remove old svg
      var oldSvg = $("#" + this.get('id'));
      if(oldSvg){
        oldSvg.remove();
      }

      // draw new svg
      this.appendSvg();
    }.observes('data', 'thresholdMin', 'thresholdMax')
  })
});
