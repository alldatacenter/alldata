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
.factory('Auth',['$http', 'Settings', function($http, Settings) {
  var ambari;
  var currentUserName;
  if (localStorage.ambari) {
    ambari = JSON.parse(localStorage.ambari);
    if (ambari && ambari.app && ambari.app.loginName) {
      currentUserName = ambari.app.loginName;
    }
  }
  return {
    signout: function() {
      var data = JSON.parse(localStorage.ambari);
      delete data.app.authenticated;
      delete data.app.loginName;
      delete data.app.user;
      localStorage.ambari = JSON.stringify(data);
      // Workaround for sign off within Basic Authorization
      //commenting this out since using Date.now() in the url causes a security error in IE and does not log out user
      /*var origin = $window.location.protocol + '//' + Date.now() + ':' + Date.now() + '@' +
            $window.location.hostname + ($window.location.port ? ':' + $window.location.port : '');
      return $http({
        method: 'GET',
        url: origin + Settings.baseUrl + '/logout'
      });*/
      //use an invalid username and password in the request header
      $http.defaults.headers.common['Authorization'] = 'Basic ' + btoa('invalid_username:password');
      return $http({
        method: 'GET',
        url: Settings.baseUrl + '/logout'
      });
    },
    getCurrentUser: function() {
    	return currentUserName;
    }
  };
}]);
