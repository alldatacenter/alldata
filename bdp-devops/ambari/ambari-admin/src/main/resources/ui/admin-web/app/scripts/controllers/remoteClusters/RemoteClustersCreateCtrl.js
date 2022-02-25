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
.controller('RemoteClustersCreateCtrl', ['$scope', '$routeParams', '$location', 'Alert', '$translate', 'Cluster', 'AddRepositoryModal' , 'Settings', 'RemoteCluster', function($scope, $routeParams, $location, Alert, $translate, Cluster, AddRepositoryModal, Settings, RemoteCluster) {
  var $t = $translate.instant;

  $scope.cluster = {};

  $scope.nameValidationPattern = /^\s*\w*\s*$/;

  $scope.registerRemoteCluster = function () {
    $scope.form.submitted = true;
    if ($scope.form.$valid){
     var payload = {
        "ClusterInfo" :{
          "name" : $scope.cluster.cluster_name,
          "url" : $scope.cluster.cluster_url,
          "username" : $scope.cluster.cluster_user,
          "password" : $scope.cluster.cluster_password
        }
      };

      var config = {
        headers : {
          'X-Requested-By': 'Ambari;'
        }
      }

      RemoteCluster.register(payload, config).then(function(data) {
          Alert.success($t('common.alerts.remoteClusterRegistered', {clusterName: payload.ClusterInfo.name}));
          $scope.form.$setPristine();
          $location.path('/remoteClusters/'+ $scope.cluster.cluster_name +'/edit')
        })
        .catch(function(data) {
          console.log(data);
          Alert.error(data.message);
       });

    }
  };

  $scope.cancel = function () {
    $scope.editVersionDisabled = true;
    $location.path('/remoteClusters');
  };


}]);
