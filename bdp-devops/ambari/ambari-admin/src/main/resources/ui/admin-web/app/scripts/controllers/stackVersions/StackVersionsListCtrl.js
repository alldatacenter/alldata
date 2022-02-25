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
  .controller('StackVersionsListCtrl',
  ['$scope', 'Cluster', 'Stack', '$routeParams', '$translate', 'Settings', 'Pagination', '$q', 'Filters',
  function ($scope, Cluster, Stack, $routeParams, $translate, Settings, Pagination, $q, Filters) {
    var $t = $translate.instant;
    $scope.getConstant = function (key) {
      return $t(key).toLowerCase();
    };
    $scope.minInstanceForPagination = Settings.minRowsToShowPagination;
    $scope.isLoading = false;
    $scope.clusterName = $routeParams.clusterName;
    $scope.tableInfo = {
      total: 0,
      showed: 0,
      filtered: 0
    };
    $scope.repos = [];
    $scope.dropDownClusters = [];
    $scope.selectedCluster = $scope.dropDownClusters[0];
    $scope.filters = [
      {
        key: 'stack',
        label: $t('common.stack'),
        customValueConverter: function(item) {
          return item.stack_name + '-' + item.stack_version;
        },
        options: []
      },
      {
        key: 'display_name',
        label: $t('common.name'),
        options: []
      },
      {
        key: 'type',
        label: $t('common.type'),
        options: []
      },
      {
        key: 'repository_version',
        label: $t('common.version'),
        options: []
      },
      {
        key: 'cluster',
        label: $t('common.cluster'),
        options: []
      }
    ];
    $scope.pagination = Pagination.create();

    $scope.resetPagination = function() {
      $scope.pagination.resetPagination($scope.repos, $scope.tableInfo);
    };

    $scope.pageChanged = function() {
      $scope.pagination.pageChanged($scope.repos, $scope.tableInfo);
    };

    $scope.filterRepos = function (appliedFilters) {
      $scope.tableInfo.filtered = Filters.filterItems(appliedFilters, $scope.repos, $scope.filters);
      $scope.pagination.resetPagination($scope.repos, $scope.tableInfo);
    };

    $scope.toggleSearchBox = function() {
      $('.search-box-button .popup-arrow-up, .search-box-row').toggleClass('hide');
    };

    $scope.goToCluster = function() {
      window.location.replace(Settings.siteRoot + '#/main/admin/stack/versions');
    };

    $scope.fetchRepoClusterStatus = function (allRepos) {
      var calls = [];
      if (allRepos && allRepos.length) {
        // only support one cluster at the moment
        var clusterName = $scope.cluster && $scope.cluster.Clusters.cluster_name;
        if (clusterName) {
          $scope.repos = allRepos;
          $scope.tableInfo.total = allRepos.length;
          angular.forEach($scope.repos, function (repo) {
            calls.push(Cluster.getRepoVersionStatus(clusterName, repo.id).then(function (response) {
              repo.cluster = (response.status === 'CURRENT' || response.status === 'INSTALLED') ? clusterName : '';
              if (repo.cluster) {
                repo.status = response.status;
                repo.totalHosts = response.totalHosts;
                repo.currentHosts = response.currentHosts;
                repo.installedHosts = response.installedHosts;
                repo.stackVersionId = response.stackVersionId;
              }
            }));
          });
        }
      } else {
        $scope.repos = [];
        $scope.tableInfo.total = 0;
        $scope.pagination.totalRepos = 0;
        $scope.tableInfo.showed = 0;
      }
      $scope.tableInfo.total = $scope.repos.length;
      return $q.all(calls);
    };

    $scope.fetchRepos = function () {
      return Stack.allRepos().then(function (repos) {
        $scope.isLoading = false;
        return repos.items;
      });
    };

    $scope.fetchClusters = function () {
      return Cluster.getAllClusters().then(function (clusters) {
        if (clusters && clusters.length > 0) {
          $scope.dropDownClusters = clusters;
        }
      });
    };

    $scope.loadAllData = function () {
      $scope.isLoading = true;
      $scope.fetchRepos()
        .then(function (repos) {
          $scope.fetchClusters();
          $scope.fetchRepoClusterStatus(repos).then(function() {
            Filters.initFilterOptions($scope.filters, $scope.repos);
          });
          $scope.filterRepos();
        });
    };

    $scope.loadAllData();

    $scope.toggleVisibility = function (repo) {
      repo.isProccessing = true;
      var payload = {
        RepositoryVersions: {
          hidden: repo.hidden
        }
      };
      Stack.updateRepo(repo.stack_name, repo.stack_version, repo.id, payload).then(null, function () {
        repo.hidden = !repo.hidden;
      }).finally(function () {
        delete repo.isProccessing;
      });
    };

    $scope.isHideCheckBoxEnabled = function ( repo ) {
      return !repo.isProccessing && ( (!repo.cluster && repo.status !== 'OUT_OF_SYNC') || repo.isPatch && ( repo.status === 'INSTALLED' || repo.status === 'INSTALL_FAILED') );
    }
  }]);
