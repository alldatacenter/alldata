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
var filters = require('views/common/filter_view');
require('utils/helper');

describe('filters.getFilterByType', function () {

  describe('ambari-bandwidth', function () {

    var filter = filters.getFilterByType('ambari-bandwidth');
    var testData = [
      {
        condition: '<',
        value: 'any value',
        result: true
      },
      {
        condition: '=',
        value: 'any value',
        result: true
      },
      {
        condition: '>',
        value: 'any value',
        result: true
      },
      {
        condition: '1',
        value: '1GB',
        result: true
      },
      {
        condition: '1g',
        value: '1GB',
        result: true
      },
      {
        condition: '=1g',
        value: '1GB',
        result: true
      },
      {
        condition: '<1g',
        value: '0.9GB',
        result: true
      },
      {
        condition: '>1g',
        value: '1.1GB',
        result: true
      },
      {
        condition: '=1k',
        value: '1KB',
        result: true
      },
      {
        condition: '<1k',
        value: '0.9KB',
        result: true
      },
      {
        condition: '>1k',
        value: '1.1KB',
        result: true
      },
      {
        condition: '=1m',
        value: '1MB',
        result: true
      },
      {
        condition: '<1m',
        value: '0.9MB',
        result: true
      },
      {
        condition: '>1m',
        value: '1.1MB',
        result: true
      },
      {
        condition: '=1024k',
        value: '1MB',
        result: true
      },
      {
        condition: '=1024m',
        value: '1GB',
        result: true
      }
    ];

    testData.forEach(function(item){
      it('Condition: {0} - match value: {1}'.format(JSON.stringify(item.condition), JSON.stringify(item.value)), function () {
        expect(filter(item.value, item.condition)).to.equal(item.result);
      })
    });
  });

  describe('duration', function () {

    var filter = filters.getFilterByType('duration');
    var testData = [
      {
        condition: '<',
        value: 'any value',
        result: true
      },
      {
        condition: '=',
        value: 'any value',
        result: true
      },
      {
        condition: '>',
        value: 'any value',
        result: true
      },
      {
        condition: '1',
        value: '1000',
        result: true
      },
      {
        condition: '1s',
        value: '1000',
        result: true
      },
      {
        condition: '=1s',
        value: '1000',
        result: true
      },
      {
        condition: '>1s',
        value: '1001',
        result: true
      },
      {
        condition: '<1s',
        value: '999',
        result: true
      },
      {
        condition: '=1m',
        value: '60000',
        result: true
      },
      {
        condition: '>1m',
        value: '60001',
        result: true
      },
      {
        condition: '<1m',
        value: '59999',
        result: true
      },
      {
        condition: '=1h',
        value: '3600000',
        result: true
      },
      {
        condition: '>1h',
        value: '3600001',
        result: true
      },
      {
        condition: '<1h',
        value: '3599999',
        result: true
      }

    ];

    testData.forEach(function(item){
      it('Condition: {0} - match value: {1}'.format(JSON.stringify(item.condition), JSON.stringify(item.value)), function () {
        expect(filter(item.value, item.condition)).to.equal(item.result);
      })
    });
  });

  describe('date', function () {

    var filter = filters.getFilterByType('date');
    var testData = [
      {
        condition: 'Past 1 Day',
        value: 86300000,
        result: true
      },
      {
        condition: 'Past 2 Days',
        value: 172700000,
        result: true
      },
      {
        condition: 'Past 7 Days',
        value: 604700000,
        result: true
      },
      {
        condition: 'Past 14 Days',
        value: 1209500000,
        result: true
      },
      {
        condition: 'Past 30 Days',
        value: 2591900000,
        result: true
      },
      {
        condition: 'Any',
        value: 'any value',
        result: true
      }
    ];

    testData.forEach(function(item){
      it('Condition: {0} - match value: {1}'.format(JSON.stringify(item.condition), JSON.stringify(item.value)), function () {
        var currentTime = App.dateTime();
        item.value = currentTime - item.value;
        expect(filter(item.value, item.condition)).to.equal(item.result);
      })
    });
  });

  describe('number', function () {

    var filter = filters.getFilterByType('number');
    var testData = [
      {
        condition: '<',
        value: 'any value',
        result: true
      },
      {
        condition: '=',
        value: 'any value',
        result: true
      },
      {
        condition: '>',
        value: 'any value',
        result: true
      },
      {
        condition: '1',
        value: '1',
        result: true
      },
      {
        condition: '=1',
        value: '1',
        result: true
      },
      {
        condition: '<1',
        value: '0',
        result: true
      },
      {
        condition: '>1',
        value: '2',
        result: true
      }
    ];

    testData.forEach(function(item){
      it('Condition: {0} - match value: {1}'.format(JSON.stringify(item.condition), JSON.stringify(item.value)), function () {
        expect(filter(item.value, item.condition)).to.equal(item.result);
      })
    });
  });

  describe('multiple', function () {

    var filter = filters.getFilterByType('multiple');
    var commonValue = [
      {componentName: 'DATANODE'},
      {componentName: 'NAMENODE'},
      {componentName: 'JOBTRACKER'}
    ];
    var testData = [
      {
        condition: 'DATANODE',
        value: commonValue,
        result: true
      },
      {
        condition: 'DATANODE,NAMENODE',
        value: commonValue,
        result: true
      },
      {
        condition: 'DATANODE,NAMENODE,JOBTRACKER',
        value: commonValue,
        result: true
      },
      {
        condition: 'JOBTRACKER,TASKTRACKER',
        value: commonValue,
        result: true
      },
      {
        condition: 'TASKTRACKER',
        value: commonValue,
        result: false
      }
    ];

    testData.forEach(function(item) {
      var substr = item.condition + (item.result ? ' - match ' : ' - doesn\'t match ');
      var components = item.value.mapProperty('componentName').join(' ');
      it('Condition: {0} value: {1}'.format(substr, components), function () {
        expect(filter(item.value, item.condition)).to.equal(item.result);
      })
    });
  });

  describe('string', function () {

    var filter = filters.getFilterByType('string');

    var testData = [
      {
        condition: '',
        value: '',
        result: true
      },
      {
        condition: '',
        value: 'hello',
        result: true
      },
      {
        condition: 'hello',
        value: 'hello',
        result: true
      },
      {
        condition: 'HeLLo',
        value: 'hello',
        result: true
      },
      {
        condition: 'he',
        value: 'hello',
        result: true
      },
      {
        condition: 'lo',
        value: 'hello',
        result: true
      },
      {
        condition: 'lo (1)',
        value: 'hello (1)',
        result: true
      },
      {
        condition: 'lol',
        value: 'hello',
        result: false
      },
      {
        condition: 'hello',
        value: '',
        result: false
      },
      {
        condition: '?',
        value: 'hello',
        result: false
      }
    ];

    testData.forEach(function(item){
      var substr = item.condition + (item.result ? ' - match ' : ' - doesn\'t match ');
      it('Condition: {0} value: {1}'.format(substr, item.value), function () {
        expect(filter(item.value, item.condition)).to.equal(item.result);
      });
    });
  });

  describe('sub-resource', function () {

    var filter = filters.getFilterByType('sub-resource');

    var testData = [
      {
        title: 'condition is null',
        condition: null,
        value: [Em.Object.create({
          prop1: 1
        })],
        result: true
      },
      {
        title: 'condition is empty',
        condition: [],
        value: [Em.Object.create({
          prop1: 1
        })],
        result: true
      },
      {
        title: 'condition match one property',
        condition: [
          {
            property: 'prop1',
            value: 1
          }
        ],
        value: [Em.Object.create({
          prop1: 1
        })],
        result: true
      },
      {
        title: 'condition match two properties',
        condition: [
          {
            property: 'prop1',
            value: 1
          },
          {
            property: 'prop2',
            value: 2
          }
        ],
        value: [Em.Object.create({
          prop1: 1,
          prop2: 2
        })],
        result: true
      },
      {
        title: 'only one of two properties match',
        condition: [
          {
            property: 'prop1',
            value: 3
          },
          {
            property: 'prop2',
            value: 2
          }
        ],
        value: [Em.Object.create({
          prop1: 1,
          prop2: 2
        })],
        result: false
      },
      {
        title: 'none of two properties match',
        condition: [
          {
            property: 'prop1',
            value: 3
          },
          {
            property: 'prop2',
            value: 4
          }
        ],
        value: [Em.Object.create({
          prop1: 1,
          prop2: 2
        })],
        result: false
      }
    ];

    testData.forEach(function (test) {
      it(test.title, function () {
        expect(filter(test.value, test.condition)).to.equal(test.result);
      })
    });
  });

  describe('alert_status', function () {

    var filter = filters.getFilterByType('alert_status');

    Em.A([
      {
        origin: {OK: {count: 1, maintenanceCount: 0}},
        compareValue: 'OK',
        e: true
      },
      {
        origin: {OK: {count: 0, maintenanceCount: 1}},
        compareValue: 'OK',
        e: true
      },
      {
        origin: {WARN: {count: 1, maintenanceCount: 0}},
        compareValue: 'OK',
        e: false
      },
      {
        origin: {WARN: {count: 0, maintenanceCount: 0}},
        compareValue: 'WARN',
        e: false
      },
      {
        origin: {OK: {count: 0, maintenanceCount: 0}, WARN: {count: 0, maintenanceCount: 0}},
        compareValue: 'PENDING',
        e: true
      },
      {
        origin: {},
        compareValue: 'PENDING',
        e: true
      },
      {
        origin: {OK: {count: 1, maintenanceCount: 0}},
        compareValue: 'PENDING',
        e: false
      }
    ]).forEach(function(test, i) {
        it('test #' + (i + 1), function() {
          expect(filter(test.origin, test.compareValue)).to.equal(test.e);
        });
      });

  });

  describe('alert_group', function () {

    var filter = filters.getFilterByType('alert_group');

    Em.A([
        {
          origin: [{id: 1}, {id: 2}, {id: 3}],
          compareValue: 1,
          e: true
        },
        {
          origin: [],
          compareValue: 1,
          e: false
        },
        {
          origin: [{id: 2}, {id: 3}],
          compareValue: 1,
          e: false
        }
      ]).forEach(function(test, i) {
        it('test #' + (i + 1), function() {
          expect(filter(test.origin, test.compareValue)).to.equal(test.e);
        });
      });

  });

  describe('os', function () {

    var filter = filters.getFilterByType('os');

    [
      {
        origin: [{osType: 'os1'}, {osType: 'os2'}, {osType: 'os3'}],
        compareValue: 'os1',
        e: true
      },
      {
        origin: [{osType: 'os1'}, {osType: 'os2'}, {osType: 'os3'}],
        compareValue: 'os2',
        e: true
      },
      {
        origin: [{osType: 'os1'}, {osType: 'os2'}, {osType: 'os3'}],
        compareValue: 'os3',
        e: true
      },
      {
        origin: [],
        compareValue: 'os1',
        e: false
      },
      {
        origin: [{}, {}, {}],
        compareValue: 'os1',
        e: false
      }
    ].forEach(function (test, i) {
      it('test #' + (i + 1), function () {
        expect(filter(test.origin, test.compareValue)).to.be.equal(test.e);
      });
    });

  });

  describe('range', function () {

    var filter = filters.getFilterByType('range');

    [
      {
        compareValue: [2],
        origin: 1,
        e: false
      },
      {
        compareValue: [0, 1],
        origin: 1,
        e: true
      },
      {
        compareValue: [1, 1],
        origin: 1,
        e: true
      },
      {
        compareValue: [2, 2],
        origin: 1,
        e: false
      },
      {
        compareValue: [4, 2],
        origin: 1,
        e: false
      }
    ].forEach(function (test, i) {
      it('test #' + (i + 1), function () {
        expect(filter(test.origin, test.compareValue)).to.be.equal(test.e);
      });
    });

  });

});
