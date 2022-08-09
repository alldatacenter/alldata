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
var validator = require('utils/validator');

App.TableServerMixin = Em.Mixin.create({
  queryParams: [],
  resetStartIndex: false,

  /**
   * filterProps support follow types of filter:
   * MATCH - match of RegExp
   * EQUAL - equality "="
   * LESS - "<"
   * MORE - ">"
   * MULTIPLE - multiple values to compare
   * CUSTOM - substitute values with keys "{#}" in alias
   */
  filterProps: [],

  /**
   * include "from" and "page_size"
   */
  paginationProps: [
    {
      name: 'displayLength',
      key: 'page_size',
      value: '25',
      type: 'EQUAL'
    },
    {
      name: 'startIndex',
      key: 'from',
      value: 0,
      type: 'EQUAL'
    }
  ],

  sortProps: [],

  /**
   * update values of pagination properties from local db and return them
   * @return {array}
   */
  getPaginationProps: function () {
    var displayLength = App.db.getDisplayLength(this.get('name'));
    if (displayLength) {
      this.get('paginationProps').findProperty('name', 'displayLength').value = displayLength;
    }

    var startIndex = App.db.getStartIndex(this.get('name'));
    if (!Em.isNone(startIndex)) {
      startIndex = (startIndex > 0) ? startIndex - 1 : startIndex;
      this.get('paginationProps').findProperty('name', 'startIndex').value = startIndex;
    }
    return this.get('paginationProps').slice(0);
  },

  /**
   * get sort properties from local db
   * @return {Array}
   */
  getSortProps: function () {
    var savedSortConditions = App.db.getSortingStatuses(this.get('name')) || [],
      sortProperties = this.get('sortProps'),
      sortParams = [];
    savedSortConditions.forEach(function (sort) {
      var property = sortProperties.findProperty('name', sort.name);
      if (property && (sort.status === 'sorting_asc' || sort.status === 'sorting_desc')) {
        property.value = sort.status.replace('sorting_', '');
        property.type = 'SORT';
        sortParams.push(property);
      }
    });
    return sortParams;
  },
  /**
   * get filter properties from local db
   * @return {Array}
   */
  getFilterProps: function () {
    var savedFilterConditions = App.db.getFilterConditions(this.get('name')) || [],
      filterProperties = this.get('filterProps'),
      filterParams = [],
      colPropAssoc = this.get('colPropAssoc');

    savedFilterConditions.forEach(function (filter) {
      var property = filterProperties.findProperty('name', colPropAssoc[filter.iColumn]);

      if (property && filter.value && filter.value.length > 0 && !filter.skipFilter) {
        property.isFilter = true;

        if (filter.type === 'range') {
          //range value should contain array of two element with start and end values accordingly
          filter.value.forEach(function (val, index) {
            if (val) {
              property.type = (index === 0) ? "MORE" : "LESS";
              property.value = val;
              filterParams.push(property);
            }
          });
        } else if (filter.type === 'string') {
          property.value = this.getRegExp(filter.value);
          filterParams.push(property);
        } else {
          property.value = filter.value;
          filterParams.push(property);
        }
      }
    }, this);
    return filterParams;
  },

  getQueryParameters: function () {
    var queryParams = [];

    queryParams.pushObjects(this.getPaginationProps());
    queryParams.pushObjects(this.getSortProps());
    queryParams.pushObjects(this.getFilterProps());
    this.set('queryParams', queryParams);
    return queryParams;
  },

  getRegExp: function (value) {
    value = validator.isValidMatchesRegexp(value) ? value.replace(/(\.+\*?|(\.\*)+)$/, '') + '.*' : '^$';
    value = /^\.\*/.test(value) || value == '^$' ? value : '.*' + value;
    return value;
  }

});
