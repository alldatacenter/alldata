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
require('models/widget_property');

describe('App.WidgetProperty', function () {

  var widgetProperty,
    unit = App.WidgetPropertyTypes.findProperty('name', 'display_unit'),
    threshold = App.WidgetPropertyTypes.findProperty('name', 'threshold'),
    validate = function (value) {
      return !isNaN(value);
    };

  beforeEach(function () {
    widgetProperty = App.WidgetProperty.create();
  });

  describe('#viewClass', function () {

    var cases = [
      {
        displayType: 'textField',
        viewClass: App.WidgetPropertyTextFieldView
      },
      {
        displayType: 'threshold',
        viewClass: App.WidgetPropertyThresholdView
      },
      {
        displayType: 'select',
        viewClass: App.WidgetPropertySelectView
      },
      {
        displayType: 'none',
        viewClass: undefined
      }
    ];

    cases.forEach(function (item) {

      it(item.displayType, function () {
        widgetProperty.set('displayType', item.displayType);
        expect(widgetProperty.get('viewClass')).to.eql(item.viewClass);
      });

    });

  });

  describe('#isValid', function () {

    describe('display_unit', function () {

      var cases = [
        {
          isRequired: true,
          value: 'MB',
          isValid: true,
          title: 'valid value'
        },
        {
          isRequired: true,
          value: '0',
          isValid: true,
          title: 'non-empty value'
        },
        {
          isRequired: true,
          value: '',
          isValid: false,
          title: 'empty value'
        },
        {
          isRequired: false,
          value: '',
          isValid: true,
          title: 'value not required'
        }
      ];

      beforeEach(function () {
        widgetProperty.reopen(unit);
      });

      cases.forEach(function (item) {

        it(item.title, function () {
          widgetProperty.setProperties({
            isRequired: item.isRequired,
            value: item.value
          });
          expect(widgetProperty.get('isValid')).to.equal(item.isValid);
        });

      });

    });

    describe('threshold', function () {

      var cases = [
        {
          isSmallValueValid: true,
          isBigValueValid: true,
          isValid: true,
          title: 'both threshold values are valid'
        },
        {
          isSmallValueValid: false,
          isBigValueValid: true,
          isValid: false,
          title: 'warning threshold value is invalid'
        },
        {
          isSmallValueValid: true,
          isBigValueValid: false,
          isValid: false,
          title: 'error threshold value is invalid'
        },
        {
          isSmallValueValid: false,
          isBigValueValid: false,
          isValid: false,
          title: 'both threshold values are invalid'
        }
      ];

      cases.forEach(function (item) {

        it(item.title, function () {
          widgetProperty.reopen(threshold, {
            isSmallValueValid: item.isSmallValueValid,
            isBigValueValid: item.isBigValueValid
          });
          expect(widgetProperty.get('isValid')).to.equal(item.isValid);
        });

      });

    });

  });

  describe('#isSmallValueValid', function () {

    var cases = [
      {
        smallValue: '1',
        isSmallValueValid: true,
        title: 'valid value'
      },
      {
        smallValue: 'value',
        isSmallValueValid: false,
        title: 'invalid value'
      }
    ];

    beforeEach(function () {
      widgetProperty.reopen(threshold);
      sinon.stub(widgetProperty, 'validate', validate);
    });

    afterEach(function () {
      widgetProperty.validate.restore();
    });

    cases.forEach(function (item) {

      it(item.title, function () {
        widgetProperty.set('smallValue', item.smallValue);
        expect(widgetProperty.get('isSmallValueValid')).to.equal(item.isSmallValueValid);
      });

    });

  });

  describe('#isBigValueValid', function () {

    var cases = [
      {
        bigValue: '1',
        isBigValueValid: true,
        title: 'valid value'
      },
      {
        bigValue: 'value',
        isBigValueValid: false,
        title: 'invalid value'
      }
    ];

    beforeEach(function () {
      widgetProperty.reopen(threshold);
      sinon.stub(widgetProperty, 'validate', validate);
    });

    afterEach(function () {
      widgetProperty.validate.restore();
    });

    cases.forEach(function (item) {

      it(item.title, function () {
        widgetProperty.set('bigValue', item.bigValue);
        expect(widgetProperty.get('isBigValueValid')).to.equal(item.isBigValueValid);
      });

    });

  });

  describe('#validate', function () {

    var cases = [
      {
        value: '',
        validateResult: true,
        title: 'empty value'
      },
      {
        value: ' \r\n\t ',
        validateResult: true,
        title: 'spaces only'
      },
      {
        value: 'v',
        validateResult: false,
        title: 'invalid value'
      },
      {
        value: ' v \r\n\t',
        validateResult: false,
        title: 'invalid value with spaces'
      },
      {
        value: '-1',
        validateResult: false,
        title: 'value below the minimum'
      },
      {
        value: ' -1 \r\n\t',
        validateResult: false,
        title: 'value below the minimum with spaces'
      },
      {
        value: '2',
        validateResult: false,
        title: 'value above the minimum'
      },
      {
        value: ' 2 \r\n\t',
        validateResult: false,
        title: 'value above the minimum with spaces'
      },
      {
        value: '0,5',
        validateResult: false,
        title: 'malformed number'
      },
      {
        value: ' 0,5 \r\n\t',
        validateResult: false,
        title: 'malformed number with spaces'
      },
      {
        value: '0.5',
        validateResult: true,
        title: 'valid value'
      },
      {
        value: ' 0.5 \r\n\t',
        validateResult: true,
        title: 'valid value with spaces'
      },
      {
        value: '2E-1',
        validateResult: true,
        title: 'exponentially formatted value'
      },
      {
        value: ' 2E-1 \r\n\t',
        validateResult: true,
        title: 'exponentially formatted value with spaces'
      }
    ];

    beforeEach(function () {
      widgetProperty.reopen(threshold);
    });

    cases.forEach(function (item) {

      it(item.title, function () {
        expect(widgetProperty.validate(item.value)).to.equal(item.validateResult);
      });

    });

  });

});