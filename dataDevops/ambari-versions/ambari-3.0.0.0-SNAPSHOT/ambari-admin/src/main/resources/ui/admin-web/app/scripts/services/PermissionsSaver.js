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
.factory('PermissionSaver', ['Cluster', 'View', '$q', 'getDifference', '$translate', function(Cluster, View, $q, getDifference, $translate) {
  var $t = $translate.instant;

  function savePermissionsFor(resource, permissions, params){
    var arr = [];
    angular.forEach(permissions, function(permission) {
      // Sanitaize input
      var users = permission.USER.toString().split(',').filter(function(item) {return item.trim();}).map(function(item) {return item.trim()});
      var groups = permission.GROUP.toString().split(',').filter(function(item) {return item.trim();}).map(function(item) {return item.trim()});
      // Build array
      arr = arr.concat(users.map(function(user) {
        return {
          'PrivilegeInfo':{
            'permission_name': permission.PermissionInfo.permission_name,
            'principal_name': user,
            'principal_type': 'USER'
          }
        }
      }));

      arr = arr.concat(groups.map(function(group) {
        return {
          'PrivilegeInfo':{
            'permission_name': permission.PermissionInfo.permission_name,
            'principal_name': group,
            'principal_type': 'GROUP'
          }
        }
      }));

      angular.forEach(View.permissionRoles, function(key) {
        if(permission.ROLE[key] === true) {
          arr.push({
            'PrivilegeInfo': {
              'permission_name': 'VIEW.USER',
              'principal_name': key,
              'principal_type': 'ROLE'
            }
          });
        }
      });

    });
    if (!passOneRoleCheck(arr)) {
      console.log($t('common.alerts.checkFailed'));
      var deferred = $q.defer();
      deferred.reject({
        data: {
          message: $t('users.roles.oneRolePerUserOrGroup')
        }
      });
      return deferred.promise;
    }
    return resource.updatePrivileges(params, arr);
  }

  function passOneRoleCheck(arr) {
    var hashes = {};
    for(var i = 0; i < arr.length; i++) {
      var obj = arr[i],
        type = obj.PrivilegeInfo.principal_type,
        name = obj.PrivilegeInfo.principal_name;
      if (!hashes[type]) {
        hashes[type] = {};
      }
      if (hashes[type][name] && name !== "*") {
        return false;
      } else {
        hashes[type][name] = true;
      }
    }
    return true;
  }

  return {
    saveClusterPermissions: function(permissions, params) {
      return savePermissionsFor(Cluster, permissions, params);
    },
    saveViewPermissions: function(permissions, params) {
      return savePermissionsFor(View, permissions, params);
    }
  };
}]);
