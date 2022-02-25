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

require('views/common/time_range_list');
var timeRangePopup = require('views/common/custom_date_popup');

App.TimeRangeMixin = Em.Mixin.create({

  timeRangeClassName: 'pull-right',

  /**
   * time range options for service metrics, a dropdown will list all options
   * value set in hours
   */
  timeRangeOptions: [
    {index: 0, name: Em.I18n.t('graphs.timeRange.hour'), value: '1'},
    {index: 1, name: Em.I18n.t('graphs.timeRange.twoHours'), value: '2'},
    {index: 2, name: Em.I18n.t('graphs.timeRange.fourHours'), value: '4'},
    {index: 3, name: Em.I18n.t('graphs.timeRange.twelveHours'), value: '12'},
    {index: 4, name: Em.I18n.t('graphs.timeRange.day'), value: '24'},
    {index: 5, name: Em.I18n.t('graphs.timeRange.week'), value: '168'},
    {index: 6, name: Em.I18n.t('graphs.timeRange.month'), value: '720'},
    {index: 7, name: Em.I18n.t('graphs.timeRange.year'), value: '8760'},
    {index: 8, name: Em.I18n.t('common.custom'), value: '0'}
  ],

  currentTimeRangeIndex: 0,

  currentTimeRange: function() {
    return this.get('timeRangeOptions').objectAt(this.get('currentTimeRangeIndex'));
  }.property('currentTimeRangeIndex'),

  isCustomTimeRange: function () {
    return !(Em.isNone(this.get('customStartTime')) || Em.isNone(this.get('customEndTime')));
  }.property('customStartTime', 'customEndTime'),

  customStartTime: null,

  customEndTime: null,

  customDurationFormatted: null,

  customStartTimeFormatted: function () {
    var time = this.get('customStartTime');
    return Em.isNone(time) ? '' : App.formatDateTimeWithTimeZone(time, 'M/D/YYYY hh:mmA');
  }.property('customStartTime'),

  customEndTimeFormatted: function () {
    var time = this.get('customEndTime');
    return Em.isNone(time) ? '' : App.formatDateTimeWithTimeZone(time, 'M/D/YYYY hh:mmA');
  }.property('customEndTime'),

  tooltipMessage: function () {
    var message;
    if (this.get('isCustomTimeRange')) {
      message = Em.I18n.t('common.start') + ': ' + this.get('customStartTimeFormatted') + '<br>' + Em.I18n.t('common.duration') + ': ' + this.get('customDurationFormatted') + '<br>' + Em.I18n.t('common.end') + ': ' + this.get('customEndTimeFormatted');
    } else {
      message = '';
    }
    return message;
  }.property('customStartTimeFormatted', 'customEndTimeFormatted', 'customDurationFormatted'),

  didInsertElement: function () {
    App.tooltip(this.$(), {
      selector: '.dropdown-toggle[rel="tooltip"]',
      html: true,
      placement: 'left'
    });
  },

  /**
   * onclick handler for a time range option
   * @param {object} event
   * @param {function} callback
   * @param {object} context
   */
  setTimeRange: function (event, callback, context) {
    var prevCustomTimeRange = this.getProperties(['currentTimeRangeIndex', 'customStartTime', 'customEndTime', 'customDurationFormatted']),
      index = event.context.index,
      primary = function () {
        var timeRange = {
          customEndTime: timeRangePopup.endTime,
          customStartTime: timeRangePopup.startTime,
          customDurationFormatted: timeRangePopup.customDuration
        };
        if (callback) {
          callback();
        }
        this.set('currentTimeRangeIndex', index);
        this.setProperties(timeRange);
        if (context) {
          context.setProperties(timeRange);
        }
      },
      secondary = this.setProperties.bind(this, prevCustomTimeRange);

    if (index === 8) {
      // Custom start and end time is specified by user
      var defaultStartTime,
        defaultEndTime,
        duration;
      if (prevCustomTimeRange.currentTimeRangeIndex === 8) {
        // Custom time range is active
        defaultStartTime = new Date(this.get('customStartTime')).getTime();
        defaultEndTime = new Date(this.get('customEndTime')).getTime();
        duration = this.get('customDurationFormatted') || 0;
      } else {
        // Preset time range is active
        var minutes;
        duration = this.get('currentTimeRange.value') * 3600000;
        defaultEndTime = new Date();
        minutes = 5 * Math.ceil(defaultEndTime.getMinutes() / 5);
        defaultEndTime.setMinutes(minutes);
        defaultStartTime = defaultEndTime.getTime() - duration;
      }
      timeRangePopup.showCustomDatePopup(primary.bind(this), secondary, {
        startDate: App.formatDateTimeWithTimeZone(defaultStartTime, 'MM/DD/YYYY'),
        hoursForStart: App.formatDateTimeWithTimeZone(defaultStartTime, 'hh'),
        minutesForStart: App.formatDateTimeWithTimeZone(defaultStartTime, 'mm'),
        middayPeriodForStart: App.formatDateTimeWithTimeZone(defaultStartTime, 'A'),
        duration: duration,
        endDate: App.formatDateTimeWithTimeZone(defaultEndTime, 'MM/DD/YYYY'),
        hoursForEnd: App.formatDateTimeWithTimeZone(defaultEndTime, 'hh'),
        minutesForEnd: App.formatDateTimeWithTimeZone(defaultEndTime, 'mm'),
        middayPeriodForEnd: App.formatDateTimeWithTimeZone(defaultEndTime, 'A')
      });
    } else {
      // Preset time range is specified by user
      this.setProperties({
        currentTimeRangeIndex: index,
        customStartTime: null,
        customEndTime: null,
        customDurationFormatted: null
      });
    }

  },

  timeRangeListView: App.TimeRangeListView.extend()

});
