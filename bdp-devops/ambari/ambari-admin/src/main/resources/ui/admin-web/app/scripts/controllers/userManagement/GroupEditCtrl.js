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
.controller('GroupEditCtrl',
['$scope', '$rootScope', 'Group', '$routeParams', 'Cluster', 'View', 'Alert', 'ConfirmationModal', '$location', '$translate', 'RoleDetailsModal',
function($scope, $rootScope, Group, $routeParams, Cluster, View, Alert, ConfirmationModal, $location,  $translate, RoleDetailsModal) {
  var $t = $translate.instant;
  var nonRole = {
    permission_name: 'NONE',
    permission_label: $t('users.roles.none')
  };

  $scope.constants = {
    group: $t('common.group'),
    view: $t('common.view').toLowerCase(),
    cluster: $t('common.cluster').toLowerCase()
  };
  $scope.editMode = false;
  $scope.group = new Group($routeParams.id);
  $scope.group.editingUsers = [];
  $scope.groupMembers = [];
  $scope.dataLoaded = false;
  $scope.isMembersEditing = false;

  $scope.$watch(function() {
    return $scope.group.editingUsers;
  }, function(newValue) {
    if(newValue && !angular.equals(newValue, $scope.groupMembers)){
      $scope.updateMembers();  
    }
  }, true);
  
  $scope.enableMembersEditing = function() {
    $scope.isMembersEditing = true;
    $scope.group.editingUsers = angular.copy($scope.groupMembers);
  };
  $scope.cancelUpdate = function() {
    $scope.isMembersEditing = false;
    $scope.group.editingUsers = '';
  };
  $scope.updateMembers = function() {
    var newMembers = $scope.group.editingUsers.toString().split(',').filter(function(item) {
      return item.trim();}
    ).map(function(item) {
        return item.trim()
      }
    );
    $scope.group.members = newMembers;
    $scope.group.saveMembers().catch(function(data) {
        Alert.error($t('groups.alerts.cannotUpdateGroupMembers'), "<div class='break-word'>" + data.message + "</div>");
      }).finally(function() {
        loadGroup();
      });
    $scope.isMembersEditing = false;
  };

  $scope.removeViewPrivilege = function(name, privilege) {
    var privilegeObject = {
        id: privilege.privilege_id,
        view_name: privilege.view_name,
        version: privilege.version,
        instance_name: name
    };
    View.deletePrivilege(privilegeObject).then(function() {
      loadGroup();
    });
  };

  $scope.showHelpPage = function() {
    Cluster.getRolesWithAuthorizations().then(function(roles) {
      RoleDetailsModal.show(roles);
    });
  };

  $scope.updateRole = function () {
    var clusterName = $rootScope.cluster.Clusters.cluster_name;
    if ($scope.originalRole.permission_name !== $scope.currentRole.permission_name) {
      if ($scope.currentRole.permission_name === 'NONE') {
        deleteGroupRoles(clusterName, $scope.group).finally(loadGroup);
      } else {
        if ($scope.group.roles.length) {
          deleteGroupRoles(clusterName, $scope.group, true).finally(function() {
            addGroupRoles(clusterName, $scope.currentRole, $scope.group).finally(loadGroup);
          });
        } else {
          addGroupRoles(clusterName, $scope.currentRole, $scope.group).finally(loadGroup);
        }
      }
    }
  };

  function deleteGroupRoles(clusterName, group, ignoreAlert) {
    return Cluster.deleteMultiplePrivileges(
      clusterName,
      group.roles.map(function(item) {
        return item.privilege_id;
      })
    ).then(function () {
      if (!ignoreAlert) {
        Alert.success($t('users.alerts.roleChangedToNone', {
          user_name: group.group_name
        }));
      }
    }).catch(function (data) {
      Alert.error($t('common.alerts.cannotSavePermissions'), data.data.message);
    });
  }

  function addGroupRoles(clusterName, newRole, group) {
    return Cluster.createPrivileges(
      {
        clusterId: clusterName
      },
      [{
        PrivilegeInfo: {
          permission_name: newRole.permission_name,
          principal_name: group.group_name,
          principal_type: 'GROUP'
        }
      }]
    ).then(function () {
      Alert.success($t('users.alerts.roleChanged', {
        name: group.group_name,
        role: newRole.permission_label
      }));
    }).catch(function (data) {
      Alert.error($t('common.alerts.cannotSavePermissions'), data.data.message);
    });
  }

  function parsePrivileges(rawPrivileges) {
    var privileges = {
      clusters: {},
      views: {}
    };
    angular.forEach(rawPrivileges, function (privilege) {
      privilege = privilege.PrivilegeInfo;
      if (privilege.type === 'CLUSTER') {
        // This is cluster
        privileges.clusters[privilege.cluster_name] = privileges.clusters[privilege.cluster_name] || [];
        privileges.clusters[privilege.cluster_name].push(privilege.permission_label);
      } else if (privilege.type === 'VIEW') {
        privileges.views[privilege.instance_name] = privileges.views[privilege.instance_name] || {
          privileges: [],
          version: privilege.version,
          view_name: privilege.view_name,
          privilege_id: privilege.privilege_id
        };
        if (privileges.views[privilege.instance_name].privileges.indexOf(privilege.permission_label) === -1) {
          privileges.views[privilege.instance_name].privileges.push(privilege.permission_label);
        }
      }
    });

    $scope.privileges = privileges;
    $scope.noClusterPriv = $.isEmptyObject(privileges.clusters);
    $scope.noViewPriv = $.isEmptyObject(privileges.views);
    $scope.hidePrivileges = $scope.noClusterPriv && $scope.noViewPriv;
  }

  function loadGroup() {
    Group.get($routeParams.id).then(function(group) {
      $scope.group = group;
      parsePrivileges(group.privileges);
      var clusterRole = $scope.group.roles[0];
      $scope.currentRole = clusterRole || nonRole;
      $scope.originalRole = clusterRole || nonRole;
      $scope.groupMembers = group.members.map(function(item) {
        return item.MemberInfo.user_name;
      });
      $scope.group.editingUsers = angular.copy($scope.groupMembers);
    }).finally(function() {
      $scope.dataLoaded = true;
    });
  }

  function loadRoles() {
    return Cluster.getRoleOptions().then(function(data) {
      $scope.roleOptions = data;
    });
  }

  loadRoles().finally(loadGroup);

}]);
