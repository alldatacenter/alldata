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

describe('#comboSearch', function () {
  var scope, element;

  beforeEach(module('ambariAdminConsole'));
  beforeEach(module('views/directives/comboSearch.html'));

  beforeEach(inject(function($rootScope, $compile, _$httpBackend_) {
    scope = $rootScope.$new();
    _$httpBackend_.expectGET('views/clusters/clusterInformation.html').respond(200);
    var preCompiledElement = '<combo-search suggestions="filters" filter-change="filterItems" placeholder="Search"></combo-search>';

    scope.filters = [
      {
        key: 'f1',
        label: 'filter1',
        options: []
      },
      {
        key: 'f2',
        label: 'filter2',
        options: []
      }
    ];
    scope.filterItems = angular.noop;
    spyOn(scope, 'filterItems');


    element = $compile(preCompiledElement)(scope);
    scope.$digest();
  }));

  afterEach(function() {
    element.remove();
  });


  describe('#removeFilter', function() {
    it('should remove filter by id', function () {
      var isoScope = element.isolateScope();
      isoScope.appliedFilters.push({
        id: 1
      });
      spyOn(isoScope, 'observeSearchFilterInput');
      spyOn(isoScope, 'updateFilters');

      isoScope.removeFilter({id: 1});

      expect(isoScope.appliedFilters).toEqual([]);
      expect(isoScope.observeSearchFilterInput).toHaveBeenCalled();
      expect(isoScope.updateFilters).toHaveBeenCalledWith([]);
    });
  });

  describe('#clearFilters', function() {
    it('should empty appliedFilters', function () {
      var isoScope = element.isolateScope();
      isoScope.appliedFilters.push({
        id: 1
      });
      spyOn(isoScope, 'updateFilters');

      isoScope.clearFilters();

      expect(isoScope.appliedFilters).toEqual([]);
      expect(isoScope.updateFilters).toHaveBeenCalledWith([]);
    });
  });

  describe('#selectFilter', function() {
    it('should add new filter to appliedFilters', function () {
      var isoScope = element.isolateScope();

      isoScope.selectFilter({
        key: 'f1',
        label: 'filter1',
        options: []
      });

      expect(isoScope.appliedFilters[0]).toEqual({
        id: 'filter_1',
        currentOption: null,
        filteredOptions: [],
        searchOptionInput: '',
        key: 'f1',
        label: 'filter1',
        options: [],
        showAutoComplete: false
      });
      expect(isoScope.isEditing).toBeFalsy();
      expect(isoScope.showAutoComplete).toBeFalsy();
      expect(isoScope.searchFilterInput).toEqual('');
    });
  });

  describe('#selectOption', function() {
    it('should set value to appliedFilter', function () {
      var isoScope = element.isolateScope();
      var filter = {};

      spyOn(isoScope, 'observeSearchFilterInput');
      spyOn(isoScope, 'updateFilters');

      isoScope.selectOption(null, {
        key: 'o1',
        label: 'option1'
      }, filter);

      expect(filter.currentOption).toEqual({
        key: 'o1',
        label: 'option1'
      });
      expect(filter.showAutoComplete).toBeFalsy();
      expect(isoScope.observeSearchFilterInput).toHaveBeenCalled();
      expect(isoScope.updateFilters).toHaveBeenCalled();
    });
  });

  describe('#makeActive', function() {
    it('category option can not be active', function () {
      var isoScope = element.isolateScope();
      var active = {
        key: 'o1',
        isCategory: true,
        active: false
      };

      isoScope.makeActive(active, [active]);

      expect(active.active).toBeFalsy();
    });

    it('value option can be active', function () {
      var isoScope = element.isolateScope();
      var active = {
        key: 'o1',
        isCategory: false,
        active: false
      };

      isoScope.makeActive(active, [active]);

      expect(active.active).toBeTruthy();
    });
  });

  describe('#updateFilters', function() {
    it('filter function from parent scope should be called', function () {
      var isoScope = element.isolateScope();
      spyOn(isoScope, 'extractFilters').and.returnValue([{}]);

      isoScope.updateFilters([{}]);

      expect(scope.filterItems).toHaveBeenCalledWith([{}]);
    });
  });

  describe('#extractFilters', function() {
    it('should extract filters', function () {
      var isoScope = element.isolateScope();
      var filters = [
        {
          currentOption: { key: 'o1'},
          key: 'f1'
        },
        {
          currentOption: { key: 'o2'},
          key: 'f1'
        },
        {
          currentOption: null,
          key: 'f2'
        }
      ];

      expect(isoScope.extractFilters(filters)).toEqual([
        {
          key: 'f1',
          values: ['o1', 'o2']
        }
      ]);
    });
  });

  describe('#observeSearchFilterInput', function() {
    it('should show all filters when search filter empty', function () {
      var isoScope = element.isolateScope();
      isoScope.searchFilterInput = '';

      isoScope.observeSearchFilterInput();

      expect(isoScope.showAutoComplete).toBeTruthy();
      expect(isoScope.filterSuggestions).toEqual([
        {
          key: 'f1',
          label: 'filter1',
          options: [  ],
          active: true
        },
        {
          key: 'f2',
          label: 'filter2',
          options: [  ],
          active: false
        }
      ]);
    });

    it('should show only searched filter when search filter not empty', function () {
      var isoScope = element.isolateScope();
      isoScope.searchFilterInput = 'filter1';

      isoScope.observeSearchFilterInput();

      expect(isoScope.showAutoComplete).toBeTruthy();
      expect(isoScope.filterSuggestions).toEqual([
        {
          key: 'f1',
          label: 'filter1',
          options: [  ],
          active: true
        }
      ]);
    });

    it('should show no filter when search filter not found', function () {
      var isoScope = element.isolateScope();
      isoScope.searchFilterInput = 'unknown-filter';

      isoScope.observeSearchFilterInput();

      expect(isoScope.showAutoComplete).toBeFalsy();
      expect(isoScope.filterSuggestions).toEqual([]);
    });
  });

  describe('#observeSearchOptionInput', function() {
    it('should show all options when options search empty', function () {
      var isoScope = element.isolateScope();
      var filter = {
        key: 'p1',
        searchOptionInput: '',
        currentOption: null,
        options: [
          {
            key: 'op1',
            label: 'op1'
          },
          {
            key: 'op2',
            label: 'op2'
          }
        ]
      };
      isoScope.appliedFilters = [
        {
          key: 'p5',
          currentOption: {
            key: 'op5'
          }
        }
      ];

      isoScope.observeSearchOptionInput(filter);

      expect(filter.showAutoComplete).toBeTruthy();
      expect(filter.filteredOptions).toEqual([
        {
          key: 'op1',
          label: 'op1',
          active: false
        },
        {
          key: 'op2',
          label: 'op2',
          active: false
        }
      ]);
    });

    it('should show only filtered options when options search not empty', function () {
      var isoScope = element.isolateScope();
      var filter = {
        key: 'p1',
        currentOption: null,
        searchOptionInput: 'op1',
        options: [
          {
            key: 'op1',
            label: 'op1'
          },
          {
            key: 'op2',
            label: 'op2'
          }
        ]
      };
      isoScope.appliedFilters = [
        {
          key: 'p5',
          currentOption: {
            key: 'op5'
          }
        }
      ];

      isoScope.observeSearchOptionInput(filter);

      expect(filter.showAutoComplete).toBeTruthy();
      expect(filter.filteredOptions).toEqual([
        {
          key: 'op1',
          label: 'op1',
          active: false
        }
      ]);
    });

    it('should show no options when options search not found', function () {
      var isoScope = element.isolateScope();
      var filter = {
        key: 'p1',
        currentOption: null,
        searchOptionInput: 'op3',
        options: [
          {
            key: 'op1',
            label: 'op1'
          },
          {
            key: 'op2',
            label: 'op2'
          }
        ]
      };
      isoScope.appliedFilters = [];

      isoScope.observeSearchOptionInput(filter);

      expect(filter.showAutoComplete).toBeFalsy();
      expect(filter.filteredOptions).toEqual([]);
    });
  });

});
