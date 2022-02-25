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
.factory('Cluster', ['$http', '$q', 'Settings', '$translate', function($http, $q, Settings, $translate) {
  var $t = $translate.instant;
  var permissions = null;
  var rolesWithAuthorizations = null;

  return {
    repoStatusCache : {},

    orderedRoles : [
      'CLUSTER.ADMINISTRATOR',
      'CLUSTER.OPERATOR',
      'SERVICE.ADMINISTRATOR',
      'SERVICE.OPERATOR',
      'CLUSTER.USER'
    ],

    orderedLevels: ['SERVICE', 'HOST', 'CLUSTER', 'AMBARI'],

    ineditableRoles : ['VIEW.USER', 'AMBARI.ADMINISTRATOR'],

    sortRoles: function(roles) {
      var orderedRoles = ['AMBARI.ADMINISTRATOR'].concat(this.orderedRoles);
      return roles.sort(function(a, b) {
        return orderedRoles.indexOf(a.permission_name) - orderedRoles.indexOf(b.permission_name);
      });
    },

    getAllClusters: function() {
      var deferred = $q.defer();
      $http.get(Settings.baseUrl + '/clusters?fields=Clusters/cluster_id', {mock: 'cluster/clusters.json'})
      .then(function(data, status, headers) {
        deferred.resolve(data.data.items);
      })
      .catch(function(data) {
        deferred.reject(data);
      });

      return deferred.promise;
    },
    getStatus: function() {
      var deferred = $q.defer();

      $http.get(Settings.baseUrl + '/clusters?fields=Clusters/provisioning_state', {mock: 'cluster/init.json'})
      .then(function(data, status, headers) {
        deferred.resolve(data.data.items[0]);
      })
      .catch(function(data) {
        deferred.reject(data);
      });

      return deferred.promise;
    },
    getAmbariVersion: function() {
      var deferred = $q.defer();

      $http.get(Settings.baseUrl + '/services/AMBARI/components/AMBARI_SERVER?fields=RootServiceComponents/component_version,RootServiceComponents/properties/server.os_family&minimal_response=true', {mock: '2.1'})
      .then(function(data) {
        deferred.resolve(data.data.RootServiceComponents.component_version);
      })
      .catch(function(data) {
        deferred.reject(data);
      });

      return deferred.promise;
    },
    getClusterOS: function() {
      var deferred = $q.defer();

      $http.get(Settings.baseUrl + '/services/AMBARI/components/AMBARI_SERVER?fields=RootServiceComponents/properties/server.os_family&minimal_response=true', {mock: 'redhat6'})
      .then(function(data) {
        deferred.resolve(data.data.RootServiceComponents.properties['server.os_family']);
      })
      .catch(function(data) {
        deferred.reject(data);
      });

      return deferred.promise;
    },
    getAmbariTimeout: function() {
      var deferred = $q.defer();
      var url = '/services/AMBARI/components/AMBARI_SERVER?fields=RootServiceComponents/properties/user.inactivity.timeout.default';
      $http.get(Settings.baseUrl + url)
        .then(function(data) {
          var properties = data.data.RootServiceComponents.properties;
          var timeout = properties ? properties['user.inactivity.timeout.default'] : 0;
          deferred.resolve(timeout);
        }, function (data) {
          deferred.reject(data);
        });

      return deferred.promise;
    },
    getPermissions: function() {
      var deferred = $q.defer();

      $http({
        method: 'GET',
        url: Settings.baseUrl + '/permissions',
        mock: 'permission/permissions.json',
        params: {
          fields: 'PermissionInfo',
          'PermissionInfo/resource_name': 'CLUSTER'
        }
      })
      .then(function(data) {
        deferred.resolve(data.data.items);
      })
      .catch(function(data) {
        deferred.reject(data); });

      return deferred.promise;
    },
    getRoleOptions: function () {
      var roleOptions = [];
      var deferred = $q.defer();
      var localDeferred = $q.defer();
      var promise = permissions ? localDeferred.promise : this.getPermissions();

      localDeferred.resolve(permissions);
      promise.then(function(data) {
        permissions = data;
        roleOptions = data.map(function(item) {
          return item.PermissionInfo;
        });
        roleOptions.unshift({
          permission_name: 'NONE',
          permission_label: $t('users.roles.none')
        });
      }).finally(function() {
        deferred.resolve(roleOptions);
      });
      return deferred.promise;
    },
    getRolesWithAuthorizations: function() {
      var deferred = $q.defer();
      if (rolesWithAuthorizations) {
        deferred.resolve(rolesWithAuthorizations);
      } else {
        $http({
          method: 'GET',
          url: Settings.baseUrl + '/permissions?PermissionInfo/resource_name.in(CLUSTER,AMBARI)',
          mock: 'permission/permissions.json',
          params: {
            fields: 'PermissionInfo/*,authorizations/AuthorizationInfo/*'
          }
        })
          .then(function (data) {
            rolesWithAuthorizations = data.data.items;
            deferred.resolve(data.data.items);
          })
          .catch(function (data) {
            deferred.reject(data);
          });
      }

      return deferred.promise;
    },

    getPrivileges: function(params) {
      var deferred = $q.defer();

      $http({
        method: 'GET',
        url: Settings.baseUrl + '/clusters/'+params.clusterId,
        params : {
          'fields': 'privileges/PrivilegeInfo'
        }
      })
      .then(function(data) {
        deferred.resolve(data.data.privileges);
      })
      .catch(function(data) {
        deferred.reject(data);
      });

      return deferred.promise;
    },
    getPrivilegesForResource: function(params) {
      var deferred = $q.defer();
      var isUser = (params.typeFilter.value == 'USER');
      var endpoint = isUser ? '/users' : '/groups';
      var nameURL = isUser ? '&Users/user_name.matches(' : '&Groups/group_name.matches(';
      var nameFilter = params.nameFilter ? (nameURL + params.nameFilter + ')') : '';
      $http({
        method : 'GET',
        url : Settings.baseUrl + endpoint + '?' + 'fields=privileges/PrivilegeInfo/*' + nameFilter
      })
      .then(function(data) {
        deferred.resolve(data.data);
      })
      .catch(function(data) {
        deferred.reject(data);
      });

      return deferred.promise;
    },
    createPrivileges: function(params, data) {
      return $http({
        method: 'POST',
        url: Settings.baseUrl + '/clusters/'+params.clusterId+'/privileges',
        data: data
      });
    },
    deletePrivileges: function(params, data) {
      return $http({
        method: 'DELETE',
        url: Settings.baseUrl + '/clusters/'+params.clusterId+'/privileges',
        data: data
      });
    },
    deleteMultiplePrivileges: function(clusterId, privilege_ids) {
      return $http({
        method: 'DELETE',
        url: Settings.baseUrl + '/clusters/'+clusterId+'/privileges?PrivilegeInfo/privilege_id.in\('+privilege_ids+'\)'
      });
    },
    updatePrivileges: function(params, privileges) {
      return $http({
        method: 'PUT',
        url: Settings.baseUrl + '/clusters/' + params.clusterId + '/privileges',
        data: privileges
      });
    },
    deletePrivilege: function(clusterId, id) {
      return $http({
        method: 'DELETE',
        url: Settings.baseUrl + '/clusters/'+clusterId+'/privileges/' + id
      });
    },
    editName: function(oldName, newName) {
      return $http({
        method: 'PUT',
        url: Settings.baseUrl + '/clusters/' + oldName,
        data: {
          Clusters: {
            "cluster_name": newName
          }
        }
      });
    },
    getBlueprint: function(params){
      var deferred = $q.defer();
      var clusterName = params.clusterName;
      $http({
        method: 'GET',
        url: Settings.baseUrl + '/clusters/' + clusterName + '?' + 'format=blueprint'
      })
      .then(function(data) {
        deferred.resolve(data.data);
      })
      .catch(function(data) {
        deferred.reject(data);
      });
      return deferred.promise;
    },
    getRepoVersionStatus: function (clusterName, repoId ) {
      var me = this;
      var deferred = $q.defer();
      var url = Settings.baseUrl + '/clusters/' + clusterName +
        '/stack_versions?fields=*&ClusterStackVersions/repository_version=' + repoId;
      $http.get(url, {mock: 'cluster/repoVersionStatus.json'})
      .then(function (data) {
        data = data.data.items;
        var response = {};
        if (data.length > 0) {
          var hostStatus = data[0].ClusterStackVersions.host_states;
          var currentHosts = hostStatus['CURRENT'].length;
          var installedHosts = hostStatus['INSTALLED'].length;
          var totalHosts = 0;
          // collect hosts on all status
          angular.forEach(hostStatus, function(status) {
            totalHosts += status.length;
          });
          response.status = data[0].ClusterStackVersions.state;
          response.currentHosts = currentHosts;
          response.installedHosts = installedHosts;
          response.totalHosts = totalHosts;
          response.stackVersionId = data[0].ClusterStackVersions.id;
        } else {
          response.status = '';
        }
        me.repoStatusCache[repoId] = response.status;
        deferred.resolve(response);
      })
      .catch(function (data) {
        deferred.reject(data);
      });
      return deferred.promise;
    }
  };
}]);
