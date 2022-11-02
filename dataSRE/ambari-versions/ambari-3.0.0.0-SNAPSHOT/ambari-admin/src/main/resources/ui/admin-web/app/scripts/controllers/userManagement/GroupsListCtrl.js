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
.controller('GroupsListCtrl',
['$scope', 'Group', '$modal', 'ConfirmationModal', '$rootScope', '$translate', 'Settings', 'Cluster', 'View', 'Alert', 'Pagination', 'Filters',
function($scope, Group, $modal, ConfirmationModal, $rootScope, $translate, Settings, Cluster, View, Alert, Pagination, Filters) {
  var $t = $translate.instant;
  $scope.constants = {
    groups: $t('common.groups').toLowerCase()
  };
  $scope.minRowsToShowPagination = Settings.minRowsToShowPagination;
  $scope.isLoading = false;
  $scope.groups = [];
  $scope.tableInfo = {
    filtered: 0,
    total: 0,
    showed: 0
  };
  $scope.pagination = Pagination.create();
  $scope.filters = [
    {
      key: 'group_name',
      label: $t('groups.name'),
      options: []
    },
    {
      key: 'groupTypeName',
      label: $t('common.type'),
      options: []
    }
  ];

  $scope.resetPagination = function() {
    $scope.pagination.resetPagination($scope.groups, $scope.tableInfo);
  };

  $scope.pageChanged = function() {
    $scope.pagination.pageChanged($scope.groups, $scope.tableInfo);
  };

  $scope.filterGroups = function(appliedFilters) {
    $scope.tableInfo.filtered = Filters.filterItems(appliedFilters, $scope.groups, $scope.filters);
    $scope.pagination.resetPagination($scope.groups, $scope.tableInfo);
  };

  $scope.toggleSearchBox = function() {
    $('.search-box-button .popup-arrow-up, .search-box-row').toggleClass('hide');
  };

  $scope.loadGroups = function() {
    $scope.isLoading = true;
    Group.all().then(function(groups) {
      $scope.isLoading = false;
      $scope.groups = groups.map(Group.makeGroup);
      $scope.tableInfo.total = $scope.groups.length;
      Filters.initFilterOptions($scope.filters, $scope.groups);
      $scope.filterGroups();
    })
    .catch(function(data) {
      Alert.error($t('groups.alerts.getGroupsListError'), data.data.message);
    });
  };

  $scope.loadGroups();

  $rootScope.$watch(function(scope) {
    return scope.LDAPSynced;
  }, function(LDAPSynced) {
    if(LDAPSynced === true){
      $rootScope.LDAPSynced = false;
      $scope.loadGroups();
    }
  });

  $scope.createGroup = function () {
    var modalInstance = $modal.open({
      templateUrl: 'views/userManagement/modals/groupCreate.html',
      controller: 'GroupCreateCtrl',
      backdrop: 'static'
    });

    modalInstance.result.finally($scope.loadGroups);
  };

  $scope.deleteGroup = function(group) {
    ConfirmationModal.show(
      $t('common.delete', {
        term: $t('common.group')
      }),
      $t('common.deleteConfirmation', {
        instanceType: $t('common.group').toLowerCase(),
        instanceName: '"' + group.group_name + '"'
      }),
      null,
      null,
      {primaryClass: 'btn-danger'}
    ).then(function() {
      Cluster.getPrivilegesForResource({
        nameFilter : group.group_name,
        typeFilter : {value: 'GROUP'}
      }).then(function(data) {
        var clusterPrivilegesIds = [];
        var viewsPrivileges = [];
        if (data.items && data.items.length) {
          angular.forEach(data.items[0].privileges, function(privilege) {
            if (privilege.PrivilegeInfo.principal_type === 'GROUP') {
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
        group.destroy().then(function() {
          if (clusterPrivilegesIds.length) {
            Cluster.deleteMultiplePrivileges($rootScope.cluster.Clusters.cluster_name, clusterPrivilegesIds);
          }
          angular.forEach(viewsPrivileges, function(privilege) {
            View.deletePrivilege(privilege);
          });
          $scope.loadGroups();
        });
      });
    });
  };

}]);