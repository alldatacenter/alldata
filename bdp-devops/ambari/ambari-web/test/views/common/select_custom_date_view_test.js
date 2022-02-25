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
require('views/common/select_custom_date_view');

describe('App.JobsCustomDatesSelectView', function () {

  var view;

  beforeEach(function () {
    view = App.JobsCustomDatesSelectView.create();
  });

  describe('#isCustomEndDate', function () {

    var cases = [
      {
        duration: null,
        isCustomEndDate: false,
        title: 'duration not set'
      },
      {
        duration: 1000,
        isCustomEndDate: false,
        title: 'preset duration'
      },
      {
        duration: 0,
        isCustomEndDate: true,
        title: 'custom duration'
      }
    ];

    beforeEach(function () {
      view.reopen({
        validate: Em.K
      });
    });

    cases.forEach(function (item) {
      it(item.title, function () {
        view.set('customDateFormFields.duration', {
          value: item.duration
        });
        expect(view.get('isCustomEndDate')).to.equal(item.isCustomEndDate);
      });
    });

  });

  describe('#createCustomStartDate', function () {

    var cases = [
      {
        startDate: '01/01/2016',
        hoursForStart: '01',
        minutesForStart: '00',
        middayPeriodForStart: 'AM',
        isInvalidDate: false,
        title: 'valid date and time'
      },
      {
        startDate: '',
        hoursForStart: '01',
        minutesForStart: '00',
        middayPeriodForStart: 'AM',
        isInvalidDate: true,
        title: 'no date specified'
      },
      {
        startDate: '01/01/2016',
        hoursForStart: '',
        minutesForStart: '00',
        middayPeriodForStart: 'AM',
        isInvalidDate: true,
        title: 'no hours specified'
      },
      {
        startDate: '01/01/2016',
        hoursForStart: '01',
        minutesForStart: '',
        middayPeriodForStart: 'AM',
        isInvalidDate: true,
        title: 'no minutes specified'
      },

      {
        startDate: '01/01/2016',
        hoursForStart: '01',
        minutesForStart: '00',
        middayPeriodForStart: '',
        isInvalidDate: true,
        title: 'no midday period specified'
      }
    ];

    beforeEach(function () {
      view.reopen({
        validate: Em.K
      });
    });

    cases.forEach(function (item) {
      it(item.title, function () {
        view.get('customDateFormFields').setProperties({
          startDate: item.startDate,
          hoursForStart: item.hoursForStart,
          minutesForStart: item.minutesForStart,
          middayPeriodForStart: item.middayPeriodForStart
        });
        expect(Em.isNone(view.createCustomStartDate())).to.equal(item.isInvalidDate);
      });
    });

  });

  describe('#createCustomEndDate', function () {

    var customEndCases = [
      {
        endDate: '01/01/2016',
        hoursForEnd: '01',
        minutesForEnd: '00',
        middayPeriodForEnd: 'AM',
        isInvalidDate: false,
        title: 'valid date and time'
      },
      {
        endDate: '',
        hoursForEnd: '01',
        minutesForEnd: '00',
        middayPeriodForEnd: 'AM',
        isInvalidDate: true,
        title: 'no date specified'
      },
      {
        endDate: '01/01/2016',
        hoursForEnd: '',
        minutesForEnd: '00',
        middayPeriodForEnd: 'AM',
        isInvalidDate: true,
        title: 'no hours specified'
      },
      {
        endDate: '01/01/2016',
        hoursForEnd: '01',
        minutesForEnd: '',
        middayPeriodForEnd: 'AM',
        isInvalidDate: true,
        title: 'no minutes specified'
      },

      {
        endDate: '01/01/2016',
        hoursForEnd: '01',
        minutesForEnd: '00',
        middayPeriodForEnd: '',
        isInvalidDate: true,
        title: 'no midday period specified'
      }
    ];

    beforeEach(function () {
      view.reopen({
        validate: Em.K
      });
    });

    customEndCases.forEach(function (item) {
      it(item.title, function () {
        view.get('customDateFormFields').setProperties({
          endDate: item.endDate,
          hoursForEnd: item.hoursForEnd,
          minutesForEnd: item.minutesForEnd,
          middayPeriodForEnd: item.middayPeriodForEnd,
          duration: {
            value: 0
          }
        });
        expect(Em.isNone(view.createCustomEndDate(1000))).to.equal(item.isInvalidDate);
      });
    });

    it('preset duration', function () {
      view.set('customDateFormFields.duration', {
        value: 900000
      });
      expect(view.createCustomEndDate(1000)).to.equal(901000);
    });

  });

  describe('#setErrorMessage', function () {

    var cases = [
      {
        key: 'startDate',
        property: 'isStartDateError',
        value: true,
        message: 'error',
        errorMessage: 'error',
        title: 'error'
      },
      {
        key: 'endDate',
        property: 'isEndDateError',
        value: false,
        message: null,
        errorMessage: '',
        title: 'no error'
      }
    ];

    cases.forEach(function (item) {

      describe(item.title, function () {

        beforeEach(function () {
          view.get('errors').setProperties({
            isStartDateError: false,
            isEndDateError: true
          });
          view.get('errorMessages').setProperties({
            startDate: '',
            endDate: 'error'
          });
          view.setErrorMessage(item.key, item.message);
        });

        it('should set error flag', function () {
          expect(view.get('errors').get(item.property)).to.equal(item.value);
        });

        it('should set error message', function () {
          expect(view.get('errorMessages').get(item.key)).to.equal(item.errorMessage);
        });

      });

    });

  });

  describe('#durationSelect', function () {

    var select;

    beforeEach(function () {
      select = view.get('durationSelect').create();
    });

    describe('#willInsertElement', function () {

      var cases = [
        {
          duration: 1800000,
          selection: {
            value: 1800000,
            label: Em.I18n.t('jobs.customDateFilter.duration.30min')
          },
          title: 'should detect preset option by value'
        },
        {
          duration: Em.I18n.t('jobs.customDateFilter.duration.2hr'),
          selection: {
            value: 7200000,
            label: Em.I18n.t('jobs.customDateFilter.duration.2hr')
          },
          title: 'should detect preset option by label'
        },
        {
          duration: '40 minutes',
          selection: {
            value: 0,
            label: Em.I18n.t('common.custom')
          },
          title: 'should set "Custom" option if preset one can\'t be detected'
        }
      ];

      cases.forEach(function (item) {
        it(item.title, function () {
          select.set('selection', item.duration);
          select.willInsertElement();
          expect(select.get('selection')).to.eql(item.selection);
        });
      });

    });

  });

});
