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

var numberUtils = require('utils/number_utils');

describe('utils/number_utils', function() {

  describe('#bytesToSize', function() {

    describe('check bytes', function() {
      var tests = Em.A([
        {
          bytes: null,
          precision: null,
          parseType: null,
          multiplyBy: null,
          e: 'n/a',
          m: '"n/a" if bytes is null'
        },
        {
          bytes: undefined,
          precision: null,
          parseType: null,
          multiplyBy: null,
          e: 'n/a',
          m: '"n/a" if bytes is undefined'
        },
        {
          bytes: 200,
          precision: null,
          parseType: undefined,
          multiplyBy: null,
          e: '0 Bytes',
          m: '0 if multiply is `null`'
        },
        {
          bytes: 200,
          precision: null,
          parseType: undefined,
          multiplyBy: undefined,
          e: '200 Bytes',
          m: '"200 Bytes" if `multiplyBy` and `parseType` are `undefined`'
        },
        {
          bytes: 200,
          precision: null,
          parseType: undefined,
          multiplyBy: 1,
          e: '200 Bytes',
          m: '`200 Bytes` if `parsetype` is `undefined`'
        }
      ]);

      tests.forEach(function(test) {
        it(test.m, function() {
          expect(numberUtils.bytesToSize(test.bytes, test.precision, test.parseType, test.multiplyBy)).to.equal(test.e);
        });
      });
    });

    describe('check sizes', function() {
      var tests = Em.A([
        {
          bytes: 12,
          precision: null,
          parseType: 'parseInt',
          multiplyBy: 1,
          e: 'Bytes',
          m: 'Bytes'
        },
        {
          bytes: 1024 + 12,
          precision: null,
          parseType: 'parseInt',
          multiplyBy: 1,
          e: 'KB',
          m: 'KB'
        },
        {
          bytes: 1024 * 1024 + 12,
          precision: null,
          parseType: 'parseInt',
          multiplyBy: 1,
          e: 'MB',
          m: 'MB'
        },
        {
          bytes: 1024 * 1024 * 1024 + 12,
          precision: null,
          parseType: 'parseInt',
          multiplyBy: 1,
          e: 'GB',
          m: 'GB'
        },
        {
          bytes: 1024 * 1024 * 1024 * 1024 + 12,
          precision: null,
          parseType: 'parseInt',
          multiplyBy: 1,
          e: 'TB',
          m: 'TB'
        },
        {
          bytes: 1024 * 1024 * 1024 * 1024 * 1024 + 12,
          precision: null,
          parseType: 'parseInt',
          multiplyBy: 1,
          e: 'PB',
          m: 'PB'
        }
      ]);

      tests.forEach(function(test) {
        it(test.m, function() {
          expect(numberUtils.bytesToSize(test.bytes, test.precision, test.parseType, test.multiplyBy).endsWith(test.e)).to.equal(true);
        });
      });
    });

    describe('check calculated result', function() {
      var tests = Em.A([
        {
          bytes: 42,
          precision: null,
          parseType: 'parseInt',
          multiplyBy: 1,
          e: '42',
          m: 'Bytes'
        },
        {
          bytes: 1024 * 12,
          precision: null,
          parseType: 'parseInt',
          multiplyBy: 1,
          e: '12',
          m: 'KB'
        },
        {
          bytes: 1024 * 1024 * 23,
          precision: null,
          parseType: 'parseInt',
          multiplyBy: 1,
          e: '23',
          m: 'MB'
        },
        {
          bytes: 1024 * 1024 * 1024 * 34,
          precision: null,
          parseType: 'parseInt',
          multiplyBy: 1,
          e: '34',
          m: 'GB'
        },
        {
          bytes: 1024 * 1024 * 1024 * 1024 * 45,
          precision: null,
          parseType: 'parseInt',
          multiplyBy: 1,
          e: '45',
          m: 'TB'
        },
        {
          bytes: 1024 * 1024 * 1024 * 1024 * 1024 * 56,
          precision: null,
          parseType: 'parseInt',
          multiplyBy: 1,
          e: '56',
          m: 'PB'
        }
      ]);

      tests.forEach(function(test) {
        it(test.m, function() {
          expect(numberUtils.bytesToSize(test.bytes, test.precision, test.parseType, test.multiplyBy).startsWith(test.e)).to.equal(true);
        });
      });
    });

  });
  describe('#validateInteger()', function() {
    var tests = [
      {
        str: null,
        min: null,
        max: null,
        m: 'all params null to' + Em.I18n.t('number.validate.empty'),
        e: Em.I18n.t('number.validate.empty')
      },
      {
        str: "string",
        min: null,
        max: null,
        m: 'try to validate `string` should return ' + Em.I18n.t('number.validate.empty'),
        e: Em.I18n.t('number.validate.notValidNumber')
      },
      {
        str: "string",
        min: null,
        max: null,
        m: 'try to validate `string` should return ' + Em.I18n.t('number.validate.notValidNumber'),
        e: Em.I18n.t('number.validate.notValidNumber')
      },
      {
        str: "1abc",
        min: null,
        max: null,
        m: 'try to validate `1abc` should return ' + Em.I18n.t('number.validate.notValidNumber'),
        e: Em.I18n.t('number.validate.notValidNumber')
      },
      {
        str: "1",
        min: null,
        max: null,
        m: 'try to validate `1` should return ' + Em.I18n.t('number.validate.moreThanMaximum').format(null),
        e: Em.I18n.t('number.validate.moreThanMaximum').format(null)
      },
      {
        str: "1",
        min: 2,
        max: 0,
        m: 'try to validate `1` with max = 0 and min = 2 should return ' + Em.I18n.t('number.validate.lessThanMinimum').format(2),
        e: Em.I18n.t('number.validate.lessThanMinimum').format(2)
      }
    ];

    tests.forEach(function(test) {
      it(test.m, function(){
        expect(numberUtils.validateInteger(test.str, test.min, test.max)).to.eql(test.e);
      });
    });
  });

  describe('#getCardinalityValue()', function() {
    var generateTestObject = function(cardinality, isMax, expected) {
      return {
        cardinality: cardinality,
        isMax: isMax,
        e: expected
      }
    };
    var tests = [
      generateTestObject(null, true, 0),
      generateTestObject(undefined, true, 0),
      generateTestObject('1', true, 1),
      generateTestObject('1', false, 1),
      generateTestObject('0+', true, Infinity),
      generateTestObject('0+', false, 0),
      generateTestObject('1+', true, Infinity),
      generateTestObject('1-2', false, 1),
      generateTestObject('1-2', true, 2),
      generateTestObject('ALL', true, Infinity),
      generateTestObject('ALL', false, Infinity)
    ];
    var message = 'cardinality `{0}`. {1} value should be {2}';
    tests.forEach(function(test) {
      it(message.format('' + test.cardinality, test.isMax ? 'maximum' : 'minimum', test.e), function() {
        expect(numberUtils.getCardinalityValue(test.cardinality, test.isMax)).to.be.eql(test.e);
      });
    })
  });
});