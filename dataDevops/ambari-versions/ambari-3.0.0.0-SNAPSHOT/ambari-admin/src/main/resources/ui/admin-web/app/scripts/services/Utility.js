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
/**
 * This service should be used to keep all utility functions in one place that can be used in any controller
 */
angular.module('ambariAdminConsole')
  .factory('Utility', ['$injector', 'Settings', function ($injector, Settings) {
    return {
      /**
       *  if version1>= version2 then return true
       *     version1 < version2 then return false
       * @param version1 {String}
       * @param version2 {String}
       * @return boolean
       */
      compareVersions: function(version1, version2) {
        version1 = version1 || '0';
        version2 = version2 || '0';
        var version1Arr = version1.split('.').map(function(item){
          return parseInt(item);
        }).filter(function(item){
          return !!item || item === 0;
        });
        var version2Arr = version2.split('.').map(function(item){
          return parseInt(item);
        }).filter(function(item){
          return !!item || item === 0;
        });
        var totalLength = Math.max(version1Arr.length, version2Arr.length);
        var result = true, i;
        for (i = 0; i <=totalLength; i++) {
          if (version2Arr[i] === undefined) {
            // Example: version1 = "2.3.2.2" and version2 = 2.3.2
            result = true;
            break;
          } else if (version1Arr[i] === undefined) {
            // Example: version1 = "2.3.2" and version2 = "2.3.2.2"
            result = false;
            break;
          } else if (version1Arr[i] > version2Arr[i]) {
            // Example: version1 = "2.3.2.2" and version2 = "2.3.2.1"
            result = true;
            break;
          } else if (version1Arr[i] < version2Arr[i]) {
            // Example: version1 = "2.3.1.2" and version2 = "2.3.2.1"
            result = false;
            break;
          }
        }
        return result;
      },

      getUserPref: function (key) {
        return $injector.get('$http').get(Settings.baseUrl + '/persist/' + key);
      },

      postUserPref: function (key, value) {
        var deferred = $injector.get('$q').defer();
        $injector.get('$rootScope').authDataLoad.promise.then(function (canPersistData) {
          if (canPersistData) {
            var keyValuePair = {};
            keyValuePair[key] = JSON.stringify(value);
            $injector.get('$http').post(Settings.baseUrl + '/persist/', JSON.stringify(keyValuePair)).then(function () {
              deferred.resolve();
            }, function () {
              deferred.reject();
            });
          } else {
            deferred.reject();
          }
        });
        return deferred.promise;
      }
    };
  }
]);