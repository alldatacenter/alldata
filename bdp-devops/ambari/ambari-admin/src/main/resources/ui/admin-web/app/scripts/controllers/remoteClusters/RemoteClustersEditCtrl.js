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
.controller('RemoteClustersEditCtrl', ['$scope', '$modal', '$routeParams', '$location', 'Alert', '$translate', 'Cluster', 'Settings','RemoteCluster', 'DeregisterClusterModal', function($scope, $modal, $routeParams, $location, Alert, $translate, Cluster, Settings, RemoteCluster, DeregisterClusterModal) {
  var $t = $translate.instant;

  $scope.cluster = {};
  $scope.instancesAffected = [];

  $scope.nameValidationPattern = /^\s*\w*\s*$/;

  $scope.openChangePwdDialog = function() {
    var modalInstance = $modal.open({
      templateUrl: 'views/remoteClusters/modals/changePassword.html',
      resolve: {
        clusterId: function() {
          return $scope.cluster.cluster_id;
        },
        clusterName: function() {
          return $scope.cluster.cluster_name;
        },
        clusterUrl: function() {
          return $scope.cluster.cluster_url;
        },
        clusterUser: function() {
          return $scope.cluster.cluster_user;
        }
      },
      controller: ['$scope', 'clusterId' ,'clusterName', 'clusterUrl', 'clusterUser', 'Settings','Alert',  function($scope, clusterId, clusterName, clusterUrl , clusterUser , Settings, Alert) {
        $scope.passwordData = {
          password: '',
          currentUserName: clusterUser || ''
        };

        $scope.form = {};

        $scope.clusterId = clusterId;
        $scope.currentUser = clusterUser;
        $scope.clusterName = clusterName;
        $scope.clusterUrl = clusterUrl;

        $scope.ok = function() {
          $scope.form.passwordChangeForm.submitted = true;


          if ($scope.form.passwordChangeForm.$valid){

            var payload = {
              "ClusterInfo" :{
                "cluster_id" : $scope.clusterId,
                "name" : $scope.clusterName,
                "url" : $scope.clusterUrl,
                "username" : $scope.passwordData.currentUserName,
                "password" : $scope.passwordData.password
              }
            };

            var config = {
              headers : {
                'X-Requested-By': 'Ambari;'
              }
            }

            RemoteCluster.edit(payload, config).then(function(data) {
                Alert.success($t('views.alerts.credentialsUpdated'));
                $scope.form.passwordChangeForm = {};
              })
              .catch(function(data) {
                console.log(data);
                Alert.error(data.message);
              });

            modalInstance.dismiss('cancel');
          }

        };
        $scope.cancel = function() {
          modalInstance.dismiss('cancel');
        };
      }]
    });
  };

  $scope.deleteCluster = function() {

    $scope.instancesAffected = [];
    RemoteCluster.affectedViews($scope.cluster.cluster_name).then(function(response) {

        response.items.forEach(function(item){
          item.versions.forEach(function(version){
            version.instances.forEach(function(instance){
              $scope.instancesAffected.push(instance.ViewInstanceInfo.instance_name);
            })
          })
        })

        DeregisterClusterModal.show(
          $t('common.deregisterCluster',{term: $t('common.cluster')}),
          $t('common.remoteClusterDelConfirmation', {instanceType: $t('common.cluster').toLowerCase(), instanceName: '"' + $scope.cluster.cluster_name + '"'}),
          $scope.instancesAffected

        ).then(function() {
          RemoteCluster.deregister($scope.cluster.cluster_name).then(function() {
            $location.path('/remoteClusters');
          });
        });
    })
    .catch(function(data) {
      console.log(data);
    });
  };

  $scope.editRemoteCluster = function () {
    $scope.form.submitted = true;
    if ($scope.form.$valid){
      var payload = {
        "ClusterInfo" :{
          "cluster_id" : $scope.cluster.cluster_id,
          "name" : $scope.cluster.cluster_name,
          "url" : $scope.cluster.cluster_url,
          "username" : $scope.cluster.cluster_user
        }
      };

      var config = {
        headers : {
          'X-Requested-By': 'Ambari;'
        }
      }

      RemoteCluster.edit(payload, config).then(function(data) {
          Alert.success($t('views.alerts.savedRemoteClusterInformation'));
          $scope.form.$setPristine();
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

  // Fetch remote cluster details
  $scope.fetchRemoteClusterDetails = function (clusterName) {

    RemoteCluster.getDetails(clusterName).then(function(response) {
        $scope.cluster.cluster_id = response.ClusterInfo.cluster_id;
        $scope.cluster.cluster_name = response.ClusterInfo.name;
        $scope.cluster.cluster_url = response.ClusterInfo.url;
        $scope.cluster.cluster_user = response.ClusterInfo.username;
      })
      .catch(function(data) {
        console.log(data);
      });

  };

  $scope.fetchRemoteClusterDetails($routeParams.clusterName);


}]);
