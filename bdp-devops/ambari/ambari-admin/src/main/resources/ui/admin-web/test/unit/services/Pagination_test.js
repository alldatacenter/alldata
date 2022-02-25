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

describe('Pagination Service', function () {
  var Pagination, $pagination;

  beforeEach(function () {
    module('ambariAdminConsole', angular.noop);
    inject(function (_Pagination_) {
      Pagination = _Pagination_;
      $pagination = Pagination.create({
        itemsPerPage: 1
      });
    });
  });

  describe('#pageChanged', function() {

    it('should show items on second page', function() {
      var items = [
        { isFiltered: true },
        { isFiltered: true },
        { isFiltered: true }
      ];
      var tableInfo = {
        showed: 0
      };
      $pagination.currentPage = 2;
      $pagination.pageChanged(items, tableInfo);
      expect(items[0].isShowed).toBeFalsy();
      expect(items[1].isShowed).toBeTruthy();
      expect(items[2].isShowed).toBeFalsy();
      expect(tableInfo.showed).toEqual(1);
    });
  });

  describe('#resetPagination', function() {

    it('should show items on first page', function() {
      var items = [
        { isFiltered: true },
        { isFiltered: true },
        { isFiltered: true }
      ];
      var tableInfo = {
        showed: 0
      };
      $pagination.currentPage = 2;
      $pagination.resetPagination(items, tableInfo);
      expect(items[0].isShowed).toBeTruthy();
      expect(items[1].isShowed).toBeFalsy();
      expect(items[2].isShowed).toBeFalsy();
      expect(tableInfo.showed).toEqual(1);
    });
  });

});
