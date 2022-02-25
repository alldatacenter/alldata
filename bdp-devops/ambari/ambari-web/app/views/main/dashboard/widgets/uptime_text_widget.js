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
var date = require('utils/date/date');

App.UptimeTextDashboardWidgetView = App.TextDashboardWidgetView.extend({

  templateName: require('templates/main/dashboard/widgets/uptime'),

  timeUnit: null,

  hiddenInfoClass: "hidden-info-three-line",

  maxValue: 'infinity',

  component: null,

  /**
   * Value received from model that's used to calculate data and content
   * Should be defined in every child
   */
  modelValue: null,

  data: null,

  content: Em.computed.alias('data'),

  isGreen: function () {
    return !Em.isNone(this.get('data'));
  }.property('data'),

  isOrange: false,

  isRed: false,

  timeConverter: function (timestamp) {
    var m = moment(new Date(timestamp));
    return [m.format('ddd MMM DD YYYY'), m.format('HH:mm:ss')];
  },

  /**
   * All children should have such code
   * <code>
   * didInsertElement: function() {
   *   this._super();
   *   this.calc();
   * }
   * </code>
   */
  didInsertElement: function () {
    this._super();
    this.calculate();
    this.addObserver('modelValue', this, this.calculate);
  },

  calculate: function () {
    const uptime = this.get('modelValue');
    if (uptime) {
      let diff = App.dateTimeWithTimeZone() - App.dateTimeWithTimeZone(uptime);
      diff = diff < 0 ? 0 : diff;
      const formatted = date.timingFormat(diff);
      const uptimeString = this.timeConverter(uptime);

      this.setProperties({
        data: formatted,
        hiddenInfo: [formatted, uptimeString[0], uptimeString[1]]
      });
    } else {
      this.setProperties({
        data: null,
        hiddenInfo: [this.get('component'), Em.I18n.t('services.service.summary.notRunning')]
      });
    }
  }
});
