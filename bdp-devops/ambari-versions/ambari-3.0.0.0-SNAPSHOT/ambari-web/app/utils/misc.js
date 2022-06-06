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
   * Convert value from bytes to appropriate measure
   */
  formatBandwidth: function (value) {
    if (value) {
      if (value < 1024) {
        value = '<1KB';
      } else {
        if (value < 1048576) {
          value = (value / 1024).toFixed(1) + 'KB';
        } else  if (value >= 1048576 && value < 1073741824){
          value = (value / 1048576).toFixed(1) + 'MB';
        } else {
          value = (value / 1073741824).toFixed(2) + 'GB';
        }
      }
    }
    return value;
  },
  /**
   * Convert ip address to integer
   * @param ip
   * @return integer
   */
  ipToInt: function(ip){
    // *     example 1: ipToInt('192.0.34.166');
    // *     returns 1: 3221234342
    // *     example 2: ipToInt('255.255.255.256');
    // *     returns 2: false
    // Verify IP format.
    if (!/^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$/.test(ip)) {
      return false; // Invalid format.
    }
    // Reuse ip variable for component counter.
    var d = ip.split('.');
    return ((((((+d[0])*256)+(+d[1]))*256)+(+d[2]))*256)+(+d[3]);
  },

  sortByOrder: function (sortOrder, array) {
    var sorted = [];
    for (var i = 0; i < sortOrder.length; i++)
      for (var j = 0; j < array.length; j++) {
        if (sortOrder[i] == Em.get(array[j], 'id')) {
          sorted.push(array[j]);
        }
      }
    return sorted;
  },

  utf8ToB64: function(stringToEncode) {
    return window.btoa(unescape(encodeURIComponent(stringToEncode)));
  },

  b64ToUtf8: function(stringTodecode) {
    return decodeURIComponent(escape(window.atob(stringTodecode)));
  }
  
};
