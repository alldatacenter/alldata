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

describe('#Cluster', function () {
  describe('ViewsListCtrl', function() {
    var scope, ctrl;

    beforeEach(function () {
      module('ambariAdminConsole');
      inject(function($rootScope, $controller) {
        scope = $rootScope.$new();
        scope.pagination = {
          resetPagination: angular.noop
        };
        ctrl = $controller('ViewsListCtrl', {$scope: scope});
      });
      scope.instances = [
        {
          short_url_name: 'sun1',
          url: 'url1',
          view_name: 'vn1',
          instance_name: 'in1',
          short_url: 'su1'
        },
        {
          short_url_name: 'sun2',
          url: 'url2',
          view_name: 'vn2',
          instance_name: 'in2',
          short_url: 'su2'
        }
      ];
    });

    describe('#filterInstances', function() {
      beforeEach(function() {
        spyOn(scope.pagination, 'resetPagination');
      });

      it('all should be filtered when filters not applied', function() {
        scope.filterInstances();
        expect(scope.tableInfo.filtered).toEqual(2);
        scope.filterInstances([]);
        expect(scope.tableInfo.filtered).toEqual(2);
      });

      it('resetPagination should be called', function() {
        scope.filterInstances();
        expect(scope.pagination.resetPagination).toHaveBeenCalled();
      });

      it('one view should be filtered', function() {
        var appliedFilters = [
          {
            key: 'view_name',
            values: ['vn1']
          }
        ];
        scope.filterInstances(appliedFilters);
        expect(scope.tableInfo.filtered).toEqual(1);
        expect(scope.instances[0].isFiltered).toBeTruthy();
        expect(scope.instances[1].isFiltered).toBeFalsy();
      });

      it('two views should be filtered', function() {
        var appliedFilters = [
          {
            key: 'view_name',
            values: ['vn1', 'vn2']
          }
        ];
        scope.filterInstances(appliedFilters);
        expect(scope.tableInfo.filtered).toEqual(2);
        expect(scope.instances[0].isFiltered).toBeTruthy();
        expect(scope.instances[1].isFiltered).toBeTruthy();
      });

      it('one views should be filtered with combo filter', function() {
        var appliedFilters = [
          {
            key: 'view_name',
            values: ['vn1', 'vn2']
          },
          {
            key: 'instance_name',
            values: ['in2']
          }
        ];
        scope.filterInstances(appliedFilters);
        expect(scope.tableInfo.filtered).toEqual(1);
        expect(scope.instances[0].isFiltered).toBeFalsy();
        expect(scope.instances[1].isFiltered).toBeTruthy();
      });
    });
  });
});
