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
  .controller('ViewUrlEditCtrl',['$scope', 'View', 'Alert', 'Cluster', '$routeParams', '$location', 'UnsavedDialog', '$translate','ConfirmationModal', 'Settings' ,function($scope, View, Alert, Cluster, $routeParams, $location, UnsavedDialog, $translate,ConfirmationModal, Settings) {
  var $t = $translate.instant;
  $scope.form = {};
  $scope.constants = {
    props: $t('views.properties')
  };
  var targetUrl = '/viewUrls';


  function setUpEdit(){

      View.getUrlInfo($routeParams.urlName).then(function(url) {
        $scope.url = url.ViewUrlInfo;
        $scope.nameVersion = url.ViewUrlInfo.view_instance_common_name +" {" + url.ViewUrlInfo.view_instance_version +"}"
      }).catch(function(data) {
        Alert.error($t('views.alerts.cannotLoadViewUrl'), data.data.message);
      });
  }

  setUpEdit();


  $scope.updateUrl = function() {
      $scope.url_form.submitted = true;

      if($scope.url_form.$valid){

          var payload = {ViewUrlInfo:{
              url_name:$scope.url.url_name,
              url_suffix:$scope.url.url_suffix,
              view_instance_version:'',
              view_instance_name:'',
              view_instance_common_name:''
          }};

          View.editShortUrl(payload).then(function(urlStatus) {
              Alert.success($t('urls.urlUpdated', {
                  siteRoot: Settings.siteRoot,
                  viewName:$scope.url.view_instance_common_name ,
                  shortUrl:$scope.url.suffix,
                  urlName:$scope.url.url_name
              }));
              $scope.url_form.$setPristine();
              $location.path(targetUrl);
          }).catch(function(data) {
              Alert.error($t('views.alerts.cannotLoadViewUrls'), data.data.message);
          });

      }
  };


    $scope.deleteUrl = function() {

        ConfirmationModal.show(
            $t('common.delete', {
                term: $t('urls.url')
            }),
            $t('common.deleteConfirmation', {
                instanceType: $t('urls.url').toLowerCase(),
                instanceName: '"' + $scope.url.url_name + '"'
            })
        ).then(function() {
            View.deleteUrl($scope.url.url_name).then(function() {
                $location.path(targetUrl);
            });
        });



    };



}]);
