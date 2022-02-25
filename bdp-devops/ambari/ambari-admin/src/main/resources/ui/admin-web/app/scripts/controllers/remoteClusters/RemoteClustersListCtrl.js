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
.controller('RemoteClustersListCtrl',
['$scope', '$routeParams', '$translate', 'RemoteCluster', 'Settings', 'Pagination', 'Filters',
function ($scope, $routeParams, $translate, RemoteCluster, Settings, Pagination, Filters) {
  var $t = $translate.instant;

  $scope.minInstanceForPagination = Settings.minRowsToShowPagination;
  $scope.clusterName = $routeParams.clusterName;
  $scope.isLoading = false;
  $scope.constants = {
    groups: $t('common.clusters').toLowerCase()
  };
  $scope.tableInfo = {
    filtered: 0,
    total: 0,
    showed: 0
  };
  $scope.pagination = Pagination.create();
  $scope.filters = [
    {
      key: 'clusterName',
      label: $t('views.clusterName'),
      options: []
    },
    {
      key: 'service',
      label: $t('common.services'),
      customValueConverter: function (item) {
        return item.ClusterInfo.services;
      },
      isMultiple: true,
      options: []
    }
  ];

  $scope.toggleSearchBox = function () {
    $('.search-box-button .popup-arrow-up, .search-box-row').toggleClass('hide');
  };

  $scope.filterClusters = function (appliedFilters) {
    $scope.tableInfo.filtered = Filters.filterItems(appliedFilters, $scope.remoteClusters, $scope.filters);
    $scope.pagination.resetPagination($scope.remoteClusters, $scope.tableInfo);
  };

  $scope.pageChanged = function () {
    $scope.pagination.pageChanged($scope.remoteClusters, $scope.tableInfo);
  };

  $scope.resetPagination = function () {
    $scope.pagination.resetPagination($scope.remoteClusters, $scope.tableInfo);
  };

  function loadRemoteClusters() {
    $scope.isLoading = true;
    RemoteCluster.all().then(function (remoteclusters) {
      $scope.isLoading = false;
      $scope.remoteClusters = remoteclusters.items.map(function (item) {
        item.clusterName = item.ClusterInfo.name;
        return item;
      });
      $scope.tableInfo.total = $scope.remoteClusters.length;
      $scope.filterClusters();
      Filters.initFilterOptions($scope.filters, $scope.remoteClusters);
    })
      .catch(function (data) {
        console.warn($t('remoteClusters.alerts.fetchError'), data);
      });
  }

  loadRemoteClusters();

}]);
