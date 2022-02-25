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

describe('Filters Service', function () {
  var Filters;

  beforeEach(function () {
    module('ambariAdminConsole', angular.noop);
    inject(function (_Filters_) {
      Filters = _Filters_;
    });
  });
  describe('#initFilterOptions', function() {
    var items = [
      {
        k1: {
          values: ['val1', 'val3']
        },
        k2: 'val2'
      }
    ];
    var filters = [
      {
        isMultiple: true,
        key: 'k1',
        options: [],
        customValueConverter: function(item) {
          return item.k1.values;
        }
      },
      {
        key: 'k2',
        options: []
      },
      {
        isStatic: true,
        options: [
          {
            key: 'static1',
            label: 'static1'
          }
        ]
      }
    ];
    beforeEach(function() {
      Filters.initFilterOptions(filters, items);
    });

    it('should set static options of filters', function() {
      expect(filters[2].options).toEqual([{
        key: 'static1',
        label: 'static1'
      }]);
    });

    it('should set options of filters', function() {
      expect(filters[1].options).toEqual([{
        key: 'val2',
        label: 'val2'
      }]);
    });

    it('should set multiple options of filters', function() {
      expect(filters[0].options).toEqual([
        {
          key: 'val1',
          label: 'val1'
        },
        {
          key: 'val3',
          label: 'val3'
        }
      ]);
    });
  });

  describe('#filterItems', function() {

    it('all items should be filtered when no filters applied', function() {
      var items = [{}];

      expect(Filters.filterItems(null, items, [])).toEqual(1);
      expect(items[0].isFiltered).toBeTruthy();
    });

    it('items should be filtered when simple filter applied', function() {
      var appliedFilters = [
        {
          key: 'p1',
          values: ['val1']
        }
      ];
      var items = [
        { p1: 'val1' },
        { p1: 'val2' }
      ];
      var filterDefinitions = [];

      expect(Filters.filterItems(appliedFilters, items, filterDefinitions)).toEqual(1);
      expect(items[0].isFiltered).toBeTruthy();
      expect(items[1].isFiltered).toBeFalsy();
    });

    it('items should be filtered when filter applied on array values', function() {
      var appliedFilters = [
        {
          key: 'p1',
          values: ['a']
        }
      ];
      var items = [
        { p1: ['a', 'b'] },
        { p1: ['c', 'b'] }
      ];
      var filterDefinitions = [];

      expect(Filters.filterItems(appliedFilters, items, filterDefinitions)).toEqual(1);
      expect(items[0].isFiltered).toBeTruthy();
      expect(items[1].isFiltered).toBeFalsy();
    });

    it('items should be filtered when custom filter applied', function() {
      var appliedFilters = [
        {
          key: 'p1',
          values: ['a']
        }
      ];
      var items = [
        { p1: { customValue: 'a' } },
        { p1: { customValue: 'b' } }
      ];
      var filterDefinitions = [{
        key: 'p1',
        customValueConverter: function(item) {
          return item.p1.customValue;
        }
      }];

      expect(Filters.filterItems(appliedFilters, items, filterDefinitions)).toEqual(1);
      expect(items[0].isFiltered).toBeTruthy();
      expect(items[1].isFiltered).toBeFalsy();
    });
  });

});
