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

require('mixins/common/widgets/time_range_mixin');

var timeRangePopup = require('views/common/custom_date_popup');

describe('App.TimeRangeMixin', function () {

  var obj;

  beforeEach(function () {
    obj = Em.Object.create(App.TimeRangeMixin);
  });

  describe('#currentTimeRange', function () {

    var cases = Em.Object.create(App.TimeRangeMixin).get('timeRangeOptions'),
      title = 'should set "{0}" time range';

    cases.forEach(function (item) {
      it(title.format(item.name), function () {
        obj.set('currentTimeRangeIndex', item.index);
        expect(obj.get('currentTimeRange')).to.eql(item);
      });
    });

  });

  describe('#setTimeRange', function () {

    var indexCases = [
        {
          index: 1,
          currentTimeRangeIndex: 1,
          showCustomDatePopupCallCount: 0,
          title: 'preset time range',
          popupTestTitle: 'popup should not be displayed'
        },
        {
          index: 8,
          currentTimeRangeIndex: 0,
          showCustomDatePopupCallCount: 1,
          title: 'custom time range',
          popupTestTitle: 'popup should be displayed'
        }
      ],
      rangeCases = [
        {
          index: 1,
          currentTimeRangeIndex: 1,
          customStartTime: null,
          customEndTime: null,
          customDurationFormatted: null,
          title: 'time range is preset',
          testTitle: 'should reset time range boundaries'
        },
        {
          index: 8,
          currentTimeRangeIndex: 0,
          customStartTime: 1,
          customEndTime: 1,
          customDurationFormatted: '1 hour',
          title: 'time range is custom',
          testTitle: 'should not reset time range boundaries'
        }
      ];

    beforeEach(function () {
      obj.set('currentTimeRangeIndex', 0);
      sinon.stub(timeRangePopup, 'showCustomDatePopup', Em.K);
    });

    afterEach(function () {
      timeRangePopup.showCustomDatePopup.restore();
    });

    indexCases.forEach(function (item) {

      describe(item.title, function () {

        beforeEach(function () {
          obj.setTimeRange({
            context: {
              index: item.index
            }
          });
        });

        it('should set time range', function () {
          expect(obj.get('currentTimeRangeIndex')).to.equal(item.currentTimeRangeIndex);
        });

        it(item.popupTestTitle, function () {
          expect(timeRangePopup.showCustomDatePopup.callCount).to.equal(item.showCustomDatePopupCallCount);
        });

      });

    });

    rangeCases.forEach(function (item) {

      describe(item.title, function () {

        beforeEach(function () {
          obj.setProperties({
            customStartTime: 1,
            customEndTime: 1,
            customDurationFormatted: '1 hour'
          });
          obj.setTimeRange({
            context: {
              index: item.index
            }
          });
        });

        it(item.testTitle, function () {
          expect(obj.getProperties(['currentTimeRangeIndex', 'customStartTime', 'customEndTime', 'customDurationFormatted'])).to.eql({
            currentTimeRangeIndex: item.currentTimeRangeIndex,
            customStartTime: item.customStartTime,
            customEndTime: item.customEndTime,
            customDurationFormatted: item.customDurationFormatted
          });
        });

      });

    });

  });

});
