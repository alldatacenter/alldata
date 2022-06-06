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

function _parseId(id) {
  return id.replace(/[^\d|\.]/g, '').split('.').map(function (i) {return parseInt(i, 10);});
}

const flatten = (list) => list.reduce((a, b) => a.concat(Array.isArray(b) ? flatten(b) : b), []);

module.exports = {
  /**
   *
   * @param arr {Array}
   * @param id
   * @return result {Array}
   */
  uniqObjectsbyId: function (arr, id) {
    var result = [];
    arr.forEach(function (item) {
      var isIdPresent = result.someProperty(id, item[id]);
      if (!isIdPresent) {
        result.pushObject(item);
      }
    });
    return result;
  },


  /**
   * intersect two arrays and return the common values
   *
   * @param  {Array} arr1 - first array
   * @param  {Array} arr2 - second array
   * @return {Array} intersection - the intersection of arr1 and arr2
   */
  intersect: function (arr1, arr2) {
    var intersection = [];
    var shortest = arr1.length >= arr2.length ? arr2 : arr1;
    var longest = arr1.length >= arr2.length ? arr1 : arr2;

    shortest.forEach(function (entry) {
      longest.indexOf(entry) > -1 ? intersection.push(entry) : null;
    });

    return intersection;
  },

  /**
   * Callback for sorting models with `id`-property equal to something like version number: 'HDP-1.2.3', '4.2.52' etc
   *
   * @param {{id: string}} obj1
   * @param {{id: string}} obj2
   * @returns {number}
   */
  sortByIdAsVersion: function (obj1, obj2) {
    var id1 = _parseId(Em.get(obj1, 'id'));
    var id2 = _parseId(Em.get(obj2, 'id'));
    var lId1 = id1.length;
    var lId2 = id2.length;
    var limit = lId1 > lId2 ? lId2 : lId1;
    for (var i = 0; i < limit; i++) {
      if (id1[i] > id2[i]) {
        return 1;
      }
      if (id1[i] < id2[i]) {
        return -1;
      }
    }
    if (lId1 === lId2) {
      return 0
    }
    return lId1 > lId2 ? 1 : -1;
  },

  flatten

};
