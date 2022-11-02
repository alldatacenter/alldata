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
'use strict';

angular.module('ambariAdminConsole')
.factory('Pagination', function() {

  function showItemsOnPage(items, tableInfo) {
    var startIndex = (this.currentPage - 1) * this.itemsPerPage + 1;
    var endIndex = this.currentPage * this.itemsPerPage;
    var showedCount = 0;
    var filteredCount = 0;

    angular.forEach(items, function (item) {
      item.isShowed = false;
      if (item.isFiltered) {
        filteredCount++;
        if (filteredCount >= startIndex && filteredCount <= endIndex) {
          item.isShowed = true;
          showedCount++;
        }
      }
    });
    tableInfo.showed = showedCount;
  }

  return {
    create: function(options) {
      options = options || {};
      return {
        itemsPerPage: options.itemsPerPage || 10,
        currentPage: options.currentPage || 1,
        maxVisiblePages: options.maxVisiblePages || 10,
        pageChanged: function(items, tableInfo) {
          showItemsOnPage.call(this, items, tableInfo);
        },
        resetPagination: function(items, tableInfo) {
          this.currentPage = 1;
          showItemsOnPage.call(this, items, tableInfo);
        }
      }
    }
  };
});
