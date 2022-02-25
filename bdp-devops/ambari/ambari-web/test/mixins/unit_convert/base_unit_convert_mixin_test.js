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

describe('App.BaseUnitConvertMixin', function() {
  beforeEach(function() {
    this.mixin = Em.Object.create(App.BaseUnitConvertMixin, {});
  });

  describe('#convertValue', function() {
    var tests = [
      {
        value: "10",
        fromUnit: "b",
        toUnit: "b",
        e: 10
      },
      {
        value: "2048",
        fromUnit: "b",
        toUnit: "kb",
        e: 2
      },
      {
        value: "2097152",
        fromUnit: "b",
        toUnit: "mb",
        e: 2
      },
      {
        value: "1",
        fromUnit: "mb",
        toUnit: "kb",
        e: 1024
      },
      {
        value: "107374182400",
        fromUnit: "b",
        toUnit: "gb",
        e: 100
      },
      {
        value: "100",
        fromUnit: "gb",
        toUnit: "b",
        e: 107374182400
      },
      {
        value: "1294336",
        fromUnit: "b",
        toUnit: ["mb", "kb"],
        e: [
          { type: "mb", value: 1},
          { type: "kb", value: 240}
        ]
      },
      {
        value: [
          { type: "mb", value: 1},
          { type: "kb", value: 240}
        ],
        fromUnit: ["mb", "kb"],
        toUnit: "b",
        e: 1294336
      },
      {
        value: [
          { type: "mb", value: 1},
          { type: "kb", value: 240}
        ],
        fromUnit: "mb,kb",
        toUnit: "b",
        e: 1294336
      },
      {
        value: 60000,
        fromUnit: 'milliseconds',
        toUnit: "days,hours,minutes",
        e: [
          { type: 'days', value: 0},
          { type: 'hours', value: 0},
          { type: 'minutes', value: 1}
        ]
      },
      {
        value: 80,
        fromUnit: 'percent',
        toUnit: 'int',
        currentDimensionType: 'percent.percent_int',
        e: 80
      },
      {
        value: 80,
        fromUnit: 'int',
        toUnit: 'percent',
        currentDimensionType: 'percent.percent_int',
        e: 80
      },
      {
        value: 0.89,
        fromUnit: 'float',
        toUnit: 'percent',
        currentDimensionType: 'percent.percent_float',
        e: 89
      },
      {
        value: 89,
        fromUnit: 'percent',
        toUnit: 'float',
        currentDimensionType: 'percent.percent_float',
        e: 0.89
      },
      {
        value: 100,
        fromUnit: 'percent',
        toUnit: 'float',
        currentDimensionType: 'percent.percent_float',
        e: 1
      },
      {
        value: 1,
        fromUnit: 'float',
        toUnit: 'percent',
        currentDimensionType: 'percent.percent_float',
        e: 100
      }
    ];

    tests.forEach(function(test) {
      it('should convert {0} {1} to {2} {3}'.format(JSON.stringify(test.value), test.fromUnit, JSON.stringify(test.e), test.toUnit), function() {
        if (test.currentDimensionType) {
          this.mixin.set('currentDimensionType', test.currentDimensionType);
        }

        var result = this.mixin.convertValue(test.value, test.fromUnit, test.toUnit);
        if (Em.isArray(result)) {
          result = result.map(function(item) {
            return App.permit(item, ['type', 'value']);
          });
        }
        expect(result).to.be.eql(test.e);
      });
    }, this);
  });
});
