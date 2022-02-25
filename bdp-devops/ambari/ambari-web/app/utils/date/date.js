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

var validator = require('utils/validator');
var App = require('app');

module.exports = {

  /**
   * List of monthes short names
   * @type {string[]}
   */
  dateMonths: ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'],

  /**
   * List of days short names
   * @type {string[]}
   */
  dateDays: ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'],

  /**
   * Add leading zero
   *
   * @param {string} time
   * @returns {string}
   * @method dateFormatZeroFirst
   */
  dateFormatZeroFirst: function (time) {
    return (time < 10 ? '0' : '') + time;
  },

  /**
   * Convert timestamp to date-string
   * default format - 'DAY_OF_THE_WEEK, MONTH DAY, YEAR HOURS:MINUTES'
   *
   * @param {number} timestamp
   * @param {bool} format
   * @return {*} date
   * @method dateFormat
   */
  dateFormat: function (timestamp, format) {
    if (!validator.isValidInt(timestamp)) {
      return timestamp;
    }
    format = format || 'ddd, MMM DD, YYYY HH:mm';

    return moment((new Date(timestamp))).format(format);
  },

  /**
   * Convert timestamp to date-string 'DAY_OF_THE_WEEK MONTH DAY YEAR'
   *
   * @param {string} timestamp
   * @return {string}
   * @method dateFormatShort
   */
  dateFormatShort: function (timestamp) {
    if (!validator.isValidInt(timestamp)) {
      return timestamp;
    }
    var format = 'ddd MMM DD YYYY';
    var date = moment((new Date(timestamp))).format(format);
    var today = moment((new Date())).format(format);
    if (date === today) {
      return 'Today ' + (new Date(timestamp)).toLocaleTimeString();
    }
    return date;
  },

  startTime: function (startTimestamp) {
    return this._time(startTimestamp, 'Not started');
  },

  endTime: function (endTimestamp) {
    return this._time(endTimestamp, 'Not finished');
  },

  /**
   * Convert timestamp to 'DAY_OF_THE_WEEK, MONTH DAY, YEAR HOURS:MINUTES', except for the case: year equals 1969
   *
   * @param {string} timestamp
   * @param {string} msg
   * @return {string} TimeSummary
   */
  _time: function (timestamp, msg) {
    if (!validator.isValidInt(timestamp)) {
      return '';
    }
    var startDate = new Date(timestamp);
    var months = this.dateMonths;
    var days = this.dateDays;
    if (startDate.getFullYear() === 1969 || timestamp < 1) {
      return msg;
    }
    var startTimeSummary = '';
    if (new Date(timestamp).setHours(0, 0, 0, 0) === new Date().setHours(0, 0, 0, 0)) { //today
      startTimeSummary = 'Today ' + this.dateFormatZeroFirst(startDate.getHours()) + ':' + this.dateFormatZeroFirst(startDate.getMinutes());
    } else {
      startTimeSummary = days[startDate.getDay()] + ' ' + months[startDate.getMonth()] + ' ' +
        this.dateFormatZeroFirst(startDate.getDate()) + ' ' + startDate.getFullYear() + ' '
        + this.dateFormatZeroFirst(startDate.getHours()) + ':' + this.dateFormatZeroFirst(startDate.getMinutes());
    }
    return startTimeSummary;
  },

  /**
   * Provides the duration between the given start and end timestamp. If start time
   * not valid, duration will be ''. If end time is not valid, duration will
   * be till now, showing 'Lasted for xxx secs'.
   *
   * @param {string} startTimestamp
   * @param {string} endTimestamp
   * @return {string} durationSummary
   * @method durationSummary
   */
  durationSummary: function (startTimestamp, endTimestamp) {
    // generate duration
    var durationSummary = '';
    var startDate = new Date(startTimestamp);
    var endDate = new Date(endTimestamp);
    if (startDate.getFullYear() == 1969 || startTimestamp < 1) {
      // not started
      return Em.I18n.t('common.na');
    }
    if (endDate.getFullYear() != 1969 && endTimestamp > 0) {
      durationSummary = '' + this.timingFormat(endTimestamp - startTimestamp);
      return durationSummary.contains('-') ? Em.I18n.t('common.na') : durationSummary; //lasted for xx secs
    } else {
      // still running, duration till now
      var time = (App.dateTimeWithTimeZone() - startTimestamp) < 0 ? 0 : (App.dateTimeWithTimeZone() - startTimestamp);
      durationSummary = '' + this.timingFormat(time);
    }
    return durationSummary.contains('-') ? Em.I18n.t('common.na') : durationSummary;
  },

  /**
   * Format: "{#days}d {#hours}h {#minutes}m {#seconds}s"
   * Example: "1d 3h 46m"
   *
   * Display optimization rules:
   *   if time more than a day then hide time lower than minute
   *   if time more than a minute and less than an hour then hide time lower than second
   * @param {number|string} time
   * @return {string|null} formatted date
   * @method timingFormat
   */
  timingFormat: function (time) {
    if (Em.isNone(time)) {
      return null;
    }

    time = parseInt(time);
    const fullTime = time;
    let duration = '';

    if (time === 0) {
      return '0s';
    }

    const oneSecMs = 1000;
    const oneMinMs = 60000;
    const oneHourMs = 3600000;
    const oneDayMs = 86400000;
    let days, hours, minutes, seconds;

    [days, time] = this.extractTimeUnit(time, oneDayMs, 'd');
    [hours, time] = this.extractTimeUnit(time, oneHourMs, 'h');
    [minutes, time] = this.extractTimeUnit(time, oneMinMs, 'm');
    duration += days + hours + minutes;
    if (fullTime < oneDayMs) {
      [seconds, time] = this.extractTimeUnit(time, oneSecMs, 's');
      duration += seconds;
      if (fullTime < oneSecMs) {
        duration += '1s';
      }
    }

    return duration.trim();
  },

  extractTimeUnit: function (time, unitValue, unitSuffix) {
    let result = '';
    if (time >= unitValue) {
      result = Math.floor(time / unitValue) + `${unitSuffix} `;
      time -= Math.floor(time / unitValue) * unitValue;
    }
    return [result, time];
  },

  /**
   * Provides the duration between the given start and end time. If start time
   * is not given, duration will be 0. If end time is not given, duration will
   * be till now.
   *
   * @param {Number} startTime Start time from epoch
   * @param {Number} endTime End time from epoch
   * @return {Number} duration
   * @method duration
   */
  duration: function (startTime, endTime) {
    var duration = 0;
    if (startTime && startTime > 0) {
      if (!endTime || endTime < 1) {
        endTime = App.dateTime();
      }
      duration = endTime - startTime;
    }
    return duration;
  }

};
