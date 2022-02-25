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
.controller('UsersListCtrl',
['$scope', 'User', '$modal', '$rootScope', 'UserConstants', '$translate', 'Cluster', 'View', 'ConfirmationModal', 'Settings', 'Pagination', 'Filters',
function($scope, User, $modal, $rootScope, UserConstants, $translate, Cluster, View, ConfirmationModal, Settings, Pagination, Filters) {
  var $t = $translate.instant;
  $scope.constants = {
    admin: $t('users.ambariAdmin'),
    users: $t('common.users').toLowerCase()
  };
  $scope.users = [];
  $scope.minRowsToShowPagination = Settings.minRowsToShowPagination;
  $scope.isLoading = false;
  $scope.pagination = Pagination.create();
  $scope.tableInfo = {
    filtered: 0,
    total: 0,
    showed: 0
  };
  $scope.filters = [
    {
      key: 'user_name',
      label: $t('users.username'),
      customValueConverter: function(item) {
        return item.Users.user_name;
      },
      options: []
    },
    {
      key: 'role',
      label: $t('clusters.role'),
      customValueConverter: function(item) {
        return item.Users.roles[0] ? item.Users.roles[0].permission_label : '';
      },
      options: []
    },
    {
      key: 'status',
      label: $t('users.status'),
      isStatic: true,
      customValueConverter: function(item) {
        return item.Users.active ? $t('users.active') : $t('users.inactive');
      },
      options: [
        {
          key: $t('users.active'),
          label: $t('users.active')
        },
        {
          key: $t('users.inactive'),
          label: $t('users.inactive')
        }
      ]
    },
    {
      key: 'type',
      label: $t('common.type'),
      customValueConverter: function(item) {
        return item.Users.userTypeName;
      },
      options: []
    },
    {
      key: 'group',
      label: $t('common.group'),
      isMultiple: true,
      customValueConverter: function(item) {
        return item.Users.groups;
      },
      options: []
    }
  ];

  function loadUsers() {
    $scope.isLoading = true;
    User.list().then(function (data) {
      $scope.users = data.data.items.map(User.makeUser);
      $scope.tableInfo.total = $scope.users.length;
      $scope.filterUsers();
      Filters.initFilterOptions($scope.filters, $scope.users);
    }).finally(function () {
      $scope.isLoading = false;
    });
  }

  $scope.toggleSearchBox = function() {
    $('.search-box-button .popup-arrow-up, .search-box-row').toggleClass('hide');
  };

  $scope.pageChanged = function () {
    $scope.pagination.pageChanged($scope.users, $scope.tableInfo);
  };

  $scope.resetPagination = function () {
    $scope.pagination.resetPagination($scope.users, $scope.tableInfo);
  };

  $scope.filterUsers = function(appliedFilters) {
    $scope.tableInfo.filtered = Filters.filterItems(appliedFilters, $scope.users, $scope.filters);
    $scope.pagination.resetPagination($scope.users, $scope.tableInfo);
  };

  $rootScope.$watch(function (scope) {
    return scope.LDAPSynced;
  }, function (LDAPSynced) {
    if (LDAPSynced === true) {
      $rootScope.LDAPSynced = false;
      loadUsers();
    }
  });

  $scope.createUser = function () {
    var modalInstance = $modal.open({
      templateUrl: 'views/userManagement/modals/userCreate.html',
      controller: 'UserCreateCtrl',
      backdrop: 'static'
    });

    modalInstance.result.finally(loadUsers);
  };

  $scope.deleteUser = function (user) {
    if (!user.isDeletable) {
      return false;
    }
    ConfirmationModal.show(
      $t('common.delete', {
        term: $t('common.user')
      }),
      $t('common.deleteConfirmation', {
        instanceType: $t('common.user').toLowerCase(),
        instanceName: '"' + user.user_name + '"'
      }),
      null,
      null,
      {primaryClass: 'btn-danger'}
    ).then(function () {
      Cluster.getPrivilegesForResource({
        nameFilter: user.user_name,
        typeFilter: {value: 'USER'}
      }).then(function (data) {
        var clusterPrivilegesIds = [];
        var viewsPrivileges = [];
        if (data.items && data.items.length) {
          angular.forEach(data.items[0].privileges, function (privilege) {
            if (privilege.PrivilegeInfo.principal_type === 'USER') {
              if (privilege.PrivilegeInfo.type === 'VIEW') {
                viewsPrivileges.push({
                  id: privilege.PrivilegeInfo.privilege_id,
                  view_name: privilege.PrivilegeInfo.view_name,
                  version: privilege.PrivilegeInfo.version,
                  instance_name: privilege.PrivilegeInfo.instance_name
                });
              } else {
                clusterPrivilegesIds.push(privilege.PrivilegeInfo.privilege_id);
              }
            }
          });
        }
        User.delete(user.user_name).then(function () {
          if (clusterPrivilegesIds.length) {
            Cluster.deleteMultiplePrivileges($rootScope.cluster.Clusters.cluster_name, clusterPrivilegesIds);
          }
          angular.forEach(viewsPrivileges, function (privilege) {
            View.deletePrivilege(privilege);
          });
          loadUsers();
        });
      });
    });
  };

  loadUsers();

}]);
