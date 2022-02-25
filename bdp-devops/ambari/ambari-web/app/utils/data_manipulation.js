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

module.exports = {

  /**
   * Exclude objects from <code>collection</code> if its <code>key</code> exist in <code>valuesToReject</code>
   * Example:
   * <code>
   *   var collection = [{n: 'v1'}, {n: 'v2'}, {n: 'v3'}, {n: 'v4'}],
   *   key = 'n',
   *   valuesToReject = ['v2', 'v3'];
   *   var result = rejectPropertyValues(collection, key, valuesToReject); // [{n: 'v1'}, {n: 'v4'}]
   * </code>
   * @param {Object[]} collection array of objects
   * @param {String} key property name of each object to be checked
   * @param {String[]} valuesToReject list of <code>key</code> values. Objects with <code>key</code>,
   *                                    equal to one of them, will be excluded
   * @returns {Object[]} Rejected array
   */
  rejectPropertyValues: function(collection, key, valuesToReject) {
    return collection.filter(function (item) {
      var propertyValue = Em.get(item, key);
      return !valuesToReject.contains(propertyValue);
    });
  },

  /**
   * Wrapper to <code>filterProperty</code>-method that allows using list of values to filter
   * Example:
   * <code>
   *   var collection = [{n: 'v1'}, {n: 'v2'}, {n: 'v3'}, {n: 'v4'}],
   *   key = 'n',
   *   valuesToFilter = ['v2', 'v3'];
   *   var result = filterPropertyValues(collection, key, valuesToFilter); // [{n: 'v2'}, {n: 'v3'}]
   * </code>
   * @param {Object[]} collection array of objects
   * @param {String} key property name of each object to be filtered
   * @param {String|String[]} valuesToFilter array of values (or one value-string) to filter
   * @returns {Object[]} Filtered array
   */
  filterPropertyValues: function(collection, key, valuesToFilter) {
    var type = Em.typeOf(valuesToFilter);
    if ('string' === type)
      return collection.filterProperty(key, valuesToFilter);

    var ret = [];
    if ('array' === type) {
      valuesToFilter.forEach(function (value) {
        ret = ret.concat(collection.filterProperty(key, value));
      });
    }
    return ret;
  },

  /**
   * Group <code>collection</code> items by <code>key</code> value
   * Example:
   * <code>
   *   var collection = [{n: 'v1'}, {n: 'v2'}, {n: 'v2'}, {n: 'v4'}],
   *   key = 'n';
   *   var result = groupPropertyValues(collection, key); // {v1: [{n: 'v1'}], v2: [{n: 'v2'}, {n: 'v2'}], v4: [{n: 'v4'}]}
   * </code>
   * @param {Object[]} collection array of objects
   * @param {String} key property name of each object to be grouped
   * @returns {object}
   */
  groupPropertyValues: function(collection, key) {
    var group = {};
    collection.forEach(function(item) {
      var value = Em.get(item, key);
      if (Em.isNone(group[value])) {
        group[value] = [item];
      }
      else {
        group[value].pushObject(item);
      }
    });
    return group;
  }

};
