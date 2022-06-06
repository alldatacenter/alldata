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
.controller('ViewsListCtrl',
['$scope', 'View','$modal', 'Alert', 'ConfirmationModal', '$translate', 'Settings', 'Pagination', 'Filters',
function($scope, View, $modal, Alert, ConfirmationModal, $translate, Settings, Pagination, Filters) {
  var $t = $translate.instant;
  var VIEWS_VERSION_STATUS_TIMEOUT = 5000;
  $scope.isLoading = false;
  $scope.minInstanceForPagination = Settings.minRowsToShowPagination;

  $scope.filters = [
    {
      key: 'short_url_name',
      label: $t('common.name'),
      options: []
    },
    {
      key: 'url',
      label: $t('urls.url'),
      customValueConverter: function(item) {
        return '/main/view/' + item.view_name + '/' + item.short_url;
      },
      options: []
    },
    {
      key: 'view_name',
      label: $t('views.table.viewType'),
      options: []
    },
    {
      key: 'instance_name',
      label: $t('urls.viewInstance'),
      options: []
    }
  ];
  $scope.views = [];
  $scope.instances = [];
  $scope.tableInfo = {
    filtered: 0,
    showed: 0,
    total: 0
  };
  $scope.pagination = Pagination.create();

  $scope.resetPagination = function() {
    $scope.pagination.resetPagination($scope.instances, $scope.tableInfo);
  };

  $scope.pageChanged = function() {
    $scope.pagination.pageChanged($scope.instances, $scope.tableInfo);
  };

  $scope.filterInstances = function(appliedFilters) {
    $scope.tableInfo.filtered = Filters.filterItems(appliedFilters, $scope.instances, $scope.filters);
    $scope.pagination.resetPagination($scope.instances, $scope.tableInfo);
  };

  $scope.toggleSearchBox = function() {
    $('.search-box-button .popup-arrow-up, .search-box-row').toggleClass('hide');
  };

  $scope.cloneInstance = function(instanceClone) {
    $scope.createInstance(instanceClone);
  };

  $scope.createInstance = function (instanceClone) {
    var modalInstance = $modal.open({
      templateUrl: 'views/ambariViews/modals/create.html',
      controller: 'CreateViewInstanceCtrl',
      resolve: {
        views: function() {
          return $scope.views;
        },
        instanceClone: function() {
          return instanceClone;
        }
      },
      backdrop: 'static'
    });

    modalInstance.result.then(loadViews);
  };

  $scope.deleteInstance = function (instance) {
    ConfirmationModal.show(
      $t('common.delete', {
        term: $t('views.viewInstance')
      }),
      $t('common.deleteConfirmation', {
        instanceType: $t('views.viewInstance'),
        instanceName: instance.label
      }),
      null,
      null,
      {
        primaryClass: 'btn-danger'
      }
    ).then(function () {
      View.deleteInstance(instance.view_name, instance.version, instance.instance_name)
        .then(function () {
          loadViews();
        })
        .catch(function (data) {
          Alert.error($t('views.alerts.cannotDeleteInstance'), data.data.message);
        });
    });
  };

  loadViews();

  function checkViewVersionStatus(view, versionObj, versionNumber) {
    var deferred = View.checkViewVersionStatus(view.view_name, versionNumber);

    deferred.promise.then(function (status) {
      if (versionNeedStatusUpdate(status)) {
        setTimeout(function() {
          checkViewVersionStatus(view, versionObj, versionNumber);
        }, VIEWS_VERSION_STATUS_TIMEOUT);
      } else {
        versionObj.status = status;
        angular.forEach(view.versions, function (version) {
          if (version.status === 'DEPLOYED') {
            view.canCreateInstance = true;
          }
        })
      }
    });
  }

  function versionNeedStatusUpdate(status) {
    return status !== 'DEPLOYED' && status !== 'ERROR';
  }

  function loadViews() {
    $scope.isLoading = true;
    View.all().then(function (views) {
      $scope.isLoading = false;
      $scope.views = views;
      $scope.instances = [];
      angular.forEach(views, function (view) {
        angular.forEach(view.versions, function (versionObj, versionNumber) {
          if (versionNeedStatusUpdate(versionObj.status)) {
            checkViewVersionStatus(view, versionObj, versionNumber);
          }
        });
        angular.forEach(view.instances, function (instance) {
          instance.ViewInstanceInfo.short_url_name = instance.ViewInstanceInfo.short_url_name || '';
          instance.ViewInstanceInfo.short_url = instance.ViewInstanceInfo.short_url || '';
          instance.ViewInstanceInfo.versionObj = view.versions[instance.ViewInstanceInfo.version] || {};
          $scope.instances.push(instance.ViewInstanceInfo);
        });
      });
      $scope.tableInfo.total = $scope.instances.length;
      Filters.initFilterOptions($scope.filters, $scope.instances);
      $scope.filterInstances();
    }).catch(function (data) {
      Alert.error($t('views.alerts.cannotLoadViews'), data.data.message);
    });
  }

}]);
