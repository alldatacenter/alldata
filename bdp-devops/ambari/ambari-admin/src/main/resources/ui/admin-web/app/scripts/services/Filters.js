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
.factory('Filters', function() {

  function initFilterOptions(filters, items) {
    filters.filter(function(filter) {
      return !filter.isStatic;
    }).forEach(function(filter) {
      var preOptions = [];
      if (filter.isMultiple) {
        items.forEach(function(item) {
          if (typeof filter.customValueConverter === 'function') {
            preOptions = preOptions.concat(filter.customValueConverter(item));
          } else {
            preOptions = preOptions.concat(item[filter.key]);
          }
        });
      } else {
        preOptions = items.map(function(item) {
          if (typeof filter.customValueConverter === 'function') {
            return filter.customValueConverter(item);
          }
          return item[filter.key];
        });
      }
      filter.options = $.unique(preOptions).filter(function(item) {
        return item !== undefined && item !== null;
      }).map(function(item) {
        return {
          key: item,
          label: item
        }
      });
    });
  }

  function filterItems(appliedFilters, items, filterDefinitions) {
    var filteredCount = 0;
    angular.forEach(items, function(item) {
      item.isFiltered = !(appliedFilters && appliedFilters.length > 0 && appliedFilters.some(function(filter) {
        var customValueFilter = filterDefinitions.filter(function(filterDefinition) {
          return filterDefinition.key === filter.key && typeof filterDefinition.customValueConverter === 'function';
        })[0];
        if (customValueFilter) {
          return filter.values.every(function(value) {
            var itemValue = customValueFilter.customValueConverter(item);
            var preparedValue = Array.isArray(itemValue) ? itemValue.join().toLowerCase() : itemValue.toLowerCase();
            return String(preparedValue).indexOf(value.toLowerCase()) === -1;
          });
        }
        return filter.values.every(function(value) {
          var itemValue = item[filter.key];
          var preparedValue = Array.isArray(itemValue) ? itemValue.join().toLowerCase() : itemValue.toLowerCase();
          return String(preparedValue).indexOf(value.toLowerCase()) === -1;

        });
      }));

      filteredCount += ~~item.isFiltered;
    });
    return filteredCount;
  }

  return {
    initFilterOptions: initFilterOptions,
    filterItems: filterItems
  };
});
