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
    .controller('HomeDirectoryCtrl', ['$scope', '$location', 'UnsavedDialog', function($scope, $location, UnsavedDialog) {

      $scope.TEMPLATE_PLACEHOLER = '/user/{{username}}';
      $scope.autoCreate = false;
      $scope.template = '';
      $scope.group = '';
      $scope.permissions = '';
  
      $scope.save = function (targetUrl) {
        targetUrl ? $location.path(targetUrl) : "";
      }
  
      $scope.$on('$locationChangeStart', function(event, __targetUrl) {
        if( $scope.form.$dirty ){
          UnsavedDialog().then(function(action) {
            var targetUrl = __targetUrl.split('#').pop();
            switch(action){
              case 'save':
                $scope.save(targetUrl);
                $scope.form.$setPristine();
                break;
              case 'discard':
                $scope.form.$setPristine();
                $location.path(targetUrl);
                break;
              case 'cancel':
                targetUrl = '/homeDirectory';
                break;
            }
          });
          event.preventDefault();
        }
      });
    }]);
