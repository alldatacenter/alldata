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
var chartUtils = require('utils/chart_utils');

App.GaugeWidgetView = Em.View.extend(App.WidgetMixin, {
  templateName: require('templates/common/widget/gauge_widget'),

  /**
   * @type {string}
   */
  value: '',

  /**
   * common metrics container
   * @type {Array}
   */
  metrics: [],

  /**
   * 1 - is maximum value of a gauge
   * @type {number}
   * @const
   */
  MAX_VALUE: 1,
  /**
   * 0 - is minimum value of a gauge
   * @type {number}
   * @const
   */
  MIN_VALUE: 0,

  /**
   * @type {boolean}
   */
  isUnavailable: function () {
    return isNaN(parseFloat(this.get('value'))) || this.get('isOverflowed');
  }.property('value', 'isOverflowed'),

  /**
   * @type {boolean}
   */
  isOverflowed: function () {
    return parseFloat(this.get('value')) > this.get('MAX_VALUE') || parseFloat(this.get('value')) < this.get('MIN_VALUE');
  }.property('value'),

  chartView: App.ChartPieView.extend({
    stroke: '#transparent',
    innerR: 40,

    /**
     * since chart widget using percentage values factor equal 100
     * @type {number}
     * @const
     */
    FACTOR: 100,

    /**
     * 100 - is maximum percentage value
     * @type {number}
     * @const
     */
    MAX_VALUE: 100,

    warningThreshold: Em.computed.alias('parentView.content.properties.warning_threshold'),

    errorThreshold: Em.computed.alias('parentView.content.properties.error_threshold'),

    id: Em.computed.format('gauge-widget-{0}', 'parentView.content.id'),

    existCenterText: true,
    centerTextColor: Em.computed.alias('contentColor'),

    palette: new Rickshaw.Color.Palette({
      scheme: chartUtils.getColorSchemeForGaugeWidget()
    }),

    data: function () {
      var data = parseFloat(this.get('parentView.value')) * this.get('FACTOR');
      if (isNaN(data) || data > this.get('MAX_VALUE')) return [0, this.get('MAX_VALUE')];
      return [data.toFixed(0), this.get('MAX_VALUE') - data.toFixed(0)];
    }.property('parentView.value'),

    contentColor: function () {
      var used = parseFloat(this.get('parentView.value')) * this.get('FACTOR');
      var threshold1 = parseFloat(this.get('warningThreshold')) * this.get('FACTOR');
      var threshold2 = parseFloat(this.get('errorThreshold')) * this.get('FACTOR');
      var color_green = App.healthStatusGreen;
      var color_red = App.healthStatusRed;
      var color_orange = App.healthStatusOrange;
      if ((isNaN(threshold1) && isNaN(threshold2)) || (isNaN(threshold1) && used <= threshold2) || (isNaN(threshold2) && used <= threshold1) || (!isNaN(threshold2) && (threshold1 > threshold2) && (used > threshold1)) || (!isNaN(threshold2) && (threshold1 < threshold2) && (used <= threshold1))) {
        this.set('palette', new Rickshaw.Color.Palette({
          scheme: chartUtils.getColorSchemeForGaugeWidget(color_green)
        }));
      } else if ((!isNaN(threshold2) && used.isInRange(threshold1, threshold2)) || (isNaN(threshold2) && used > threshold1)) {
        this.set('palette', new Rickshaw.Color.Palette({
          scheme: chartUtils.getColorSchemeForGaugeWidget(color_orange)
        }));
      } else {
        this.set('palette', new Rickshaw.Color.Palette({
          scheme: chartUtils.getColorSchemeForGaugeWidget(color_red)
        }));
      }
      return App.widgetContentColor;
    }.property('parentView.value', 'warningThreshold', 'errorThreshold'),

    // refresh text and color when data in model changed
    refreshSvg: function () {
      // remove old svg
      var old_svg = $("#" + this.get('id'));
      if (old_svg) {
        old_svg.remove();
      }

      // draw new svg
      this.appendSvg();
    }.observes('parentView.value', 'warningThreshold', 'errorThreshold')
  })
});