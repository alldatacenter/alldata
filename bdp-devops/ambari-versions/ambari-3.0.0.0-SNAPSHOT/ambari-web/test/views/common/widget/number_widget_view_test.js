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
require('views/common/widget/number_widget_view');

describe('App.NumberWidgetView', function () {

  var view;

  beforeEach(function () {
    view = App.NumberWidgetView.create({
      value: 0,
      content: {
        properties: {
          warning_threshold: 0,
          critical_threshold: 0
        }
      }
    });
  });

  afterEach(function () {
    clearTimeout(view.get('timeoutId'));
    view.destroy();
  });

  describe("#displayValue", function () {
    var testCases = [
      {
        value: '',
        expected: Em.I18n.t('common.na')
      },
      {
        value: 'a',
        expected: Em.I18n.t('common.na')
      },
      {
        value: '1',
        expected: '1u'
      }
    ];

    testCases.forEach(function(test) {
      it("value = " + test.value, function() {
        view.set('value', test.value);
        view.set('content.properties.display_unit', 'u');
        view.propertyDidChange('displayValue');
        expect(view.get('displayValue')).to.be.equal(test.expected);
      });
    });
  });

  describe("#contentColor()", function() {
    var testCases = [
      {
        title: 'no value',
        data: {
          value: null,
          warningThreshold: 1,
          criticalThreshold: 3
        },
        result: 'grey'
      },
      {
        title: 'no value',
        data: {
          value: null,
          warningThreshold: null,
          criticalThreshold: null
        },
        result: 'grey'
      },
      {
        title: 'no value',
        data: {
          value: null,
          warningThreshold: null,
          criticalThreshold: 3
        },
        result: 'grey'
      },
      {
        title: 'both thresholds NOT existed',
        data: {
          value: 2,
          warningThreshold: null,
          criticalThreshold: null
        },
        result: 'green'
      },
      {
        title: 'both thresholds existed',
        data: {
          value: 2,
          warningThreshold: 1,
          criticalThreshold: 3
        },
        result: 'orange'
      },
      {
        title: 'both thresholds existed',
        data: {
          value: 2,
          warningThreshold: 3,
          criticalThreshold: 1
        },
        result: 'orange'
      },
      {
        title: 'both thresholds existed',
        data: {
          value: 0.5,
          warningThreshold: 1,
          criticalThreshold: 3
        },
        result: 'green'
      },
      {
        title: 'both thresholds existed',
        data: {
          value: 3.5,
          warningThreshold: 3,
          criticalThreshold: 1
        },
        result: 'green'
      },
      {
        title: 'both thresholds existed',
        data: {
          value: 3.5,
          warningThreshold: 1,
          criticalThreshold: 3
        },
        result: 'red'
      },
      {
        title: 'both thresholds existed',
        data: {
          value: 0.5,
          warningThreshold: 3,
          criticalThreshold: 1
        },
        result: 'red'
      },
      {
        title: 'only warning threshold existed',
        data: {
          value: 0,
          warningThreshold: 1,
          criticalThreshold: null
        },
        result: 'green'
      },
      {
        title: 'only warning threshold existed',
        data: {
          value: 2,
          warningThreshold: 1,
          criticalThreshold: null
        },
        result: 'orange'
      },
      {
        title: 'only critical threshold existed',
        data: {
          value: 0.5,
          warningThreshold: null,
          criticalThreshold: 1
        },
        result: 'green'
      },
      {
        title: 'only critical threshold existed',
        data: {
          value: 1.5,
          warningThreshold: null,
          criticalThreshold: 1
        },
        result: 'red'
      },
      {
        title: 'invalid thresholds',
        data: {
          value: 1.5,
          warningThreshold: '&*&%',
          criticalThreshold: 1
        },
        result: 'red'
      },
      {
        title: 'invalid thresholds',
        data: {
          value: 1.5,
          warningThreshold: '&*&%',
          criticalThreshold: '@#^^'
        },
        result: 'green'
      }
    ];

    testCases.forEach(function (test) {
      it(test.title, function () {
        view.set('value', test.data.value);
        view.set('content.properties.warning_threshold', test.data.warningThreshold);
        view.set('content.properties.error_threshold', test.data.criticalThreshold);
        expect(view.get('contentColor')).to.eql(test.result);
      });
    });
  });
});