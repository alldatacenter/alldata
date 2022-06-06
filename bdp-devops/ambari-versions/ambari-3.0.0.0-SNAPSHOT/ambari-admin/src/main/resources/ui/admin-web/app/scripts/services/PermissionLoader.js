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
.factory('PermissionLoader', ['Cluster', 'View', '$q', function(Cluster, View, $q) {
  
  function getPermissionsFor(resource, params){
    var deferred = $q.defer();

    resource.getPermissions(params).then(function(permissions) {
      var permissionsInner = {}; // Save object into closure, until it completely fills to prevent blinking
      angular.forEach(permissions, function(permission) {
        permission.GROUP = [];
        permission.USER = [];
        permission.ROLE = {};
        angular.forEach(View.permissionRoles, function(key) {
          permission.ROLE[key] = false;
        });
        permissionsInner[permission.PermissionInfo.permission_name] = permission;
      });

      // Now we can get privileges
      resource.getPrivileges(params).then(function(privileges) {
        angular.forEach(privileges, function(privilege) {
          if(privilege.PrivilegeInfo.principal_type == "ROLE") {
            permissionsInner[privilege.PrivilegeInfo.permission_name][privilege.PrivilegeInfo.principal_type][privilege.PrivilegeInfo.principal_name] = true;
          } else {
            permissionsInner[privilege.PrivilegeInfo.permission_name][privilege.PrivilegeInfo.principal_type].push(privilege.PrivilegeInfo.principal_name);
          }
        });

        // After all builded - return object
        deferred.resolve(permissionsInner);
      }).
      catch(function(data) {
        deferred.reject(data);
      });

    })
    .catch(function(data) {
      deferred.reject(data);
    });

    return deferred.promise;
  }

  return {
    getClusterPermissions: function(params) {
      return getPermissionsFor(Cluster, params);
    },
    getViewPermissions: function(params) {
      return getPermissionsFor(View, params);
    }
  };
}]);
