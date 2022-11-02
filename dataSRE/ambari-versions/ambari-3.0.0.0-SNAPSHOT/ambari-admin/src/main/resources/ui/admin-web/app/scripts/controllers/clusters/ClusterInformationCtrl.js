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
.controller('ClusterInformationCtrl',
['$scope', '$http', '$location', 'Cluster', '$routeParams', '$translate', '$rootScope', 'ConfirmationModal', 'Alert',
function($scope, $http, $location, Cluster, $routeParams, $translate, $rootScope, ConfirmationModal, Alert) {
  var $t = $translate.instant;
  $scope.isDataLoaded = false;
  $scope.edit = {
    clusterName: null
  };
  $scope.isClusterNameEdited = false;

  $scope.$watch(function() {
    return $rootScope.cluster;
  }, function() {
    $scope.cluster = $rootScope.cluster;
    if ($scope.cluster) {
      $scope.edit.clusterName = $scope.cluster.Clusters.cluster_name;
      $scope.getBlueprint();
    }
  }, true);

  $scope.getBlueprint = function () {
    Cluster.getBlueprint({
      clusterName: $scope.cluster.Clusters.cluster_name
    }).then(function (data) {
      console.debug($t('exportBlueprint.dataLoaded'), data);
      $scope.isDataLoaded = true;
      var response = JSON.stringify(data, null, 4),
        lt = /&lt;/g,
        gt = /&gt;/g,
        ap = /&#39;/g,
        ic = /&#34;/g;
      $scope.blueprint = response ? response.toString().replace(lt, "<").replace(gt, ">").replace(ap, "'").replace(ic, '"') : "";
    });
  };

  $scope.downloadBlueprint = function () {
    if (window.navigator.msSaveOrOpenBlob) {
      var blob = new Blob([decodeURIComponent(encodeURIComponent($scope.blueprint))], {
        type: "text/csv;charset=utf-8;"
      });
      navigator.msSaveBlob(blob, 'blueprint.json');
    } else {
      var a = document.createElement('a');
      a.href = 'data:attachment/json;charset=utf-8,' + encodeURIComponent($scope.blueprint);
      a.target = '_blank';
      a.download = 'blueprint.json';
      document.body.appendChild(a);
      a.click();
    }
  };

  $scope.toggleSaveButton = function() {
    var value = $scope.edit.clusterName;
    $scope.isClusterNameEdited = (value !== null && $scope.cluster.Clusters.cluster_name !== value);
  };

  $scope.confirmClusterNameChange = function() {
    ConfirmationModal.show(
      $t('common.clusterNameChangeConfirmation.title'),
      $t('common.clusterNameChangeConfirmation.message', {
        clusterName: $scope.edit.clusterName
      })
    )
      .then(function () {
        $scope.saveClusterName();
      }).catch(function () {
      // user clicked cancel
      $scope.edit.clusterName = $scope.cluster.Clusters.cluster_name;
      $scope.toggleSaveButton();
    });
  };

  $scope.saveClusterName = function() {
    var oldClusterName = $scope.cluster.Clusters.cluster_name,
        newClusterName = $scope.edit.clusterName;

    Cluster.editName(oldClusterName, newClusterName).then(function(data) {
      $scope.cluster.Clusters.cluster_name = newClusterName;
      $scope.edit.clusterName = newClusterName;
      $scope.toggleSaveButton();
      Alert.success($t('common.alerts.clusterRenamed', {clusterName: newClusterName}));
    }).catch(function(data) {
      Alert.error($t('common.alerts.cannotRenameCluster', {clusterName: newClusterName}), data.data.message);
    });
  };
}]);
