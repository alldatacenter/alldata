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

angular.module('ambariAdminConsole', [
  'ngRoute',
  'ngAnimate',
  'ui.bootstrap',
  'restangular',
  'toggle-switch',
  'pascalprecht.translate'
])
.constant('Settings', {
  siteRoot: '{proxy_root}/'.replace(/\{.+\}/g, ''),
	baseUrl: '{proxy_root}/api/v1'.replace(/\{.+\}/g, ''),
  testMode: false,
  mockDataPrefix: 'assets/data/',
  isLDAPConfigurationSupported: false,
  isLoginActivitiesSupported: false,
  maxStackTraceLength: 1000,
  errorStorageSize: 500000,
  minRowsToShowPagination: 10
})
.config(['RestangularProvider', '$httpProvider', '$provide', 'Settings', function(RestangularProvider, $httpProvider, $provide, Settings) {
  // Config Ajax-module
  RestangularProvider.setBaseUrl(Settings.baseUrl);
  RestangularProvider.setDefaultHeaders({'X-Requested-By':'ambari'});

  $httpProvider.defaults.headers.post['Content-Type'] = 'plain/text';
  $httpProvider.defaults.headers.put['Content-Type'] = 'plain/text';

  $httpProvider.defaults.headers.post['X-Requested-By'] = 'ambari';
  $httpProvider.defaults.headers.put['X-Requested-By'] = 'ambari';
  $httpProvider.defaults.headers.common['X-Requested-By'] = 'ambari';

  $httpProvider.interceptors.push(['Settings', '$q', function(Settings, $q) {
    return {
      'request': function(config) {
        if (Settings.testMode) {
          if (config.method === 'GET') {
            config.url = (config.mock) ? Settings.mockDataPrefix + config.mock : config.url;
          } else {
            config.method = "GET";
          }
        }
        return config;
      }
    };
  }]);

  $httpProvider.interceptors.push(['$rootScope', '$q', function (scope, $q) {
    return {
      responseError: function (response) {
        if (response.status === 403) {
          window.location = Settings.siteRoot;
        }
        return $q.reject(response);
      }
    };
  }]);

  $provide.factory('TimestampHttpInterceptor', [function($q) {
    return {
      request: function(config) {
        if (config && config.method === 'GET' && config.url.indexOf('html') === -1) {
          config.url += config.url.indexOf('?') < 0 ? '?' : '&';
          config.url += '_=' + new Date().getTime();
         }
         return config || $q.when(config);
      }
   };
  }]);
  $httpProvider.interceptors.push('TimestampHttpInterceptor');


  $provide.decorator('$exceptionHandler', ['$delegate', 'Utility', '$window', function ($delegate, Utility, $window) {
    return function (error, cause) {
      var ls = JSON.parse($window.localStorage.getItem('errors')) || {},
        key = new Date().getTime(),
        origin = $window.location.origin || ($window.location.protocol + '//' + $window.location.host),
        pattern = new RegExp(origin + '/.*scripts', 'g'),
        stackTrace = error && error.stack && error.stack.replace(pattern, '').substr(0, Settings.maxStackTraceLength),
        file = error && error.fileName,
        line = error && error.lineNumber,
        col = error && error.columnNumber;

      if (error && error.stack && (!file || !line || !col)) {
        var patternText = '(' + $window.location.protocol + '//.*\\.js):(\\d+):(\\d+)',
          details = error.stack.match(new RegExp(patternText));
        file = file || (details && details [1]);
        line = line || (details && Number(details [2]));
        col = col || (details && Number(details [3]));
      }

      var val = {
        file: file,
        line: line,
        col: col,
        error: error.toString(),
        stackTrace: stackTrace
      };

      //overwrite errors if storage full
      if (JSON.stringify(ls).length > Settings.errorStorageSize) {
        delete ls[Object.keys(ls).sort()[0]];
      }

      ls[key] = val;
      var lsString = JSON.stringify(ls);
      $window.localStorage.setItem('errors', lsString);
      Utility.postUserPref('errors', ls);
      $delegate(error, cause);
    };
  }]);

  if (!Array.prototype.find) {
    Array.prototype.find = function (callback, context) {
      if (this == null) {
        throw new TypeError('Array.prototype.find called on null or undefined');
      }
      if (typeof callback !== 'function') {
        throw new TypeError(callback + ' is not a function');
      }
      var list = Object(this),
        length = list.length >>> 0,
        value;
      for (var i = 0; i < length; i++) {
        value = list[i];
        if (callback.call(context, value, i, list)) {
          return value;
        }
      }
      return undefined;
    };
  }
}]);
