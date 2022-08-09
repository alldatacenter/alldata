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
.factory('RoleDetailsModal', ['$modal', 'Cluster', function($modal, Cluster) {
  return {
    show: function(roles) {
      roles = roles.map(function(role) {
        var r = role.PermissionInfo;
        r.authorizations = role.authorizations.map(function(authorization) {
          return authorization.AuthorizationInfo;
        });
        return r;
      });
      var modalInstance = $modal.open({
        templateUrl: 'views/modals/RoleDetailsModal.html',
        size: 'lg',
        controller: function($scope, $modalInstance) {
          var authorizationsOrder;
          $scope.title = '';
          $scope.orderedRoles = ['AMBARI.ADMINISTRATOR'].concat(Cluster.orderedRoles).reverse();
          $scope.orderedLevels = Cluster.orderedLevels;
          $scope.authHash = {};
          $scope.getLevelName = function (key) {
            return key.charAt(0) + key.slice(1).toLowerCase();
          };
          angular.forEach(roles, function (r) {
            angular.forEach(r.authorizations, function (auth) {
              var match = auth.authorization_id.match(/(\w+)\./),
                  levelKey = match && match[1],
                  isLevelDisplayed = $scope.orderedLevels.indexOf(levelKey) !== -1;
              if (isLevelDisplayed) {
                if (!$scope.authHash[levelKey]) {
                  $scope.authHash[levelKey] = {};
                }
                if (!$scope.authHash[levelKey][auth.authorization_id]) {
                  $scope.authHash[levelKey][auth.authorization_id] = {
                    name: auth.authorization_name,
                    roles: {}
                  };
                }
                $scope.authHash[levelKey][auth.authorization_id].roles[r.permission_name] = true;
              }
            });
          });

          // sort authorizations for each level by number of roles permissions
          for (var level in $scope.authHash) {
            if ($scope.authHash.hasOwnProperty(level)) {
              authorizationsOrder = Object.keys($scope.authHash[level]).sort(function (a, b) {
                return Object.keys($scope.authHash[level][b].roles).length - Object.keys($scope.authHash[level][a].roles).length;
              });
              $scope.authHash[level].order = authorizationsOrder;
            }
          }

          $scope.roles = roles.sort(function (a, b) {
            return $scope.orderedRoles.indexOf(a.permission_name) - $scope.orderedRoles.indexOf(b.permission_name);
          });
          $scope.ok = function() {
            $modalInstance.dismiss();
          };
        }
      });
      return modalInstance.result;
    }
  }
}]);