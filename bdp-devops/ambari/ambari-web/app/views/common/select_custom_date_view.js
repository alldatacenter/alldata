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

App.JobsCustomDatesSelectView = Em.View.extend({

  name: 'jobsCustomDatesSelectView',

  templateName: require('templates/common/custom_date_popup'),

  middayPeriodOptions: [Em.I18n.t('jobs.table.custom.date.am'), Em.I18n.t('jobs.table.custom.date.pm')],

  hourOptions: ['01', '02', '03', '04', '05', '06', '07', '08', '09', '10', '11', '12'],

  minuteOptions: ['00', '05', '10', '15', '20', '25', '30', '35', '40', '45', '50', '55'],

  customDateFormFields: Ember.Object.create({
    startDate: null,
    hoursForStart: null,
    minutesForStart: null,
    middayPeriodForStart: null,
    duration: null,
    endDate: null,
    hoursForEnd: null,
    minutesForEnd: null,
    middayPeriodForEnd: null
  }),

  errors: Ember.Object.create({
    isStartDateError: false,
    isEndDateError: false
  }),

  errorMessages: Ember.Object.create({
    startDate: '',
    endDate: ''
  }),

  durationSelect: Em.Select.extend({
    classNames: ['input-medium'],
    content: [
      {
        value: 900000,
        label: Em.I18n.t('jobs.customDateFilter.duration.15min')
      },
      {
        value: 1800000,
        label: Em.I18n.t('jobs.customDateFilter.duration.30min')
      },
      {
        value: 3600000,
        label: Em.I18n.t('jobs.customDateFilter.duration.1hr')
      },
      {
        value: 7200000,
        label: Em.I18n.t('jobs.customDateFilter.duration.2hr')
      },
      {
        value: 14400000,
        label: Em.I18n.t('jobs.customDateFilter.duration.4hr')
      },
      {
        value: 43200000,
        label: Em.I18n.t('jobs.customDateFilter.duration.12hr')
      },
      {
        value: 86400000,
        label: Em.I18n.t('jobs.customDateFilter.duration.24hr')
      },
      {
        value: 604800000,
        label: Em.I18n.t('jobs.customDateFilter.duration.1w')
      },
      {
        value: 2592000000,
        label: Em.I18n.t('jobs.customDateFilter.duration.1m')
      },
      {
        value: 31536000000,
        label: Em.I18n.t('jobs.customDateFilter.duration.1yr')
      },
      {
        value: 0,
        label: Em.I18n.t('common.custom')
      }
    ],
    optionValuePath: 'content.value',
    optionLabelPath: 'content.label',
    selectionBinding: 'parentView.customDateFormFields.duration',
    willInsertElement: function () {
      var duration = this.get('selection'),
        initialOption = this.get('content').find(function (item) {
          return duration === item.value || duration === item.label;
        }, this);
      if (!initialOption) {
        initialOption = this.get('content').findProperty('value', 0);
      }
      this.set('selection', initialOption);
    }
  }),

  isCustomEndDate: Em.computed.equal('customDateFormFields.duration.value', 0),

  didInsertElement: function () {
    $('.datepicker').datepicker({
      format: 'mm/dd/yyyy'
    }).on('changeDate', function() {
      $(this).datepicker('hide');
    });
  },

  createCustomStartDate: function () {
    var startDate = this.get('customDateFormFields.startDate');
    var hoursForStart = this.get('customDateFormFields.hoursForStart');
    var minutesForStart = this.get('customDateFormFields.minutesForStart');
    var middayPeriodForStart = this.get('customDateFormFields.middayPeriodForStart');
    if (startDate && hoursForStart && minutesForStart && middayPeriodForStart) {
      return App.getTimeStampFromLocalTime(new Date(startDate + ' ' + hoursForStart + ':' + minutesForStart + ' ' + middayPeriodForStart));
    }
    return null;
  },

  createCustomEndDate: function (startDate) {
    var duration = this.get('customDateFormFields.duration.value'),
      date;
    if (duration === 0) {
      var endDate = this.get('customDateFormFields.endDate');
      var hoursForEnd = this.get('customDateFormFields.hoursForEnd');
      var minutesForEnd = this.get('customDateFormFields.minutesForEnd');
      var middayPeriodForEnd = this.get('customDateFormFields.middayPeriodForEnd');
      if (endDate && hoursForEnd && minutesForEnd && middayPeriodForEnd) {
        date = App.getTimeStampFromLocalTime(new Date(endDate + ' ' + hoursForEnd + ':' + minutesForEnd + ' ' + middayPeriodForEnd));
      }
    } else if (!Em.isNone(startDate)) {
      date = startDate + duration;
    }
    return date;
  },

  setErrorMessage: function (key, message) {
    var errors = this.get('errors'),
      errorMessages = this.get('errorMessages'),
      isError = !Em.isNone(message);
    message = isError ? message: '';
    errors.set('is' + key.capitalize() + 'Error', isError);
    errorMessages.set(key, message);
  },

  validate: function () {
    var hasErrors = false,
      formFields = this.get('customDateFormFields'),
      errors = this.get('errors'),
      errorMessages = this.get('errorMessages');

    // Check if fields are empty or invalid
    Em.keys(errorMessages).forEach(function (key) {
      var value = formFields.get(key),
        timestamp = App.getTimeStampFromLocalTime(value);
      if (key !== 'endDate' || this.get('isCustomEndDate')) {
        if (!formFields.get(key)) {
          hasErrors = true;
          this.setErrorMessage(key);
        } else if (isNaN(timestamp)) {
          this.setErrorMessage(key, Em.I18n.t('jobs.customDateFilter.error.incorrect'));
          hasErrors = true;
        } else {
          this.setErrorMessage(key);
        }
      }
    }, this);

    // Check that endDate is after startDate
    if (!hasErrors) {
      var startDate = this.createCustomStartDate(),
        endDate = this.createCustomEndDate(startDate);
      if (startDate && endDate) {
        if (startDate > endDate) {
          hasErrors = true;
          this.setErrorMessage('endDate', Em.I18n.t('jobs.customDateFilter.error.date.order'));
        }
        if (startDate > new Date().getTime()) {
          hasErrors = true;
          this.setErrorMessage('startDate', Em.I18n.t('jobs.customDateFilter.error.laterThanNow'));
        }
      }
    }

    this.set('parentView.disablePrimary', hasErrors);

    // Get customized time range if there are no errors
    if (!hasErrors) {
      var duration;
      if (this.get('isCustomEndDate')) {
        var values = [
            {
              label: 'year',
              max: Infinity
            },
            {
              label: 'month',
              max: 12
            },
            {
              label: 'week',
              max: 4
            },
            {
              label: 'day',
              max: 7
            },
            {
              label: 'hour',
              max: 24
            },
            {
              label: 'minute',
              max: 60
            }
          ],
          start = moment(startDate),
          end = moment(endDate);
        values.forEach(function (item) {
          var diff = end.diff(start, item.label);
          item.value = diff >= item.max ? diff % item.max : diff;
        });
        duration = values.filterProperty('value').map(function (item) {
          return item.value + ' ' + stringUtils.pluralize(item.value, item.label);
        }).join(' ');
      } else {
        duration = this.get('customDateFormFields.duration.label');
      }
      this.get('controller').setProperties({
        startTime: startDate,
        endTime: endDate,
        customDuration: duration
      });
    }

  }.observes('customDateFormFields.startDate', 'customDateFormFields.hoursForStart', 'customDateFormFields.minutesForStart', 'customDateFormFields.middayPeriodForStart', 'customDateFormFields.endDate', 'customDateFormFields.hoursForEnd', 'customDateFormFields.minutesForEnd', 'customDateFormFields.middayPeriodForEnd', 'customDateFormFields.duration.value')
});
