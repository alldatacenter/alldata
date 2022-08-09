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

var arrayUtils = require('utils/array_utils');

describe('array_utils', function () {

  describe('#uniqObjectsbyId', function () {

    var arr = [
        {
          n: 0,
          v: 'v0'
        },
        {
          n: 0,
          v: 'v01'
        },
        {
          n: 1,
          v: 'v1'
        },
        {
          n: '1',
          v: 'v11'
        },
        {
          n: 2,
          v: 'v2'
        }
      ],
      result = [
        {
          n: 0,
          v: 'v0'
        },
        {
          n: 1,
          v: 'v1'
        },
        {
          n: '1',
          v: 'v11'
        },
        {
          n: 2,
          v: 'v2'
        }
      ];

    it('should return one element for one id', function () {
      expect(arrayUtils.uniqObjectsbyId(arr, 'n').toArray()).to.eql(result);
    });

  });

  describe('#intersect', function () {

    var cases = [
      {
        arr1: [Infinity, 0, 1, 2, {a: 1}, {b: 2}, null, undefined],
        arr2: ['undefined', null, {b: '2'}, {a: 1}, 2.0, '1', 0, Infinity],
        result: [null, 2, 0, Infinity],
        title: 'arrays of the same length have common items'
      },
      {
        arr1: [true, false, [0, 1], [2, 3], [4, 5], [6], null, undefined],
        arr2: [undefined, 'null', 6, [4, 5], ['2', '3'], [String(0), String(1)], '0,1', false, 'true'],
        result: [false, undefined],
        title: 'arrays of different length have common items'
      },
      {
        arr1: ['1', function () {}, NaN],
        arr2: ['function () {}', Number('1'), NaN],
        result: [],
        title: 'arrays have no common items'
      },
      {
        arr1: [[0], undefined, null],
        arr2: [],
        result: [],
        title: 'one of arrays is empty'
      },
      {
        arr1: [],
        arr2: [],
        result: [],
        title: 'both arrays are empty'
      }
    ];

    cases.forEach(function (item) {

      describe(item.title, function () {

        it('arrays intersection', function () {
          expect(arrayUtils.intersect(item.arr1, item.arr2)).to.eql(item.result);
        });

        it('commutativity', function () {
          expect(arrayUtils.intersect(item.arr1, item.arr2).sort()).to.eql(arrayUtils.intersect(item.arr2, item.arr1).sort());
        });

      });

    });

  });

  describe('#sortByIdAsVersion', function () {

    Em.A([
      {
        c: [{id: '1.2.4'}, {id: '1.2.5'}, {id: '1.2.3'}],
        m: 'Items without letters and with same length',
        e: ['1.2.3', '1.2.4', '1.2.5']
      },
      {
        c: [{id: 'HDP-1.2.4'}, {id: 'HDP-1.2.5'}, {id: 'HDP-1.2.3'}],
        m: 'Items with letters and with same length',
        e: ['HDP-1.2.3', 'HDP-1.2.4', 'HDP-1.2.5']
      },
      {
        c: [{id: 'HDP-1.2.4.2'}, {id: 'HDP-1.2.4.1'}, {id: 'HDP-1.2.3'}],
        m: 'Items with letters and with custom length',
        e: ['HDP-1.2.3', 'HDP-1.2.4.1', 'HDP-1.2.4.2']
      },
      {
        c: [{id: 'HDP-1.2.4.2.3'}, {id: 'HDP-1.2.4.11.3'}, {id: 'HDP-1.2.3'}],
        m: 'Items with letters and with double digits',
        e: ['HDP-1.2.3', 'HDP-1.2.4.2.3', 'HDP-1.2.4.11.3']
      },
      {
        c: [{id: 'HDP-1.2.3.2'}, {id: 'HDP-1.2.4'}, {id: 'HDP-1.2.3'}],
        m: 'Items with letters and with custom length (2)',
        e: ['HDP-1.2.3', 'HDP-1.2.3.2', 'HDP-1.2.4']
      },
      {
        c: [{id: 'HDP-1.2.3'}, {id: 'HDP-1.2.4'}, {id: 'HDP-1.2.3'}],
        m: 'Items with letters and equal ids',
        e: ['HDP-1.2.3', 'HDP-1.2.3', 'HDP-1.2.4']
      }
    ]).forEach(function (test) {

      it(test.m, function () {
        expect(test.c.sort(arrayUtils.sortByIdAsVersion).mapProperty('id')).to.be.eql(test.e);
      });

    });

  });

});
